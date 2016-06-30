package de.zalando.zmon.scheduler.ng.instantevaluations;

import de.zalando.zmon.scheduler.ng.DataCenterSubscriber;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by jmussler on 5/22/15.
 */
@Component
public class InstantEvalForwarder extends DataCenterSubscriber<Integer> {

    @Autowired
    public InstantEvalForwarder(SchedulerConfig config) {
        super(config.isInstantEvalForward());
    }
}
