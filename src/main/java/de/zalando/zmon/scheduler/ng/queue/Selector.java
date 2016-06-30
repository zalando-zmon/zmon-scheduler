package de.zalando.zmon.scheduler.ng.queue;

import de.zalando.zmon.scheduler.ng.Alert;
import de.zalando.zmon.scheduler.ng.Check;
import de.zalando.zmon.scheduler.ng.entities.Entity;

import java.util.Collection;

/**
 * Created by jmussler on 30.06.16.
 */
public interface Selector {
    String getQueue(Entity entity, Check check, Collection<Alert> alerts);
}
