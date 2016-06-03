package de.zalando.zmon.scheduler.ng.cleanup;

import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by jmussler on 03.06.16.
 */
@Configuration
public class CleanupConfiguration {

    private final static Logger LOG = LoggerFactory.getLogger(CleanupConfiguration.class);

    @Bean(name = "checkChangeCleaner")
    @Autowired
    public CheckChangeCleaner createCleaner(AlertRepository alertRepo, CheckRepository checkRepo, AlertChangeCleaner alertCleaner) {
        LOG.info("Registering checkChangeCleaner...");
        CheckChangeCleaner l = new CheckChangeCleaner(alertRepo, alertCleaner);
        checkRepo.registerListener(l);
        return l;
    }

    @Bean(name = "alertChangeCleaner")
    @Autowired
    public AlertChangeCleaner createCleaner(AlertRepository alertRepo, CheckRepository checkRepo, EntityRepository entityRepo, SchedulerConfig config) {
        LOG.info("Registering alertChangeCleaner...");
        AlertChangeCleaner l = new AlertChangeCleaner(alertRepo, checkRepo, entityRepo, config);
        alertRepo.registerChangeListener(l);
        return l;
    }
}
