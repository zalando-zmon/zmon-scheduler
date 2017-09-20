package de.zalando.zmon.scheduler.ng.queue;

import de.zalando.zmon.scheduler.ng.Alert;
import de.zalando.zmon.scheduler.ng.Check;
import de.zalando.zmon.scheduler.ng.CommandSerializer;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.entities.Entity;

import io.opentracing.Tracer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by jmussler on 30.06.16.
 */
public class QueueSelector {

    private final QueueWriter writer;
    private final SchedulerConfig config;
    private final List<Selector> selectors = new ArrayList<>();
    private final CommandSerializer serializer;
    private final PropertyQueueSelector propertySelector;

    public QueueSelector(QueueWriter writer, SchedulerConfig config, Tracer tracer) {
        this.writer = writer;
        this.config = config;
        this.propertySelector = new PropertyQueueSelector(config);
        
        selectors.add(new RepoSelector(config));
        selectors.add(new HardCodedSelector(config));
        selectors.add(propertySelector);

        serializer = new CommandSerializer(config.getTaskSerializer(), tracer);
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
