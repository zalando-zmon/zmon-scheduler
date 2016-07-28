package de.zalando.zmon.scheduler.ng.alerts;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;

/**
 * Created by jmussler on 4/7/15.
 */
public class DefaultAlertSource extends AlertSource {

    private final Timer timer;

    private final String url;
    private final RestTemplate restTemplate;
    private boolean isFirstLoad = true;

    private Collection<AlertDefinition> lastResults = null;
    private long lastResultMaxLastModified = 0;

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAlertSource.class);

    @Autowired
    public DefaultAlertSource(final String name, final String url, final MetricRegistry metrics, final RestTemplate restTemplate) {
        super(name);
        this.restTemplate = restTemplate;
        LOG.info("configuring alert source url={}", url);
        this.url = url;
        this.timer = metrics.timer("alert-adapter." + name);
    }

    @Override
    public Collection<AlertDefinition> getCollection() {
        AlertDefinitions defs = new AlertDefinitions();
        try {
            LOG.info("Querying alert definitions with token... ");
            final HttpEntity<String> request = new HttpEntity<>(new HttpHeaders());

            HttpHeaders headers = restTemplate.headForHeaders(url);
            if (headers.containsKey("Last-Modified")) {
                if (!doRefresh(headers.get("Last-Modified").get(0), lastResultMaxLastModified, lastResults)) {
                    LOG.info("Skipping alert update ...{}", headers.get("Last-Modified"));
                    return lastResults;
                }
            }

            ResponseEntity<AlertDefinitions> response;
            Timer.Context ct = timer.time();
            response = restTemplate.exchange(url, HttpMethod.GET, request, AlertDefinitions.class);
            ct.stop();
            defs = response.getBody();

            LOG.info("Got {} alerts from {}", defs.getAlertDefinitions().size(), getName());
            isFirstLoad = false;
        } catch (Throwable t) {
            LOG.error("Failed to get alert definitions: {}", t.getMessage());
            if (!isFirstLoad) {
                // rethrow so that currently alerts are still used not not replaced by empty list
                throw t;
            }
        }

        lastResults = defs.getAlertDefinitions();
        lastResultMaxLastModified = lastResults.stream().map(AlertDefinition::getLastModified).reduce(0L, Math::max);
        return defs.getAlertDefinitions();
    }
}
