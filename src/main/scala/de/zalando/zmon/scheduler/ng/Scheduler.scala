package de.zalando.zmon.scheduler.ng

import java.util
import java.util.concurrent.{TimeUnit, ScheduledThreadPoolExecutor}

import com.codahale.metrics.MetricRegistry
import de.zalando.zmon.scheduler.ng.alerts.{AlertRepository, AlertDefinition, AlertSourceRegistry}
import de.zalando.zmon.scheduler.ng.checks.{CheckRepository, CheckDefinition, CheckSourceRegistry}
import de.zalando.zmon.scheduler.ng.entities.{EntityRepository, Entity, EntityAdapterRegistry}

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
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
  var enabled = false
  var lastExecuted: Long = 0

  def matchEntity(entity: Entity): Boolean = {
    val properties = entity.getProperties
    for (filterMap <- repo.get(id).getEntities) { // OR on entity level definition in checks
      if(filter.overlaps(filterMap, properties)) {
        return true
      }
    }
    false
  }
}

class Alert(var id : Integer, val repo : AlertRepository) {
  var enabled = false

  def matchEntity(entity : Entity): Boolean = {
    val properties = entity.getProperties
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

class ScheduledCheck(private val rate : Long, private val check : Check, private val alerts : mutable.MutableList[Alert], entityRepo : EntityRepository, private val metrics: SchedulerMetrics) extends Runnable {

  val checkMeter = metrics.metrics.meter("scheduler.check."+check.id)

  def execute(entity : Entity, alerts : ArrayBuffer[Alert]): Unit = {
    checkMeter.mark()
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
        execute(entity, viableAlerts)
      }
    }
  }
}

object SchedulerFactory {
  val LOG = LoggerFactory.getLogger(SchedulerFactory.getClass)
}

@Configuration
class SchedulerFactory {
  @Bean
  @Autowired
  def createScheduler(alertRepo : AlertRepository, checkRepo: CheckRepository, entityRepo : EntityRepository, metrics: MetricRegistry) : Scheduler = {
    SchedulerFactory.LOG.info("Createing scheduler instance")
    val s = new Scheduler(alertRepo, checkRepo, entityRepo, metrics)
    SchedulerFactory.LOG.info("Initial scheduling of all checks")
    for(cd <- checkRepo.get()) {
      s.scheduleCheck(cd.getId)
    }
    s
  }
}

class SchedulerMetrics(val metrics : MetricRegistry) {
  val totalChecks = metrics.meter("scheduler.total-checks")
}


class Scheduler(val alertRepo : AlertRepository, val checkRepo: CheckRepository, val entityRepo : EntityRepository, val metrics: MetricRegistry) {

  val service = new ScheduledThreadPoolExecutor(8)
  val scheduledChecks = collection.mutable.HashMap[Integer, ScheduledCheck]()
  val schedulerMetrics = new SchedulerMetrics(metrics)

  def scheduleCheck(id : Integer): Unit = {
    val rate = checkRepo.get(id).getInterval
    val alerts = collection.mutable.MutableList[Alert]()
    for(ad <- alertRepo.getByCheckId(id)) {
      alerts += new Alert(ad.getId, alertRepo)
    }

    val check = new ScheduledCheck(rate, new Check(id, checkRepo), alerts, entityRepo, schedulerMetrics)
    scheduledChecks.put(id, check)

    service.scheduleAtFixedRate(check, 1, rate, TimeUnit.SECONDS)
  }

}
