package de.zalando.zmon.scheduler.ng.alerts;

import de.zalando.zmon.scheduler.ng.BaseSource;

/**
 * Created by jmussler on 4/2/15.
 */
public abstract class AlertSource extends BaseSource<AlertDefinition> {
    public AlertSource(String name) {
        super(name);
    }
}
