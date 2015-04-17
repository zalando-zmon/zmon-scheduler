package de.zalando.zmon.scheduler.ng.entities;

/**
 * Created by jmussler on 4/17/15.
 */
public interface EntityChangeListener {
    void notifyEntityChange(EntityRepository repo, String entityId);
}
