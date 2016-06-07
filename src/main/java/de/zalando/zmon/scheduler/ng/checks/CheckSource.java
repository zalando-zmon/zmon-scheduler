package de.zalando.zmon.scheduler.ng.checks;

import de.zalando.zmon.scheduler.ng.BaseSource;

/**
 * Created by jmussler on 3/31/15.
 */
public abstract class CheckSource extends BaseSource<CheckDefinition> {
    public CheckSource(String name) {
        super(name);
    }
}
