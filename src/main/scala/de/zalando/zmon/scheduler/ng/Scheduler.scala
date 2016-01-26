package de.zalando.zmon.scheduler.ng

import java.io.{FileWriter, OutputStreamWriter}
import java.io.File
import java.net.InetAddress
import java.util
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
        if(v==null) {
          Scheduler.LOG.error("Filtering for null value with key: " + k);
        }

        if (!entity.containsKey(k)) {
          return false
        }

        val eV = entity.get(k)
        if(null==eV) {
          Scheduler.LOG.error("Filtering for entity null value with key: " + k);
        }
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

  def schedule(service : ScheduledExecutorService, delay: Long): Unit = {
    this.synchronized {
      if(taskFuture == null && delay > 0) {
        // set last run to roughly last execution during first scheduling
        lastRun = System.currentTimeMillis() - (check.getCheckDef.getInterval * 1000L - delay * 1000L)
      }

      if(taskFuture != null) {
        taskFuture.cancel(false) // this should only happen for immediate evaluation triggered by UI or interval change
      }

      taskFuture = service.scheduleAtFixedRate(this, delay, check.getCheckDef.getInterval, TimeUnit.SECONDS)
    }
  }

  @volatile
  var cancel : Boolean = false

  def cancelExecution() : Unit = {
    cancel = true
  }

  def execute(entity : Entity, alerts : ArrayBuffer[Alert]): Unit = {
    if(cancel) {
      taskFuture.cancel(false)
      ScheduledCheck.LOG.info("canceling future execs of: " + check.id)
      return
    }

    selector.execute()(entity, check, alerts, lastRun)

    if(checkMeter != null) {
      checkMeter.mark()
    }
    metrics.totalChecks.mark()
  }

  val lastRunEntities : mutable.ArrayBuffer[Entity]= new ArrayBuffer[Entity]()

  def getAlerts(): mutable.MutableList[Alert] = {
    val alerts = collection.mutable.MutableList[Alert]()

    for(ad <- alertRepo.getByCheckId(id)) {
      alerts += new Alert(ad.getId, alertRepo)
    }

    alerts
  }

  def runCheck(dryRun : Boolean = false) : mutable.ArrayBuffer[Entity] = {
    lastRunEntities.clear()
    var setLastRun = false

    val checkDef = check.getCheckDef()
    if(null==checkDef) {
      Scheduler.LOG.error("Probably inactive/deleted check still scheduled: " + check.id)
      return new ArrayBuffer[Entity]()
    }

    if(checkDef.getInterval <= 15 && (System.currentTimeMillis() - lastRun < (checkDef.getInterval * 750L))) {
      // for low interval checks on trial basis skip executions too close to each other (75% of interval)
      // this is only appearing at points where all intervals mix up in huge batch of tasks ( e.g. 180 mark or 300 mark )
      return new ArrayBuffer[Entity]()
    }

    for (entity <- entityRepo.get()) {
      if (check.matchEntity(entity)) {
        val viableAlerts = ArrayBuffer[Alert]()
        for (alert <- getAlerts()) {
          if (alert.matchEntity(entity)) {
            viableAlerts += alert
          }
        }

        if (!viableAlerts.isEmpty) {
          if(!dryRun) {
            if(!setLastRun) {
              // use new last run time across all commands, to allow syncing data retrieved time stamp on worker side
              lastRun = System.currentTimeMillis()
              setLastRun=true
            }
            execute(entity, viableAlerts)
          }
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
      case e : Exception => {
        metrics.errorCount.mark()
        ScheduledCheck.LOG.error("Error in execution of check: " + id, e)
      }
    }
  }
}

object SchedulerFactory {
  val LOG = LoggerFactory.getLogger(SchedulerFactory.getClass)
}

class CheckChangedListener(val scheduler : Scheduler) extends CheckChangeListener {
  override def notifyNewCheck(repo: CheckRepository, checkId: Int): Unit = {
    Scheduler.LOG.info("New check discovered: " + checkId)
    scheduler.schedule(checkId, 0)
  }

  override def notifyCheckIntervalChange(repo: CheckRepository, checkId: Int): Unit = {
    Scheduler.LOG.info("Check interval changed: " + checkId)
    scheduler.executeImmediate(checkId)
  }

  override def notifyDeleteCheck(repo: CheckRepository, checkId : Int): Unit = {
    Scheduler.LOG.info("Check removed or inactive: " + checkId)
    scheduler.unschedule(checkId)
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
  def createScheduler(alertRepo : AlertRepository,
                      checkRepo: CheckRepository,
                      entityRepo : EntityRepository,
                      queueSelector : QueueSelector,
                      instantForwarder : InstantEvalForwarder,
                      trialRunForwarder : TrialRunForwarder,
                      tokenWrapper : TokenWrapper)
                     (implicit schedulerConfig : SchedulerConfig, metrics: MetricRegistry) : Scheduler = {
    SchedulerFactory.LOG.info("Createing scheduler instance")
    val s = new Scheduler(alertRepo, checkRepo, entityRepo, queueSelector)

    SchedulerFactory.LOG.info("Check ID filter: " + schedulerConfig.check_filter)

    SchedulerFactory.LOG.info("Initial scheduling of all checks")
    for(cd <- checkRepo.get()) {
      s.scheduleCheck(cd.getId)
    }
    SchedulerFactory.LOG.info("Initial scheduling of all checks done")

    if(schedulerConfig.enable_instant_eval) {
      val instantEvalListener = new RedisInstantEvalSubscriber(s, schedulerConfig, alertRepo, instantForwarder)
    }

    if(schedulerConfig.enable_downtime_redis_sub) {
      val downtimeEvalListener = new RedisDownTimeSubscriber(s, schedulerConfig, alertRepo)
    }

    if(schedulerConfig.enable_trail_run) {
      val trialRunSubscriber = new TrialRunSubscriber(s, schedulerConfig, trialRunForwarder)
    }

    if(schedulerConfig.instant_eval_forward) {
      entityRepo.registerListener(instantForwarder)
    }

    if(schedulerConfig.trial_run_forward) {
      entityRepo.registerListener(trialRunForwarder)
    }

    if(schedulerConfig.trial_run_http_url!=null) {
      val trialRunPoller = new TrialRunHttpSubscriber(s, schedulerConfig, tokenWrapper);
    }

    if(schedulerConfig.instant_eval_http_url!=null) {
      val instantEvalPoller = new InstantEvalHttpSubscriber(s, schedulerConfig, tokenWrapper);
    }

    s
  }
}

class SchedulerMetrics(implicit val metrics : MetricRegistry) {
  val totalChecks = metrics.meter("scheduler.total-checks")
  val errorCount = metrics.meter("scheduler.total-errors")
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

class RedisMetricsUpdater(val config : SchedulerConfig, val metrics : SchedulerMetrics) extends Runnable {
  val name = "s-p"+config.server_port+"."+InetAddress.getLocalHost.getHostName()

  override def run(): Unit = {
    try {
      val jedis = new Jedis(config.redis_host, config.redis_port);
      val p = jedis.pipelined()
      p.sadd("zmon:metrics", name)
      p.set("zmon:metrics:" + name + ":check.count", metrics.totalChecks.getCount + "")
      p.set("zmon:metrics:" + name + ":ts", System.currentTimeMillis()/1000 + "")
      p.sync()
    }
    catch {
      case e: Exception => {
        Scheduler.LOG.error("", e)
      }
    }
  }
}

class Scheduler(val alertRepo : AlertRepository, val checkRepo: CheckRepository, val entityRepo : EntityRepository, val queueSelector : QueueSelector)
               (implicit val schedulerConfig: SchedulerConfig, val metrics: MetricRegistry) {

  private val service = new ScheduledThreadPoolExecutor(schedulerConfig.thread_count)
  private val shortIntervalService = new ScheduledThreadPoolExecutor(schedulerConfig.thread_count)
  private val scheduledChecks = scala.collection.concurrent.TrieMap[Integer, ScheduledCheck]()
  private val taskSerializer = new CommandSerializer(schedulerConfig.task_serializer)

  implicit val schedulerMetrics = new SchedulerMetrics()
  val lastScheduleAtStartup = SchedulePersister.loadSchedule()

  service.scheduleAtFixedRate(new SchedulePersister(scheduledChecks), 5, 15, TimeUnit.SECONDS)
  service.scheduleAtFixedRate(new RedisMetricsUpdater(schedulerConfig, schedulerMetrics), 5, 1, TimeUnit.SECONDS)

  def viableCheck(id : Integer) : Boolean = {
    if(0 == id) return false;
    if(schedulerConfig.check_filter != null && !schedulerConfig.check_filter.isEmpty) {
      if(!schedulerConfig.check_filter.contains(id)) {
        return false
      }
    }
    true
  }

  def unschedule(id: Integer): Unit = {
    this.synchronized {
      val scheduledCheck = scheduledChecks.getOrElse(id, null)
      if(null != scheduledCheck) {
        scheduledCheck.cancelExecution()
      }
    }
  }

  def schedule(id: Integer, delay: Long) : Long = {
    this.synchronized {
      var scheduledCheck = scheduledChecks.getOrElse(id, null)
      if (scheduledCheck == null) {
        scheduledCheck = new ScheduledCheck(id, queueSelector, checkRepo, alertRepo, entityRepo)
        scheduledChecks.put(id, scheduledCheck)
      }
      val result = scheduledCheck.lastRun
      if (checkRepo.get(id).getInterval < 30) {
        scheduledCheck.schedule(shortIntervalService, delay)
      }
      else {
        scheduledCheck.schedule(service, delay)
      }
      return result
    }
  }

  def executeImmediate(id : Integer): Unit = {
    if(!viableCheck(id)) return
    try {
      val lastRun = schedule(id, 0)
      Scheduler.LOG.info("Schedule for immediate execution: " + id + " last run: " + ((System.currentTimeMillis() - lastRun) / 1000) + "s ago")
    }
    catch {
      case ex : Exception => Scheduler.LOG.error("Unexpected exception in execImmediate for check_id: " + id, ex)
    }
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

  def queryKnownEntities(filter: java.util.List[java.util.Map[String,String]], excludeFilter: java.util.List[java.util.Map[String,String]], applyBaseFilter : Boolean ) : util.List[Entity] = {
    var entities : ArrayBuffer[Entity]= null

    if(applyBaseFilter) {
      entities = getEntitiesForTrialRun(entityRepo.get(), filter, excludeFilter)
    }
    else {
      entities = getEntitiesForTrialRun(entityRepo.getUnfiltered(), filter, excludeFilter)
    }

    return bufferAsJavaList(entities)
  }

  private def getEntitiesForTrialRun(entityBase: java.util.Collection[Entity], includeFilter : java.util.List[java.util.Map[String,String]], excludeFilters : java.util.List[java.util.Map[String,String]]): ArrayBuffer[Entity] = {
    val entityList : ArrayBuffer[Entity] = new ArrayBuffer[Entity]()
    for ( e <- entityBase ) {
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
    val entitiesGlobal = getEntitiesForTrialRun(entityRepo.getUnfiltered(), request.entities, request.entities_exclude)
    val entitiesLocal = getEntitiesForTrialRun(entityRepo.get(), request.entities, request.entities_exclude)
    Scheduler.LOG.info("Trial run matched entities: global=" + entitiesGlobal.size+" local="+entitiesLocal.size)

    var jedis : Jedis = null;
    try {
      jedis = new Jedis(schedulerConfig.redis_host, schedulerConfig.redis_port)
      val redisEntityKey = "zmon:trial_run:" + request.id
      for (entity <- entitiesGlobal) {
        jedis.sadd(redisEntityKey, entity.getId)
      }

      for (entity <- entitiesLocal) {
        val command = taskSerializer.writeTrialRun(entity, request)
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