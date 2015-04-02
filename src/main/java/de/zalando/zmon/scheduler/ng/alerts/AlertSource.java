package de.zalando.zmon.scheduler.ng.alerts;

import java.util.Collection;

/**
 * Created by jmussler on 4/2/15.
 */
public interface AlertSource {
    String getName();
    Collection<AlertDefinition> getAlerts();
}
