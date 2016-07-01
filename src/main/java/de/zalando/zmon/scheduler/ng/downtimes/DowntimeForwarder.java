package de.zalando.zmon.scheduler.ng.downtimes;

import de.zalando.zmon.scheduler.ng.DataCenterSubscriber;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by jmussler on 18.06.16.
 */
@Component
public class DowntimeForwarder extends DataCenterSubscriber<DowntimeForwardTask> {

    @Autowired
    public DowntimeForwarder(SchedulerConfig config) {
        super(config.isDowntimeForward());
    }
}
