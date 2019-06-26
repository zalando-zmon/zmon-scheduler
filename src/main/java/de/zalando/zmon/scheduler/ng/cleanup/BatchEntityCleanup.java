package de.zalando.zmon.scheduler.ng.cleanup;

import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityChangeListener;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import de.zalando.zmon.scheduler.ng.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class BatchEntityCleanup implements EntityChangeListener {
    private Scheduler scheduler;
    private final static Logger LOG = LoggerFactory.getLogger(BatchEntityCleanup.class);

    public BatchEntityCleanup(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void notifyEntityChange(EntityRepository repo, Entity entityOld, Entity entityNew) {

    }

    @Override
    public void notifyEntityRemove(EntityRepository repo, Entity e) {
    }

    @Override
    public void notifyBatchEntityRemove(EntityRepository repo, Set<String> removedEntities) {
        scheduler.scheduleEntityCleanUp(removedEntities);
        LOG.info("Batch cleanup scheduled for {} entities", removedEntities.size());
    }

    @Override
    public void notifyEntityAdd(EntityRepository repo, Entity e) {

    }
}
