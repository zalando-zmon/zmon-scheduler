package de.zalando.zmon.scheduler.ng.queue;

import de.zalando.zmon.scheduler.ng.Alert;
import de.zalando.zmon.scheduler.ng.Check;
import de.zalando.zmon.scheduler.ng.CommandSerializer;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.trailruns.TrialRunRequest;
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

    public QueueSelector(QueueWriter writer, SchedulerConfig config, Tracer tracer) {
        this.writer = writer;
        this.config = config;
        serializer = new CommandSerializer(config.getTaskSerializer(), tracer);

        selectors.add(new RepoSelector(config));
        selectors.add(new HardCodedSelector(config));
        selectors.add(new PropertyQueueSelector(config));
        selectors.add(new GenericSelector(config));
    }

    private String getQueueOrDefault(Entity entity, Check check, Collection<Alert> alerts, TrialRunRequest request, String defaultQueue) {
        String queue;

        for (Selector s : selectors) {
            queue = s.getQueue(entity, check, alerts, request);
            if (queue != null) {
                return queue;
            }
        }

        return defaultQueue;
    }

    public void executeTrialRun(Entity entity, TrialRunRequest request) {
        byte[] command = serializer.writeTrialRun(entity, request);
        String queue = getQueueOrDefault(entity, null, null, request, config.getTrialRunQueue());

        writer.exec(queue, command);
    }

    public void execute(Entity entity, Check check, Collection<Alert> alerts, long scheduledTime) {
        byte[] command = serializer.write(entity, check, alerts, scheduledTime);
        String queue = getQueueOrDefault(entity, check, alerts, null, config.getDefaultQueue());

        writer.exec(queue, command);
    }
}
