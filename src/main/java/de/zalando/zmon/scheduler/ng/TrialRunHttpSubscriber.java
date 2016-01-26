package de.zalando.zmon.scheduler.ng;

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
public class TrialRunHttpSubscriber implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(TrialRunHttpSubscriber.class);

    private final String url;
    private final Scheduler scheduler;
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    private final TokenWrapper tokenWrapper;

    public TrialRunHttpSubscriber(Scheduler scheduler, SchedulerConfig config, TokenWrapper tokenWrapper) {
        url = config.trial_run_http_url();
        this.tokenWrapper = tokenWrapper;

        LOG.info("Subscribing for trial runs: {}", url);
        this.scheduler = scheduler;
        if(url!=null && !url.equals("")) {
            executor.scheduleAtFixedRate(this, 60, 5, TimeUnit.SECONDS);
        }
    }

    @Override
    public void run() {
        try {
            RestTemplate rt = new RestTemplate();
            HttpEntity<String> request;

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + tokenWrapper.get());
            request = new HttpEntity<>(headers);

            ResponseEntity<List<TrialRunRequest>> response = rt.exchange(url, HttpMethod.GET, request, new ParameterizedTypeReference<List<TrialRunRequest>>() {
            });

            for (TrialRunRequest trialRunRequest : response.getBody()) {
                LOG.info("Received trial run request: {}", trialRunRequest);
                scheduler.scheduleTrialRun(trialRunRequest);
            }
        }
        catch(Throwable ex) {
            LOG.error("", ex);
        }
    }
}
