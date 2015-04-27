package de.zalando.zmon.scheduler.ng

import java.io.{FileWriter, OutputStreamWriter}
import java.io.File
import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture, TimeUnit, ScheduledThreadPoolExecutor}

import com.codahale.metrics.{Meter, MetricRegistry}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import de.zalando.zmon.scheduler.ng.alerts.{AlertRepository, AlertDefinition, AlertSourceRegistry}
import de.zalando.zmon.scheduler.ng.checks.{CheckChangeListener, CheckRepository, CheckDefinition, CheckSourceRegistry}
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
  def overlaps(filter: java.util.Map[String, String], entity : java.util.Map[String, Object]) : Boolean = {
      for ((k,v) <- filter) {
        if (!entity.containsKey(k)) {
          return false
        }

        val eV = entity.get(k)
        eV match {
          case x : java.util.Collection[String] =>
            if(!x.contains(v)) {
              return false
            }
          case _ =>
            if(!eV.equals(v)) return false
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
    val entityFilters = repo.get(id).getEntities
    if ( entityFilters.size()==0 ) {
      val excludeEntityFilter = repo.get(id).getEntitiesExclude
      if(excludeEntityFilter!=null) {
        for (outFilter <- excludeEntityFilter) {
          if (filter.overlaps(outFilter, properties)) {
            return false
          }
        }
      }
      return true
    }

    for(inFilter <- entityFilters) {
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

class ScheduledCheck(val id : Integer,
                     private val selector: QueueSelector,
                     private val checkRepo: CheckRepository,
                     private val alertRepo: AlertRepository,
                     val entityRepo : EntityRepository)
                    (implicit private val config : SchedulerConfig, private val metrics: SchedulerMetrics) extends Runnable {

  var lastRun : Long = 0
  val check = new Check(id, checkRepo)
  val checkMeter : Meter = if (config.check_detail_metrics) metrics.metrics.meter("scheduler.check."+check.id) else null

  private var taskFuture : ScheduledFuture[_] = null

  def getAlerts(): mutable.MutableList[Alert] = {
    val alerts = collection.mutable.MutableList[Alert]()

    for(ad <- alertRepo.getByCheckId(id)) {
      alerts += new Alert(ad.getId, alertRepo)
    }

    alerts
  }

  def schedule(service : ScheduledExecutorService, delay: Long): Unit = {
    this.synchronized {
      if(taskFuture == null && delay > 0) {
        // set last run to roughly last execution during first scheduling
        lastRun = System.currentTimeMillis() - (check.getCheckDef.getInterval * 1000L - delay * 1000L)
      }
      if(taskFuture != null) taskFuture.cancel(false) // this should only happen for imidiate evaluation triggered by UI
      taskFuture = service.scheduleAtFixedRate(this, delay, check.getCheckDef.getInterval, TimeUnit.SECONDS)
    }
  }

  @volatile
  var cancel : Boolean = false

  def execute(entity : Entity, alerts : ArrayBuffer[Alert]): Unit = {
    if(cancel) {
      taskFuture.cancel(false)
      ScheduledCheck.LOG.info("canceling future execs of: " + check.id)
      return
    }

    // ScheduledCheck.LOG.info("Scheduling: " + id + " after: " + ((System.currentTimeMillis()-lastRun)/1000.0))

    lastRun = System.currentTimeMillis()
    if(checkMeter != null) {
      checkMeter.mark()
    }

    selector.execute()(entity, check, alerts)
    metrics.totalChecks.mark()
  }

  val lastRunEntities : mutable.ArrayBuffer[Entity]= new ArrayBuffer[Entity]()

  def runCheck(dryRun : Boolean = false) : mutable.ArrayBuffer[Entity] = {
    lastRunEntities.clear()

    for (entity <- entityRepo.get()) {
      if (check.matchEntity(entity)) {
        val viableAlerts = ArrayBuffer[Alert]()
        for (alert <- getAlerts()) {
          if (alert.matchEntity(entity)) {
            viableAlerts += alert
          }
        }

        if (!viableAlerts.isEmpty) {
          if(!dryRun) execute(entity, viableAlerts)
          lastRunEntities.add(entity)
        }
      }
    }

    return lastRunEntities
  }

  override def run(): Unit = {
    try {
      runCheck()
    }
    catch {
      case e : Exception => ScheduledCheck.LOG.error("", e)
    }
  }
}

object SchedulerFactory {
  val LOG = LoggerFactory.getLogger(SchedulerFactory.getClass)
}

class CheckChangedListener(val scheduler : Scheduler) extends CheckChangeListener {
  override def notifyNewCheck(repo: CheckRepository, checkId: Int): Unit = {
    scheduler.schedule(checkId, 0)
  }

  override def notifyCheckIntervalChange(repo: CheckRepository, checkId: Int): Unit = {
    scheduler.executeImmediate(checkId)
  }
}

@Configuration
class SchedulerFactory {
  @Bean
  @Autowired
  def createCheckChangeListener(checkRepo: CheckRepository, scheduler: Scheduler): CheckChangedListener = {
    val listener = new CheckChangedListener(scheduler)
    checkRepo.registerListener(listener)
    listener
  }

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

    val instantEvalListener = new RedisInstantEvalSubscriber(s, schedulerConfig, alertRepo)
    val downtimeEvalListener = new RedisDownTimeSubscriber(s, schedulerConfig, alertRepo)
    val trialRunSubscriber = new TrialRunSubscriber(s, schedulerConfig)

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
    try {
      mapper.readValue(new File("schedule.json"), new TypeReference[Map[Integer, Long]] {})
    }
    catch {
      case e: Exception => {
        return Map[Integer,Long]()
      }
    }
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

  private val service = new ScheduledThreadPoolExecutor(schedulerConfig.thread_count)
  private val scheduledChecks = scala.collection.concurrent.TrieMap[Integer, ScheduledCheck]()

  implicit val schedulerMetrics = new SchedulerMetrics()
  val lastScheduleAtStartup = SchedulePersister.loadSchedule()

  service.scheduleAtFixedRate(new SchedulePersister(scheduledChecks), 5, 15, TimeUnit.SECONDS)

  def viableCheck(id : Integer) : Boolean = {
    if(!schedulerConfig.check_filter.isEmpty) {
      if(!schedulerConfig.check_filter.contains(id)) {
        return false
      }
    }
    true
  }

  def schedule(id: Integer, delay: Long) : Long = {
    this.synchronized {
      var scheduledCheck = scheduledChecks.getOrElse(id, null)
      if(scheduledCheck==null){
        scheduledCheck = new ScheduledCheck(id, queueSelector, checkRepo, alertRepo, entityRepo)
        scheduledChecks.put(id, scheduledCheck)
      }
      val result = scheduledCheck.lastRun
      scheduledCheck.schedule(service, delay)
      return result
    }
  }

  def executeImmediate(id : Integer): Unit = {
    if(!viableCheck(id)) return

    val lastRun = schedule(id, 0)
    Scheduler.LOG.info("Schedule for immediate execution: " + id + " last run: " + ((System.currentTimeMillis()-lastRun)/1000)+"s ago")
  }

  def scheduleCheck(id : Integer): Unit = {
    if(!viableCheck(id)) return

    val rate = checkRepo.get(id).getInterval
    var startDelay = 1L
    var lastScheduled = 0L

    if(schedulerConfig.last_run_persist != SchedulePersistType.DISABLED
         && lastScheduleAtStartup != null
         && lastScheduleAtStartup.contains(id)) {
      lastScheduled = lastScheduleAtStartup.getOrElse(id, 0L)
      startDelay += math.max(rate - (System.currentTimeMillis() - lastScheduled) / 1000, 0)
    }
    else {
      startDelay = (rate.doubleValue() * math.random).toLong; // try to distribute everything along one interval
    }

    schedule(id, startDelay)
  }

  private def getEntitiesForTrialRun(includeFilter : java.util.List[java.util.Map[String,String]], excludeFilters : java.util.List[java.util.Map[String,String]]): ArrayBuffer[Entity] = {
    val entityList : ArrayBuffer[Entity] = new ArrayBuffer[Entity]()
    for ( e <- entityRepo.get() ) {
      for( oneFilter <- includeFilter) {
        if (filter.overlaps(oneFilter, e.getFilterProperties)) {
          if(excludeFilters!=null && excludeFilters.size()>0) {
            var exclude = false
            for(exFilter <- excludeFilters) {
              if(filter.overlaps(exFilter, e.getFilterProperties)) {
                exclude=true
              }
            }
            if(!exclude) {
              entityList.add(e)
            }
          }
          else {
            entityList.add(e)
          }
        }
      }
    }
    entityList
  }

  def scheduleTrialRun(request  : TrialRunRequest) : Unit = {
    val entities = getEntitiesForTrialRun(request.entities, request.entities_exclude)

    var jedis : Jedis = null;
    try {
      jedis = new Jedis(schedulerConfig.redis_host, schedulerConfig.redis_port)
      val redisEntityKey = "zmon:trial_run:" + request.id
      for (entity <- entities) {
        jedis.sadd(redisEntityKey, entity.getId)
      }

      for (entity <- entities) {
        val command = CommandWriter.writeTrialRun(entity, request)
        queueSelector.execute(command, schedulerConfig.trial_run_queue)(entity)
      }
    }
    finally {
      if(null!=jedis) {
        jedis.close()
      }
    }
  }
}