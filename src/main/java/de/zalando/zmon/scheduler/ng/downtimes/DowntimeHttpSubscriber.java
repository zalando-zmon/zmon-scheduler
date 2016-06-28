package de.zalando.zmon.scheduler.ng.downtimes;

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
public class DowntimeHttpSubscriber implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(DowntimeHttpSubscriber.class);

    private final DowntimeService service;
    private final String url;
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    private final TokenWrapper tokenWrapper;
    private final ClientHttpRequestFactory clientFactory;

    public DowntimeHttpSubscriber(DowntimeService service, SchedulerConfig config, TokenWrapper tokenWrapper, ClientHttpRequestFactory clientFactory) {
        url = config.getDowntime_http_url();
        this.service = service;
        this.tokenWrapper = tokenWrapper;
        this.clientFactory = clientFactory;

        LOG.info("Subscribing for downtimes: {}", url);
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

            ResponseEntity<List<DowntimeForwardTask>> response = rt.exchange(url, HttpMethod.GET, request, new ParameterizedTypeReference<List<DowntimeForwardTask>>() {
            });

            for (DowntimeForwardTask task : response.getBody()) {
                LOG.info("Received downtime request: type={}", task.getType());
                switch(task.getType()) {
                    case NEW:
                        service.storeDowntime(task.getRequest());
                        break;
                    case DELETE:
                        service.deleteDowntimes(task.getIds());
                        break;
                    case DELETE_GROUP:
                        service.deleteDowntimeGroup(task.getGroupId());
                        break;
                }
            }

        } catch (Throwable ex) {
            LOG.error("", ex);
        }
    }
}