package de.zalando.zmon.scheduler.ng.cleanup;

import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityChangeListener;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;

import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created by jmussler on 06.06.16.
 */
public class EntityChangedCleaner implements EntityChangeListener {

    private final AlertRepository alertRepo;
    private final CheckRepository checkRepo;
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private final SchedulerConfig config;

    public EntityChangedCleaner(AlertRepository alertRepo, CheckRepository checkRepo, SchedulerConfig config) {
        this.alertRepo = alertRepo;
        this.checkRepo = checkRepo;
        this.config = config;
    }

    private class EntityChangeCleanupTask {
        private final Entity oldEntity;
        private final Entity newEntity;

        public EntityChangeCleanupTask(Entity oldEntity, Entity newEntity) {
            this.oldEntity = oldEntity;
            this.newEntity = newEntity;
        }
    }

    @Override
    public void notifyEntityAdd(EntityRepository repo, Entity e) {

    }

    @Override
    public void notifyEntityRemove(EntityRepository repo, Entity e) {

    }

    @Override
    public void notifyEntityChange(EntityRepository repo, Entity entityOld, Entity entityNew) {

    }
}
