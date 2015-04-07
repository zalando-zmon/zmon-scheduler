package de.zalando.zmon.scheduler.ng.alerts;

import de.zalando.zmon.scheduler.ng.Source;

import java.util.Collection;

/**
 * Created by jmussler on 4/2/15.
 */
public interface AlertSource extends Source {
    Collection<AlertDefinition> getAlerts();
}
