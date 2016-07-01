package de.zalando.zmon.scheduler.ng.queue;

import com.codahale.metrics.MetricRegistry;

/**
 * Created by jmussler on 30.06.16.
 */
public abstract class QueueWriter {
    private final QueueMetrics metrics;

    public QueueWriter(MetricRegistry metrics) {
        this.metrics = new QueueMetrics(metrics);
    }

    public void exec(String queue, byte[] command) {
        write(queue, command);
        metrics.mark(queue);
        metrics.incThroughput(command.length);

    }

    protected abstract void write(String queue, byte[] command);
}
