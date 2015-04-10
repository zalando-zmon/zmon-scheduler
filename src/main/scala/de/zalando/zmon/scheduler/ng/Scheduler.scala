package de.zalando.zmon.scheduler.ng

import java.io.{FileWriter, OutputStreamWriter}
import java.io.File
import java.util.concurrent.{TimeUnit, ScheduledThreadPoolExecutor}

import com.codahale.metrics.{Meter, MetricRegistry}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import de.zalando.zmon.scheduler.ng.alerts.{AlertRepository, AlertDefinition, AlertSourceRegistry}
import de.zalando.zmon.scheduler.ng.checks.{CheckRepository, CheckDefinition, CheckSourceRegistry}
import de.zalando.zmon.scheduler.ng.entities.{EntityRepository, Entity, EntityAdapterRegistry}

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import redis.clients.jedis.Jedis
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import org.springframework.context.annotation.Configuration

/**
 * Created by jmussler on 3/27/15.
 */

object filter {
  def overlaps(filter: java.util.Map[String, String], entity : java.util.Map[String, String]) : Boolean = {
      for ((k,v) <- filter) {
        if (!entity.containsKey(k)) {
          return false
        }
        if (!entity.get(k).equals(v)) {
          return false
        }
      }
      true
  }
}

class Check(val id: Integer, val repo : CheckRepository) {

  def getCheckDef() = {
    repo.get(id)
  }

  def matchEntity(entity: Entity): Boolean = {
    val properties = entity.getFilterProperties
    for (filterMap <- repo.get(id).getEntities) { // OR on entity level definition in checks
      if(filter.overlaps(filterMap, properties)) {
        return true
      }
    }
    false
  }

}

class Alert(var id : Integer, val repo : AlertRepository) {

  def getAlertDef() = {
    repo.get(id)
  }

  def matchEntity(entity : Entity): Boolean = {
    val properties = entity.getFilterProperties
    for(inFilter <- repo.get(id).getEntities) {
      if(filter.overlaps(inFilter, properties)) {
        for(outFilter <- repo.get(id).getEntitiesExclude) {
          if(filter.overlaps(outFilter, properties)) {
            return false
          }
        }
        return true
      }
    }
    false
  }
}

object ScheduledCheck {
  val LOG = LoggerFactory.getLogger(ScheduledCheck.getClass)
}

class ScheduledCheck(private val selector : QueueSelector, private val rate : Long, var lastRun:Long, private val check : Check, private val alerts : mutable.MutableList[Alert], entityRepo : EntityRepository)(implicit private val config : SchedulerConfig, private val metrics: SchedulerMetrics) extends Runnable {

  val checkMeter : Meter = if (config.check_detail_metrics) metrics.metrics.meter("scheduler.check."+check.id) else null

  def execute(entity : Entity, alerts : ArrayBuffer[Alert]): Unit = {
    lastRun = System.currentTimeMillis()
    if(checkMeter != null) {
      checkMeter.mark()
    }

    selector.execute(entity, check, alerts)
    metrics.totalChecks.mark()
  }

  override def run(): Unit = {
    for(entity <- entityRepo.get()) {
      if(check.matchEntity(entity)) {
        val viableAlerts = ArrayBuffer[Alert]()
        for(alert <- alerts) {
          if(alert.matchEntity(entity)) {
            viableAlerts += alert
          }
        }

        if(!viableAlerts.isEmpty) {
          execute(entity, viableAlerts)
        }
      }
    }
  }
}

object JedisWriter {
  val jedis = new Jedis("localhost", 6379)
}

object SchedulerFactory {
  val LOG = LoggerFactory.getLogger(SchedulerFactory.getClass)
}

@Configuration
class SchedulerFactory {
  @Bean
  @Autowired
  def createScheduler(alertRepo : AlertRepository, checkRepo: CheckRepository, entityRepo : EntityRepository, queueSelector : QueueSelector)
                     (implicit schedulerConfig : SchedulerConfig, metrics: MetricRegistry) : Scheduler = {
    SchedulerFactory.LOG.info("Createing scheduler instance")
    val s = new Scheduler(alertRepo, checkRepo, entityRepo, queueSelector)
    SchedulerFactory.LOG.info("Initial scheduling of all checks")
    for(cd <- checkRepo.get()) {
      s.scheduleCheck(cd.getId)
    }
    SchedulerFactory.LOG.info("Initial scheduling of all checks done")
    s
  }


}

class SchedulerMetrics(implicit val metrics : MetricRegistry) {
  val totalChecks = metrics.meter("scheduler.total-checks")
}

object SchedulePersister {
  val mapper = new ObjectMapper with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  def loadSchedule(): Map[Integer, Long] = {
    mapper.readValue(new File("schedule.json"), new TypeReference[Map[Integer,Long]] {})
  }

  def writeSchedule( schedule : collection.concurrent.Map[Integer, Long]) = {
    if(schedule.size > 0) {
      mapper.writeValue(new File("schedule.json"), schedule)
    }
  }
}

class SchedulePersister(val scheduledChecks : scala.collection.concurrent.TrieMap[Integer, ScheduledCheck]) extends Runnable {
  override def run(): Unit = {
    SchedulePersister.writeSchedule(scheduledChecks.filter(_._2.lastRun > 0).map(x=>(x._1, x._2.lastRun)))
  }
}

object Scheduler {
  val LOG = LoggerFactory.getLogger(Scheduler.getClass())
}

class Scheduler(val alertRepo : AlertRepository, val checkRepo: CheckRepository, val entityRepo : EntityRepository, val queueSelector : QueueSelector)
               (implicit val schedulerConfig: SchedulerConfig, val metrics: MetricRegistry) {

  val service = new ScheduledThreadPoolExecutor(schedulerConfig.thread_count)
  val scheduledChecks = scala.collection.concurrent.TrieMap[Integer, ScheduledCheck]()
  implicit val schedulerMetrics = new SchedulerMetrics()
  val lastScheduleAtStartup = SchedulePersister.loadSchedule()

  service.scheduleAtFixedRate(new SchedulePersister(scheduledChecks), 5, 15, TimeUnit.SECONDS)

  def scheduleCheck(id : Integer): Unit = {

    if(!schedulerConfig.check_filter.isEmpty) {
      if(!schedulerConfig.check_filter.contains(id)) {
        return
      }
    }

    val rate = checkRepo.get(id).getInterval
    val alerts = collection.mutable.MutableList[Alert]()

    for(ad <- alertRepo.getByCheckId(id)) {
      alerts += new Alert(ad.getId, alertRepo)
    }

    var startDelay = 1L
    var lastScheduled = 0L

    if(schedulerConfig.last_run_persist!=SchedulePersistType.DISABLED && lastScheduleAtStartup != null && lastScheduleAtStartup.contains(id)) {
      lastScheduled = lastScheduleAtStartup.getOrElse(id, 0L)
      startDelay += math.max(rate - (System.currentTimeMillis() - lastScheduled) / 1000, 0)
    }

    val check = new ScheduledCheck(queueSelector, rate,lastScheduled, new Check(id, checkRepo), alerts, entityRepo)
    scheduledChecks.put(id, check)
    service.scheduleAtFixedRate(check, startDelay, rate, TimeUnit.SECONDS)
  }
}
