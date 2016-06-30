package de.zalando.zmon.scheduler.ng.scheduler;

import com.codahale.metrics.Meter;
import de.zalando.zmon.scheduler.ng.Alert;
import de.zalando.zmon.scheduler.ng.AlertOverlapGenerator;
import de.zalando.zmon.scheduler.ng.Check;
import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import de.zalando.zmon.scheduler.ng.queue.QueueSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by jmussler on 30.06.16.
 */
public class ScheduledCheck implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ScheduledCheck.class);

    private long lastRun = 0;
    private final int id;
    private final Check check;
    private final Meter meter;
    private final SchedulerConfig config;
    private final AlertRepository alertRepo;
    private final CheckRepository checkRepo;
    private final EntityRepository entityRepo;
    private final QueueSelector selector;
    private final SchedulerMetrics metrics;

    private ScheduledFuture taskFuture = null;
    private volatile boolean cancel = false;

    private final List<Entity> lastRunEntities = new ArrayList<>(0);

    public ScheduledCheck(int id, QueueSelector selector, CheckRepository checkRepo, AlertRepository alertRepo,
                          EntityRepository entityRepo, SchedulerConfig config, SchedulerMetrics metrics) {

        this.id = id;
        this.alertRepo = alertRepo;
        this.entityRepo = entityRepo;
        this.checkRepo = checkRepo;
        this.config = config;
        this.selector = selector;
        this.metrics = metrics;

        this.check = new Check(id, checkRepo);
        if(config.isCheckDetailMetrics()) {
            this.meter = metrics.getMetrics().meter("scheduler.check." + id);
        }
        else {
            this.meter = null;
        }
    }

    public synchronized void schedule(ScheduledExecutorService service, long delay) {
        if (null == taskFuture && delay > 0) {
            lastRun = System.currentTimeMillis() - (check.getCheckDefinition().getInterval() * 1000L - delay * 1000L);
        }

        if (null != taskFuture) {
            taskFuture.cancel(false);
        }

        taskFuture = service.scheduleAtFixedRate(this, delay, check.getCheckDefinition().getInterval(), TimeUnit.SECONDS);
    }

    public void execute(Entity entity, Collection<Alert> alerts) {
        if (cancel) {
            taskFuture.cancel(false);
            LOG.info("Canceling future executions of: id={}", check.getId());
            return;
        }

        selector.execute(entity, check, alerts, lastRun);


        if (null != meter) {
            meter.mark();
        }
        metrics.incTotal();
    }

    public Collection<Alert> getAlerts() {
        return alertRepo.getByCheckId(check.getId()).stream().map(x -> new Alert(x.getId(), alertRepo)).collect(Collectors.toList());
    }

    public List<Entity> runCheck() {
        return runCheck(false);
    }

    public List<Entity> runCheck(boolean dryRun) {
        lastRunEntities.clear();

        boolean setLastRun = false;
        CheckDefinition checkDef = check.getCheckDefinition();
        if (null == checkDef) {
            LOG.warn("Probably inactive/deleted check still scheduled: " + check.getId());
            return lastRunEntities;
        }

        if (checkDef.getInterval() <= 15 && (System.currentTimeMillis() - lastRun < (checkDef.getInterval() * 750L))) {
            // skip high frequency checks too close to last execution
            return lastRunEntities;
        }

        for(Entity entity : entityRepo.get()) {
            if (AlertOverlapGenerator.matchCheckFilter(checkDef, entity)) {
                List<Alert> viableAlerts = getAlerts().stream().filter(x->x.matchEntity(entity)).collect(Collectors.toList());

                if(!viableAlerts.isEmpty()) {
                    if(!dryRun) {
                        if(!setLastRun) {
                            lastRun = System.currentTimeMillis();
                            setLastRun = true;
                        }
                        execute(entity, viableAlerts);
                    }
                    lastRunEntities.add(entity);
                }
            }
        }

        return lastRunEntities;
    }

    @Override
    public void run() {
        try {
            runCheck();
        }
        catch(Throwable t) {
            metrics.incError();
            LOG.error("Error in check execution: checkId={} msg={}", id, t.getMessage());
        }
    }

    public void cancelExecution() {
        cancel = true;
    }

    public long getLastRun() {
        return lastRun;
    }
}
