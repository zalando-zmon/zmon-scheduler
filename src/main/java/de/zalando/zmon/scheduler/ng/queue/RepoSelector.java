package de.zalando.zmon.scheduler.ng.queue;

import de.zalando.zmon.scheduler.ng.Alert;
import de.zalando.zmon.scheduler.ng.Check;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.entities.Entity;

import java.util.Collection;
import java.util.Map;

/**
 * Created by jmussler on 30.06.16.
 */
public class RepoSelector implements Selector {

    private final SchedulerConfig config;

    public RepoSelector(SchedulerConfig config) {
        this.config = config;
    }

    @Override
    public String getQueue(Entity entity, Check check, Collection<Alert> alerts) {
        if (null == check) {
            return null;
        }

        for(Map.Entry<String, String> entry : config.getQueueMappingByUrl().entrySet()) {
            CheckDefinition checkDefinition = check.getCheckDefinition();
            if (null != checkDefinition) {
                if (null != checkDefinition.getSourceUrl() && checkDefinition.getSourceUrl().startsWith(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }
}
