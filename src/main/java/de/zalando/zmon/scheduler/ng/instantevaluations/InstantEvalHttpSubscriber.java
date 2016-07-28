package de.zalando.zmon.scheduler.ng.instantevaluations;

import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by jmussler on 5/22/15.
 */
public class InstantEvalHttpSubscriber implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(InstantEvalHttpSubscriber.class);

    private final String url;
    private final Scheduler scheduler;
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    private final RestTemplate restTemplate;

    public InstantEvalHttpSubscriber(Scheduler scheduler, SchedulerConfig config, RestTemplate restTemplate) {
        url = config.getInstantEvalHttpUrl();
        this.restTemplate = restTemplate;

        LOG.info("Subscribing for instant evaluations: {}", url);
        this.scheduler = scheduler;
        if (url != null && !url.equals("")) {
            executor.scheduleAtFixedRate(this, 60, 5, TimeUnit.SECONDS);
        }
    }

    @Override
    public void run() {
        try {

            HttpEntity<String> request;

            HttpHeaders headers = new HttpHeaders();
            request = new HttpEntity<>(headers);

            ResponseEntity<List<Integer>> response = restTemplate.exchange(url, HttpMethod.GET, request, new ParameterizedTypeReference<List<Integer>>() {
            });

            for (Integer checkId : response.getBody()) {
                LOG.info("Received instant evaluation request: {}", checkId);
                scheduler.executeImmediate(checkId);
            }
        } catch (Throwable ex) {
            LOG.error("msg={}", ex.getMessage());
        }
    }
}