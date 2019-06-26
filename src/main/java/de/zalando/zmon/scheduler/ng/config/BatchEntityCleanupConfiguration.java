package de.zalando.zmon.scheduler.ng.config;

import de.zalando.zmon.scheduler.ng.cleanup.BatchEntityCleanup;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import de.zalando.zmon.scheduler.ng.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class BatchEntityCleanupConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(BatchEntityCleanupConfiguration.class);

    @Bean
    BatchEntityCleanup getBatchEntityCleanup(Scheduler scheduler, EntityRepository entityRepository) {
        LOG.info("Registering BatchEntityCleanup job");
        BatchEntityCleanup cleanup = new BatchEntityCleanup(scheduler);
        entityRepository.registerListener(cleanup);
        return cleanup;
    }
}
