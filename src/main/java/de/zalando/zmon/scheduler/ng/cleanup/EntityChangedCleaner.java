package de.zalando.zmon.scheduler.ng.cleanup;

import de.zalando.zmon.scheduler.ng.AlertOverlapGenerator;
import de.zalando.zmon.scheduler.ng.alerts.AlertDefinition;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityChangeListener;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by jmussler on 06.06.16.
 */
public class EntityChangedCleaner implements EntityChangeListener {

    private final static Logger LOG = LoggerFactory.getLogger(EntityChangedCleaner.class);

    private final AlertRepository alertRepo;
    private final CheckRepository checkRepo;
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private final AlertChangedCleaner alertCleaner;

    public EntityChangedCleaner(AlertRepository alertRepo, CheckRepository checkRepo, AlertChangedCleaner alertCleaner) {
        this.alertRepo = alertRepo;
        this.checkRepo = checkRepo;
        this.alertCleaner = alertCleaner;
    }

    private class EntityChangeCleanupTask implements Runnable {
        private final Entity oldEntity;
        private final Entity newEntity;

        public EntityChangeCleanupTask(Entity oldEntity, Entity newEntity) {
            this.oldEntity = oldEntity;
            this.newEntity = newEntity;
        }

        @Override
        public void run() {
            try {
                for (CheckDefinition cd : checkRepo.get()) {
                    if (AlertOverlapGenerator.matchCheckFilter(cd, oldEntity)) {
                        for (AlertDefinition ad : alertRepo.getByCheckId(cd.getId())) {
                            if (AlertOverlapGenerator.matchAlertFilter(ad, oldEntity)) {
                                if (!AlertOverlapGenerator.matchCheckFilter(cd, newEntity)
                                        || !AlertOverlapGenerator.matchAlertFilter(ad, newEntity)) {
                                    // reuse code, this cleans up all non matching entities
                                    alertCleaner.notifyAlertChange(ad);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                LOG.error("Failed to cleanup changed entity: id={}", newEntity.getId());
            }
        }
    }

    @Override
    public void notifyEntityAdd(EntityRepository repo, Entity e) {

    }

    @Override
    public void notifyEntityRemove(EntityRepository repo, Entity e) {

    }

    protected void notifyEntityChangeNoWait(EntityRepository repo, Entity entityOld, Entity entityNew) {
        executor.schedule(new EntityChangeCleanupTask(entityOld, entityNew), 0, TimeUnit.SECONDS);
    }

    @Override
    public void notifyEntityChange(EntityRepository repo, Entity entityOld, Entity entityNew) {
        executor.schedule(new EntityChangeCleanupTask(entityOld, entityNew), 5, TimeUnit.SECONDS);
        executor.schedule(new EntityChangeCleanupTask(entityOld, entityNew), 60, TimeUnit.SECONDS);
    }
}
