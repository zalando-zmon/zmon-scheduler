package de.zalando.zmon.scheduler.ng.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import com.codahale.metrics.MetricRegistry;

import de.zalando.zmon.scheduler.ng.AlertOverlapGenerator;
import de.zalando.zmon.scheduler.ng.JavaCommandSerializer;
import de.zalando.zmon.scheduler.ng.SchedulePersistType;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.cleanup.AllTrialRunCleanupTask;
import de.zalando.zmon.scheduler.ng.cleanup.TrialRunCleanupTask;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import de.zalando.zmon.scheduler.ng.queue.QueueSelector;
import de.zalando.zmon.scheduler.ng.trailruns.TrialRunRequest;
import redis.clients.jedis.Jedis;

/**
 * Created by jmussler on 30.06.16.
 */
public class Scheduler {

    private final static Logger LOG = LoggerFactory.getLogger(Scheduler.class);

    private final AlertRepository alertRepo;
    private final CheckRepository checkRepo;
    private final EntityRepository entityRepo;
    private final QueueSelector queueSelector;
    private final SchedulerConfig schedulerConfig;
    private final SchedulerMetrics schedulerMetrics;

    private final ScheduledThreadPoolExecutor service;
    private final ScheduledThreadPoolExecutor shortIntervalService;

    private final Map<Integer, ScheduledCheck> scheduledChecks = new HashMap<>();

    private final JavaCommandSerializer taskSerializer;
    private final Map<Integer, Long> lastScheduleAtStartup = SchedulePersister.loadSchedule();

    public Scheduler(AlertRepository alertRepo, CheckRepository checkRepo, EntityRepository entityRepository, QueueSelector queueSelector, SchedulerConfig schedulerConfig, MetricRegistry metrics) {
        this.alertRepo = alertRepo;
        this.checkRepo = checkRepo;
        this.entityRepo = entityRepository;
        this.queueSelector = queueSelector;
        this.schedulerConfig = schedulerConfig;
        this.schedulerMetrics = new SchedulerMetrics(metrics);

        taskSerializer = new JavaCommandSerializer(schedulerConfig.getTaskSerializer());

        service = new ScheduledThreadPoolExecutor(schedulerConfig.getThreadCount(), new CustomizableThreadFactory("sc-pool-"));
        service.setRemoveOnCancelPolicy(true);

        shortIntervalService = new ScheduledThreadPoolExecutor(schedulerConfig.getThreadCount(), new CustomizableThreadFactory("sc-pool-fast-"));
        shortIntervalService.setRemoveOnCancelPolicy(true);

        service.scheduleAtFixedRate(new RedisMetricsUpdater(schedulerConfig, schedulerMetrics), 5, 3, TimeUnit.SECONDS);
        service.schedule(new AllTrialRunCleanupTask(schedulerConfig), 10, TimeUnit.SECONDS);
    }

    private boolean viableCheck(int id) {
        if (0 == id) {
            return false;
        }

        if (schedulerConfig.getCheckFilter() != null) {
            /* This is a positive filter, run only checks from the list */
            if (!schedulerConfig.getCheckFilter().contains(id)) {
                return false;
            }
        }
        return true;
    }

    public synchronized void unschedule(int id) {
        ScheduledCheck check = scheduledChecks.getOrDefault(id, null);
        if (null != check) {
            check.cancelExecution();
        }
    }

    public synchronized long schedule(int id, long delay) {
        ScheduledCheck check = scheduledChecks.getOrDefault(id, null);
        if (check == null) {
            check = new ScheduledCheck(id, queueSelector, checkRepo, alertRepo, entityRepo, schedulerConfig, schedulerMetrics);
            scheduledChecks.put(id, check);
        }

        long result = check.getLastRun();
        if (checkRepo.get(id).getInterval() < 30) {
            check.schedule(shortIntervalService, delay);
        }
        else {
            check.schedule(service, delay);
        }
        return result;
    }

    public void scheduleCheck(int id) {
        if (!viableCheck(id)) return;

        long rate = checkRepo.get(id).getInterval();
        long startDelay = 1L;
        long lastScheduled;

        if (schedulerConfig.getLastRunPersist() != SchedulePersistType.DISABLED
                && lastScheduleAtStartup != null
                && lastScheduleAtStartup.containsKey(id)) {
            lastScheduled = lastScheduleAtStartup.getOrDefault(id, 0L);
            startDelay += Math.max(rate - (System.currentTimeMillis() - lastScheduled) / 1000, 0);
        }
        else {
            startDelay = (long)((double)rate * Math.random()); // try to distribute everything along one interval
        }

        schedule(id, startDelay);
    }

    public void executeImmediate(int checkId) {
        if (!viableCheck(checkId)) return;

        try {
            long lastRun = schedule(checkId, 0);
            LOG.info("Schedule for immediate execution: checkId={}  last-run {}s ago", checkId, ((System.currentTimeMillis() - lastRun) / 1000));
        }
        catch (Throwable t) {
            LOG.error("Unexpected exception in executeImmediate for check_id: checkId={}", checkId, t);
        }
    }

    public List<Entity> queryKnownEntities(List<Map<String, String>> filter, List<Map<String, String>> excludeFilter, boolean applyBaseFilter) {
        List<Entity> entities;

        if (applyBaseFilter) {
            entities = getEntitiesForTrialRun(entityRepo.get(), filter, excludeFilter);
        }
        else {
            entities = getEntitiesForTrialRun(entityRepo.getUnfiltered(), filter, excludeFilter);
        }

        return entities;
    }

    private List<Entity> getEntitiesForTrialRun(Collection<Entity> entityBase, List<Map<String, String>> includeFilter, List<Map<String, String>> excludeFilters) {
        List<Entity> entityList = new ArrayList<>();
        for (Entity e :entityBase) {
            for (Map<String, String> oneFilter : includeFilter) {
                if (AlertOverlapGenerator.filter(oneFilter, e.getFilterProperties())) {
                    if (excludeFilters != null && excludeFilters.size() > 0) {
                        boolean exclude = false;
                        for (Map<String, String> exFilter : excludeFilters) {
                            if (AlertOverlapGenerator.filter(exFilter, e.getFilterProperties())) {
                                exclude = true;
                            }
                        }
                        if (!exclude) {
                            entityList.add(e);
                        }
                    }
                    else {
                        entityList.add(e);
                    }
                }
            }
        }
        return entityList;
    }

    public void scheduleTrialRun(TrialRunRequest request) {
        List<Entity> entitiesGlobal = getEntitiesForTrialRun(entityRepo.getUnfiltered(), request.entities, request.entitiesExclude);
        List<Entity> entitiesLocal = getEntitiesForTrialRun(entityRepo.get(), request.entities, request.entitiesExclude);
        Scheduler.LOG.info("Trial run matched entities: global=" + entitiesGlobal.size() + " local=" + entitiesLocal.size());

        try(Jedis jedis = new Jedis(schedulerConfig.getRedisHost(), schedulerConfig.getRedisPort())) {
            String redisEntityKey = "zmon:trial_run:" + request.id;
            for (Entity entity : entitiesGlobal) {
                jedis.sadd(redisEntityKey, entity.getId());
            }

            for (Entity entity : entitiesLocal) {
                byte[] command = taskSerializer.writeTrialRun(entity, request);
                queueSelector.execute(entity, command, schedulerConfig.getTrialRunQueue());
            }
        }
        finally {
            service.schedule(new TrialRunCleanupTask(request.id, schedulerConfig), 300, TimeUnit.SECONDS);
        }
    }
}
