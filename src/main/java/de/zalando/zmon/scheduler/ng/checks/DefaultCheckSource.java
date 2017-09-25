package de.zalando.zmon.scheduler.ng.checks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;

/**
 * Created by jmussler on 4/2/15.
 */
public class DefaultCheckSource extends CheckSource {

    private final static Logger LOG = LoggerFactory.getLogger(DefaultCheckSource.class);

    private String url;
    private boolean isFirstLoad = true;
    private final RestTemplate restTemplate;
    private Collection<CheckDefinition> lastResults = null;
    private long lastResultMaxLastModified = 0;

    @Autowired
    public DefaultCheckSource(String name, String url, final RestTemplate restTemplate) {
        super(name);
        this.restTemplate = restTemplate;
        LOG.info("configuring check source url={}", url);

        this.url = url;
    }

    @Override
    public Collection<CheckDefinition> getCollection() {
        CheckDefinitions defs = new CheckDefinitions();
        try {
            LOG.info("Querying check definitions with token...");
            HttpEntity<String> request = new HttpEntity<>(new HttpHeaders());

            HttpHeaders headers = restTemplate.headForHeaders(url);
            if (headers.containsKey("Last-Modified")) {
                if (!doRefresh(headers.get("Last-Modified").get(0), lastResultMaxLastModified, lastResults)) {
                    LOG.info("Skipping check update ...{}", headers.get("Last-Modified"));
                    return lastResults;
                }
            }

            ResponseEntity<CheckDefinitions> response;
            response = restTemplate.exchange(url, HttpMethod.GET, request, CheckDefinitions.class);
            defs = response.getBody();
            LOG.info("Got {} checks from {}", defs.getCheckDefinitions().size(), getName());
            isFirstLoad = false;
        } catch (Throwable t) {
            LOG.error("Failed to get check definitions: {}", t.getMessage());
            if (!isFirstLoad) {
                // rethrow so that currently used checks are still used and not replaced by empty list
                throw t;
            }
        }

        lastResults = defs.getCheckDefinitions();
        lastResultMaxLastModified = lastResults.stream().map(CheckDefinition::getLastModified).reduce(0L, Math::max);
        return defs.getCheckDefinitions();
    }
}
