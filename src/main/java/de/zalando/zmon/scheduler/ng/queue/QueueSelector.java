package de.zalando.zmon.scheduler.ng.queue;

import com.codahale.metrics.MetricRegistry;
import de.zalando.zmon.scheduler.ng.*;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.entities.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by jmussler on 30.06.16.
 */
public class QueueSelector {

    private final QueueWriter writer;
    private final SchedulerConfig config;
    private final MetricRegistry metrics;
    private final List<Selector> selectors = new ArrayList<>();
    private final JavaCommandSerializer serializer;
    private final PropertyQueueSelector propertySelector;

    public QueueSelector(QueueWriter writer, SchedulerConfig config, MetricRegistry metrics) {
        this.writer = writer;
        this.config = config;
        this.metrics = metrics;
        this.propertySelector = new PropertyQueueSelector(config);

        selectors.add(new RepoSelector(config));
        selectors.add(new HardCodedSelector(config));
        selectors.add(propertySelector);

        serializer = new JavaCommandSerializer(config.getTaskSerializer());
    }

    public void execute(Entity entity, byte[] command) {
        execute(entity, command, null);
    }

    public void execute(Entity entity, byte[] command, String targetQueue) {
        String queue = targetQueue;
        if (null == queue) {
            queue = propertySelector.getQueue(entity, null, null);
        }

        if (null == queue) {
            queue =  config.getDefaultQueue();
        }

        writer.exec(queue, command);
    }

    public void execute(Entity entity, Check check, Collection<Alert> alerts, long scheduledTime) {
        byte[] command = serializer.write(entity, check, alerts, scheduledTime);

        String queue = null;

        for(Selector s : selectors) {
            if (null == queue) {
                queue = s.getQueue(entity, check, alerts);
            }
        }

        if (null == queue) {
            queue = config.getDefaultQueue();
        }

        writer.exec(queue, command);
    }
}
