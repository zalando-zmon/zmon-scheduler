package de.zalando.zmon.scheduler.ng;

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
    private final String url;
    private final Scheduler scheduler;
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    public TrialRunHttpSubscriber(Scheduler scheduler, SchedulerConfig config) {
        url = config.instant_eval_http_url();
        this.scheduler = scheduler;
        if(url!=null && !url.equals("")) {
            executor.scheduleAtFixedRate(this, 60, 60, TimeUnit.SECONDS);
        }
    }

    @Override
    public void run() {
        RestTemplate rt = new RestTemplate();
        HttpEntity<String> request;
        request = new HttpEntity<>(new HttpHeaders());

        ResponseEntity<List<TrialRunRequest>> response = rt.exchange(url, HttpMethod.GET, request, new ParameterizedTypeReference<List<TrialRunRequest>>() {});

        for( TrialRunRequest trialRunRequest: response.getBody()) {
            scheduler.scheduleTrialRun(trialRunRequest);
        }
    }
}
