package de.zalando.zmon.scheduler.ng.alerts;

/**
 * Created by jmussler on 4/17/15.
 */
public interface AlertChangeListener {
    void notifyAlertNew(AlertRepository repo, int alertId);
    void notifyAlertChange(AlertRepository repo, int alertId);
    void notifyAlertDelete(AlertRepository repo, int alertId);
}
