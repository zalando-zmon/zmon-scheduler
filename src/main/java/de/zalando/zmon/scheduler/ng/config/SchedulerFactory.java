package de.zalando.zmon.scheduler.ng.config;

/**
 * Created by jmussler on 30.06.16.
 */

import com.codahale.metrics.MetricRegistry;
import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.TokenWrapper;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckChangedListener;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.downtimes.DowntimeForwarder;
import de.zalando.zmon.scheduler.ng.downtimes.DowntimeService;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import de.zalando.zmon.scheduler.ng.instantevaluations.InstantEvalForwarder;
import de.zalando.zmon.scheduler.ng.instantevaluations.InstantEvalHttpSubscriber;
import de.zalando.zmon.scheduler.ng.queue.QueueSelector;
import de.zalando.zmon.scheduler.ng.scheduler.Scheduler;
import de.zalando.zmon.scheduler.ng.trailruns.TrialRunForwarder;
import de.zalando.zmon.scheduler.ng.trailruns.TrialRunHttpSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SchedulerFactory {

    private final static Logger LOG = LoggerFactory.getLogger(SchedulerFactory.class);

    @Bean
    public CheckChangedListener getCheckChangeListener(CheckRepository checkRepo, Scheduler scheduler){
        CheckChangedListener listener = new CheckChangedListener(scheduler);
        checkRepo.registerListener(listener);
        return listener;
    }

    @Bean
    public Scheduler getScheduler(AlertRepository alertRepo,
                                  CheckRepository checkRepo,
                                  EntityRepository entityRepo,
                                  QueueSelector queueSelector,
                                  InstantEvalForwarder instantForwarder,
                                  TrialRunForwarder trialRunForwarder,
                                  DowntimeForwarder downtimeForwarder,
                                  DowntimeService downtimeService,
                                  TokenWrapper tokenWrapper,
                                  RestTemplate restTemplate,
                                  SchedulerConfig config,
                                  MetricRegistry metrics) {

        LOG.info("Creating scheduler instance");
        Scheduler newScheduler = new Scheduler(alertRepo, checkRepo, entityRepo, queueSelector, config, metrics);

        LOG.info("Check ID filter: {}", config.getCheckFilter());

        LOG.info("Initial scheduling of all checks");
        for (CheckDefinition cd : checkRepo.get()) {
            newScheduler.scheduleCheck(cd.getId());
        }
        SchedulerFactory.LOG.info("Initial scheduling of all checks done");

        if (config.isInstantEvalForward()) {
            entityRepo.registerListener(instantForwarder);
        }

        if (config.isTrialRunForward()) {
            entityRepo.registerListener(trialRunForwarder);
        }

        if (config.isDowntimeForward()) {
            entityRepo.registerListener(downtimeForwarder);
        }

        if (config.getTrialRunHttpUrl() != null) {
            TrialRunHttpSubscriber trialRunPoller = new TrialRunHttpSubscriber(newScheduler, config, tokenWrapper, restTemplate);
        }

        if (config.getInstantEvalHttpUrl() != null) {
            InstantEvalHttpSubscriber instantEvalPoller = new InstantEvalHttpSubscriber(newScheduler, config, tokenWrapper, restTemplate);
        }

        return newScheduler;
    }
}
