package de.zalando.zmon.scheduler.ng.config;

import com.codahale.metrics.MetricRegistry;
import de.zalando.zmon.scheduler.ng.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by jmussler on 30.06.16.
 */
@Configuration
public class QueueSelectorConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(QueueSelectorConfiguration.class);

    public static QueueWriter createWriter(SchedulerConfig schedulerConfig, MetricRegistry metrics) {
        if(schedulerConfig.getTask_writer_type() == TaskWriterType.REDIS) {
            LOG.info("Creating queue writer: Redis host={} port={}", schedulerConfig.getRedis_host(), schedulerConfig.getRedis_port());
            return new JedisQueueWriter(schedulerConfig.getRedis_host(), schedulerConfig.getRedis_port(), metrics);
        }
        else if (schedulerConfig.getTask_writer_type() == TaskWriterType.ARRAY_LIST) {
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
    public QueueSelector getSelector(QueueWriter writer, SchedulerConfig config, MetricRegistry metrics) {
        return new QueueSelector(writer, config, metrics);
    }
}
