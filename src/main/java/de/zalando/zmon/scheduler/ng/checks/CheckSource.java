package de.zalando.zmon.scheduler.ng.checks;

import de.zalando.zmon.scheduler.ng.Source;

import java.util.List;

/**
 * Created by jmussler on 3/31/15.
 */
public interface CheckSource extends Source {
    List<CheckDefinition> getChecks();
}
