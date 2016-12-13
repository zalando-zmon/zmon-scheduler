package de.zalando.zmon.scheduler.ng.config;

import de.zalando.zmon.scheduler.ng.cleanup.*;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by jmussler on 03.06.16.
 */
@Configuration
public class CleanupConfiguration {

    private final static Logger LOG = LoggerFactory.getLogger(CleanupConfiguration.class);

    @Bean
    public CheckChangedCleaner checkChangedCleaner(AlertRepository alertRepo, CheckRepository checkRepo, AlertChangedCleaner alertCleaner) {
        LOG.info("Registering checkChangedCleaner...");
        CheckChangedCleaner l = new CheckChangedCleaner(alertRepo, alertCleaner);
        checkRepo.registerListener(l);
        return l;
    }

    @Bean
    public AlertChangedCleaner alertChangedCleaner(AlertRepository alertRepo, CheckRepository checkRepo, EntityRepository entityRepo, SchedulerConfig config) {
        LOG.info("Registering alertChangedCleaner...");
        AlertChangedCleaner l = new AlertChangedCleaner(alertRepo, checkRepo, entityRepo, config);
        alertRepo.registerChangeListener(l);
        return l;
    }

    @Bean
    public EntityChangedCleaner entityChangedCleaner(EntityRepository entityRepo, AlertRepository alertRepo, CheckRepository checkRepo, AlertChangedCleaner alertCleaner) {
        LOG.info("Registering entityChangedCleaner...");
        EntityChangedCleaner l = new EntityChangedCleaner(alertRepo, checkRepo, alertCleaner);
        entityRepo.registerListener(l);
        return l;
    }

    @Bean
    public MetricsCleanup metricsCleanup(SchedulerConfig config) {
        LOG.info("Registering metricsCleaner...");
        MetricsCleanup cleaner = new MetricsCleanup(config);
        return cleaner;
    }

    @Bean
    public DowntimeCleanup downtimeCleanup(SchedulerConfig config){
        LOG.info("Registering downtimeCleanup...");
        DowntimeCleanup cleaner = new DowntimeCleanup(config);
        return cleaner;
    }
}
