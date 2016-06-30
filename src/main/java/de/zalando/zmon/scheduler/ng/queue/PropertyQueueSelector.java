package de.zalando.zmon.scheduler.ng.queue;

import de.zalando.zmon.scheduler.ng.Alert;
import de.zalando.zmon.scheduler.ng.AlertOverlapGenerator;
import de.zalando.zmon.scheduler.ng.Check;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.entities.Entity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by jmussler on 30.06.16.
 */
public class PropertyQueueSelector implements Selector {

    private final SchedulerConfig config;

    public PropertyQueueSelector(SchedulerConfig config) {
        this.config = config;
    }

    @Override
    public String getQueue(Entity entity, Check check, Collection<Alert> alerts) {
        if (null == check) {
            return null;
        }

        for(Map.Entry<String, List<Map<String, String>>> entry : config.getQueuePropertyMapping().entrySet()) {
            for(Map<String, String> ps : entry.getValue()) {
                if(AlertOverlapGenerator.filter(ps, entity.getFilterProperties())) {
                    return entry.getKey();
                }
            }
        }

        return null;
    }
}
