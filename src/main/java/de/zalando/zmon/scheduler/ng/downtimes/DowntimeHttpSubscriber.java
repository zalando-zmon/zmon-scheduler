package de.zalando.zmon.scheduler.ng.downtimes;

import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
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
public class DowntimeHttpSubscriber implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(DowntimeHttpSubscriber.class);

    private final DowntimeService service;
    private final String url;
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    private final RestTemplate restTemplate;

    public DowntimeHttpSubscriber(DowntimeService service, SchedulerConfig config, RestTemplate restTemplate) {
        url = config.getDowntimeHttpUrl();
        this.service = service;
        this.restTemplate = restTemplate;


        LOG.info("Subscribing for downtimes: {}", url);
        if (url != null && !url.equals("")) {
            executor.scheduleAtFixedRate(this, 60, 5, TimeUnit.SECONDS);
        }
    }

    @Override
    public void run() {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<List<DowntimeForwardTask>> response = restTemplate.exchange(url, HttpMethod.GET, request, new ParameterizedTypeReference<List<DowntimeForwardTask>>() {
            });

            for (DowntimeForwardTask task : response.getBody()) {

                switch (task.getType()) {
                    case NEW:
                        LOG.info("Received downtime request: type={} groupdId={}", task.getType(), task.getRequest().getGroupId());
                        service.storeDowntime(task.getRequest());
                        break;
                    case DELETE:
                        LOG.info("Received downtime delete request: type={} ids={}", task.getType(), task.getIds());
                        service.deleteDowntimes(task.getIds());
                        break;
                    case DELETE_GROUP:
                        LOG.info("Received downtime group delete request: type={} (unexpected)", task.getType());
                        service.deleteDowntimeGroup(task.getGroupId());
                        break;
                }
            }

        } catch (Throwable ex) {
            LOG.error("msg={}", ex.getMessage());
        }
    }
}