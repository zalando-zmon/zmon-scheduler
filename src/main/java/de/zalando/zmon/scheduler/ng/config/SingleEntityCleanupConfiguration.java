package de.zalando.zmon.scheduler.ng.config;

import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.cleanup.SingleEntityCleanup;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by jmussler on 30.06.16.
 */
@Configuration
public class SingleEntityCleanupConfiguration {
    private final static Logger LOG = LoggerFactory.getLogger(SingleEntityCleanupConfiguration.class);

    @Bean
    SingleEntityCleanup getSingleEntityCleanup(SchedulerConfig config, AlertRepository alertRepo, CheckRepository checkRepo, EntityRepository entityRepository) {
        SingleEntityCleanup cleanup = new SingleEntityCleanup(config, alertRepo, checkRepo, entityRepository);
        LOG.info("Registering SingleEntityCleanUp job");
        entityRepository.registerListener(cleanup);
        return cleanup;
    }
}
