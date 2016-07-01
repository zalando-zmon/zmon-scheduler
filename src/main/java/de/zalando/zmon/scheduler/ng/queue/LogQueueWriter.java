package de.zalando.zmon.scheduler.ng.queue;

import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jmussler on 30.06.16.
 */
public class LogQueueWriter extends QueueWriter {

    private final Logger logger;

    public LogQueueWriter(MetricRegistry metrics) {
        super(metrics);
        logger = LoggerFactory.getLogger(LogQueueWriter.class);
    }

    @Override
    public void write(String queue, byte[] command) {
        logger.info("q={} command={}", queue, command);
    }
}
