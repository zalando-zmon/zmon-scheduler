package de.zalando.zmon.scheduler.ng.entities;

/**
 * Created by jmussler on 4/17/15.
 */
public interface EntityChangeListener {
    void notifyEntityChange(EntityRepository repo, Entity entityOld, Entity entityNew);
    void notifyEntityRemove(EntityRepository repo, Entity e);
    void notifyEntityAdd(EntityRepository repo, Entity e);
}
