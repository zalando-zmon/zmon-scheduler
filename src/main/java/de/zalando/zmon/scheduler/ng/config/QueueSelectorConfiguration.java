package de.zalando.zmon.scheduler.ng.config;

import de.zalando.zmon.scheduler.ng.TaskWriterType;
import de.zalando.zmon.scheduler.ng.queue.ArrayQueueWriter;
import de.zalando.zmon.scheduler.ng.queue.JedisQueueWriter;
import de.zalando.zmon.scheduler.ng.queue.LogQueueWriter;
import de.zalando.zmon.scheduler.ng.queue.QueueSelector;
import de.zalando.zmon.scheduler.ng.queue.QueueWriter;

import com.codahale.metrics.MetricRegistry;
import io.opentracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by jmussler on 30.06.16.
 */
@Configuration
public class QueueSelectorConfiguration {

    @Autowired
    private Tracer tracer;

    private static final Logger LOG = LoggerFactory.getLogger(QueueSelectorConfiguration.class);

    public static QueueWriter createWriter(SchedulerConfig schedulerConfig, MetricRegistry metrics) {
        if(schedulerConfig.getTaskWriterType() == TaskWriterType.REDIS) {
            LOG.info("Creating queue writer: Redis host={} port={}", schedulerConfig.getRedisHost(), schedulerConfig.getRedisPort());
            return new JedisQueueWriter(schedulerConfig.getRedisHost(), schedulerConfig.getRedisPort(), metrics);
        }
        else if (schedulerConfig.getTaskWriterType() == TaskWriterType.ARRAY_LIST) {
            LOG.info("creating queue writer: ArrayQueueWriter");
            return new ArrayQueueWriter(metrics);
        }
        else {
            LOG.info("Creating queue writer: LOG writer");
            return new LogQueueWriter(metrics);
        }
    }

    @Bean
    public QueueWriter getWriter(SchedulerConfig config, MetricRegistry metrics) {
        return createWriter(config, metrics);
    }

    @Bean
    public QueueSelector getSelector(QueueWriter writer, SchedulerConfig config) {
        return new QueueSelector(writer, config, tracer);
    }
}
