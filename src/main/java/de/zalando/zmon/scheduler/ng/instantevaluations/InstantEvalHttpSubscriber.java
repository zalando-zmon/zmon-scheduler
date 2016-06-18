package de.zalando.zmon.scheduler.ng.instantevaluations;

import de.zalando.zmon.scheduler.ng.Scheduler;
import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.TokenWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
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
    private final TokenWrapper tokenWrapper;
    private final ClientHttpRequestFactory clientFactory;

    public InstantEvalHttpSubscriber(Scheduler scheduler, SchedulerConfig config, TokenWrapper tokenWrapper, ClientHttpRequestFactory clientFactory) {
        url = config.instant_eval_http_url();
        this.tokenWrapper = tokenWrapper;
        this.clientFactory = clientFactory;

        LOG.info("Subscribing for instant evaluations: {}", url);
        this.scheduler = scheduler;
        if (url != null && !url.equals("")) {
            executor.scheduleAtFixedRate(this, 60, 5, TimeUnit.SECONDS);
        }
    }

    @Override
    public void run() {
        try {
            RestTemplate rt = new RestTemplate(clientFactory);
            HttpEntity<String> request;

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + tokenWrapper.get());
            request = new HttpEntity<>(headers);

            ResponseEntity<List<Integer>> response = rt.exchange(url, HttpMethod.GET, request, new ParameterizedTypeReference<List<Integer>>() {
            });

            for (Integer checkId : response.getBody()) {
                LOG.info("Received instant evaluation request: {}", checkId);
                scheduler.executeImmediate(checkId);
            }
        } catch (Throwable ex) {
            LOG.error("", ex);
        }
    }
}