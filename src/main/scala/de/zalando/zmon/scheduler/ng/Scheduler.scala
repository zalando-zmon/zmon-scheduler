package de.zalando.zmon.scheduler.ng

import java.io.File
import java.net.InetAddress
import java.util
import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}

import com.codahale.metrics.{Meter, MetricRegistry}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository
import de.zalando.zmon.scheduler.ng.checks.{CheckChangedListener, CheckRepository}
import de.zalando.zmon.scheduler.ng.cleanup.{AllTrialRunCleanupTask, TrialRunCleanupTask}
import de.zalando.zmon.scheduler.ng.downtimes.{DowntimeService, DowntimeForwarder}
import de.zalando.zmon.scheduler.ng.entities.{Entity, EntityRepository}
import de.zalando.zmon.scheduler.ng.instantevaluations.{InstantEvalHttpSubscriber, InstantEvalForwarder}
import de.zalando.zmon.scheduler.ng.queue.QueueSelector
import de.zalando.zmon.scheduler.ng.scheduler.{SchedulePersister, ScheduledCheck, SchedulerMetrics, RedisMetricsUpdater}
import de.zalando.zmon.scheduler.ng.trailruns.{TrialRunRequest, TrialRunHttpSubscriber, TrialRunForwarder}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.scheduling.concurrent.CustomizableThreadFactory
import org.springframework.web.client.RestTemplate
import redis.clients.jedis.Jedis

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by jmussler on 3/27/15.
  */

object filter {
  def overlaps(filter: java.util.Map[String, String], entity: java.util.Map[String, Object]): Boolean = {
    for ((k, v) <- filter) {
      if (v == null) {
        Scheduler.LOG.error("Filtering for null value with key: " + k);
      }

      if (!entity.containsKey(k)) {
        return false
      }

      val eV = entity.get(k)
      if (null == eV) {
        Scheduler.LOG.error("Filtering for entity null value with key: " + k);
      }
      eV match {
        case x: java.util.Collection[String] =>
          if (!x.contains(v)) {
            return false
          }
        case _ =>
          if (!eV.equals(v)) return false
      }
    }
    true
  }
}

object Scheduler {
  val LOG = LoggerFactory.getLogger(Scheduler.getClass())
}

class Scheduler(val alertRepo: AlertRepository, val checkRepo: CheckRepository, val entityRepo: EntityRepository, val queueSelector: QueueSelector)
               (implicit val schedulerConfig: SchedulerConfig, val metrics: MetricRegistry) {

  private val service = new ScheduledThreadPoolExecutor(schedulerConfig.thread_count, new CustomizableThreadFactory("scheduler-pool"))
  private val shortIntervalService = new ScheduledThreadPoolExecutor(schedulerConfig.thread_count)
  private val scheduledChecks = scala.collection.concurrent.TrieMap[Integer, ScheduledCheck]()
  private val taskSerializer = new JavaCommandSerializer(schedulerConfig.task_serializer)

  implicit val schedulerMetrics = new SchedulerMetrics(metrics)
  val lastScheduleAtStartup = SchedulePersister.loadSchedule()

  // service.scheduleAtFixedRate(new SchedulePersister(scheduledChecks), 5, 15, TimeUnit.SECONDS)
  service.scheduleAtFixedRate(new RedisMetricsUpdater(schedulerConfig, schedulerMetrics), 5, 3, TimeUnit.SECONDS)
  service.schedule(new AllTrialRunCleanupTask(schedulerConfig), 10, TimeUnit.SECONDS);

  def viableCheck(id: Integer): Boolean = {
    if (0 == id) return false;
    if (schedulerConfig.check_filter != null && !schedulerConfig.check_filter.isEmpty) {
      if (!schedulerConfig.check_filter.contains(id)) {
        return false
      }
    }
    true
  }

  def unschedule(id: Integer): Unit = {
    this.synchronized {
      val scheduledCheck = scheduledChecks.getOrElse(id, null)
      if (null != scheduledCheck) {
        scheduledCheck.cancelExecution()
      }
    }
  }

  def schedule(id: Integer, delay: Long): Long = {
    this.synchronized {
      var scheduledCheck = scheduledChecks.getOrElse(id, null)
      if (scheduledCheck == null) {
        scheduledCheck = new ScheduledCheck(id, queueSelector, checkRepo, alertRepo, entityRepo, schedulerConfig, schedulerMetrics)
        scheduledChecks.put(id, scheduledCheck)
      }
      val result = scheduledCheck.getLastRun
      if (checkRepo.get(id).getInterval < 30) {
        scheduledCheck.schedule(shortIntervalService, delay)
      }
      else {
        scheduledCheck.schedule(service, delay)
      }
      return result
    }
  }

  def executeImmediate(checkId: Integer): Unit = {
    if (!viableCheck(checkId)) return
    try {
      val lastRun = schedule(checkId, 0)
      Scheduler.LOG.info("Schedule for immediate execution: checkId=" + checkId + " last run: " + ((System.currentTimeMillis() - lastRun) / 1000) + "s ago")
    }
    catch {
      case ex: Exception => Scheduler.LOG.error("Unexpected exception in executeImmediate for check_id: checkId=" + checkId, ex)
    }
  }

  def scheduleCheck(id: Integer): Unit = {
    if (!viableCheck(id)) return

    val rate = checkRepo.get(id).getInterval
    var startDelay = 1L
    var lastScheduled = 0L

    if (schedulerConfig.last_run_persist != SchedulePersistType.DISABLED
      && lastScheduleAtStartup != null
      && lastScheduleAtStartup.contains(id)) {
      lastScheduled = lastScheduleAtStartup.getOrDefault(id, 0L)
      startDelay += math.max(rate - (System.currentTimeMillis() - lastScheduled) / 1000, 0)
    }
    else {
      startDelay = (rate.doubleValue() * math.random).toLong; // try to distribute everything along one interval
    }

    schedule(id, startDelay)
  }

  def queryKnownEntities(filter: java.util.List[java.util.Map[String, String]], excludeFilter: java.util.List[java.util.Map[String, String]], applyBaseFilter: Boolean): util.List[Entity] = {
    var entities: ArrayBuffer[Entity] = null

    if (applyBaseFilter) {
      entities = getEntitiesForTrialRun(entityRepo.get(), filter, excludeFilter)
    }
    else {
      entities = getEntitiesForTrialRun(entityRepo.getUnfiltered(), filter, excludeFilter)
    }

    return bufferAsJavaList(entities)
  }

  private def getEntitiesForTrialRun(entityBase: java.util.Collection[Entity], includeFilter: java.util.List[java.util.Map[String, String]], excludeFilters: java.util.List[java.util.Map[String, String]]): ArrayBuffer[Entity] = {
    val entityList: ArrayBuffer[Entity] = new ArrayBuffer[Entity]()
    for (e <- entityBase) {
      for (oneFilter <- includeFilter) {
        if (filter.overlaps(oneFilter, e.getFilterProperties)) {
          if (excludeFilters != null && excludeFilters.size() > 0) {
            var exclude = false
            for (exFilter <- excludeFilters) {
              if (filter.overlaps(exFilter, e.getFilterProperties)) {
                exclude = true
              }
            }
            if (!exclude) {
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

  def scheduleTrialRun(request: TrialRunRequest): Unit = {
    val entitiesGlobal = getEntitiesForTrialRun(entityRepo.getUnfiltered(), request.entities, request.entitiesExclude)
    val entitiesLocal = getEntitiesForTrialRun(entityRepo.get(), request.entities, request.entitiesExclude)
    Scheduler.LOG.info("Trial run matched entities: global=" + entitiesGlobal.size + " local=" + entitiesLocal.size)

    var jedis: Jedis = null;
    try {
      jedis = new Jedis(schedulerConfig.redis_host, schedulerConfig.redis_port)
      val redisEntityKey = "zmon:trial_run:" + request.id
      for (entity <- entitiesGlobal) {
        jedis.sadd(redisEntityKey, entity.getId)
      }

      for (entity <- entitiesLocal) {
        val command = taskSerializer.writeTrialRun(entity, request)
        queueSelector.execute(entity, command, schedulerConfig.trial_run_queue)
      }
    }
    finally {
      service.schedule(new TrialRunCleanupTask(request.id, schedulerConfig), 300, TimeUnit.SECONDS)
      if (null != jedis) {
        jedis.close()
      }
    }
  }
}