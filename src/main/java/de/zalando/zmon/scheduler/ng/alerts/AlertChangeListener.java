package de.zalando.zmon.scheduler.ng.alerts;

/**
 * Created by jmussler on 4/17/15.
 */
public interface AlertChangeListener {
    void notifyAlertNew(AlertDefinition alert);
    void notifyAlertChange(AlertDefinition alert);
    void notifyAlertDelete(AlertDefinition alert);
}
