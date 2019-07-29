package de.zalando.zmon.scheduler.ng.queue;

import de.zalando.zmon.scheduler.ng.Alert;
import de.zalando.zmon.scheduler.ng.Check;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.trailruns.TrialRunRequest;


import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jmussler on 30.06.16.
 */
public class HardCodedSelector implements Selector {

    private final Map<Integer, String> queueMapping = new HashMap<>();

    public HardCodedSelector(SchedulerConfig config) {
        for(Map.Entry<String, List<Integer>> entry : config.getQueueMapping().entrySet()) {
            for(Integer id : entry.getValue()) {
                queueMapping.put(id, entry.getKey());
            }
        }
    }

    @Override
    public String getQueue(Entity entity, Check check, Collection<Alert> alerts, TrialRunRequest request) {
        if (null == check) {
            return null;
        }

        return queueMapping.getOrDefault(check.getId(), null);
    }
}
