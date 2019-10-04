package de.zalando.zmon.scheduler.ng.checks;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Component
public class MinIntervalEntityFetcher {

    private final static Logger LOG = LoggerFactory.getLogger(MinIntervalEntityFetcher.class);

    private final URI entityServiceUrl;
    private final RestTemplate restTemplate;
    private final Timer timer;
    private final Meter totalFetches;
    private final Meter totalErrors;
    private MinCheckIntervalData checkInterval;

    @Autowired
    public MinIntervalEntityFetcher(SchedulerConfig config, RestTemplate restTemplate, MetricRegistry metrics) {
        this.restTemplate = restTemplate;
        this.timer = metrics.timer("interval-fetcher.timer");
        this.totalFetches = metrics.meter("interval-fetcher.total-fetches");
        this.totalErrors = metrics.meter("interval-fetcher.total-errors");
        this.entityServiceUrl = URI.create(config.getEntityServiceUrl() + "/api/v1/entities/zmon-min-check-interval");

        this.checkInterval = new MinCheckIntervalData();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MinCheckIntervalData {
        private List<Integer> whitelistedChecks = Collections.emptyList();
        private Long minCheckInterval = 15L;
        private Long minWhitelistedCheckInterval = 15L;

        public List<Integer> getWhitelistedChecks() {
            return whitelistedChecks;
        }

        public void setWhitelistedChecks(List<Integer> whitelistedChecks) {
            this.whitelistedChecks = whitelistedChecks;
        }

        public Long getMinCheckInterval() {
            return minCheckInterval;
        }

        public void setMinCheckInterval(Long minCheckInterval) {
            this.minCheckInterval = minCheckInterval;
        }

        public Long getMinWhitelistedCheckInterval() {
            return minWhitelistedCheckInterval;
        }

        public void setMinWhitelistedCheckInterval(Long minWhitelistedCheckInterval) {
            this.minWhitelistedCheckInterval = minWhitelistedCheckInterval;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MinCheckInterval {
        private MinCheckIntervalData data;

        public MinCheckIntervalData getData() {
            return data;
        }

        public void setData(MinCheckIntervalData data) {
            this.data = data;
        }
    }

    public MinCheckIntervalData getCheckInterval() {
        return checkInterval;
    }

    public void fetch() {
        LOG.info("Fetching 'zmon-min-check-interval' entity");
        HttpEntity<String> request = new HttpEntity<>(new HttpHeaders());

        try {
            Timer.Context t = timer.time();
            ResponseEntity<MinCheckInterval> response = restTemplate.exchange(entityServiceUrl, HttpMethod.GET, request, MinCheckInterval.class);
            LOG.info("MinInterval Entity Fetcher used: {}ms", t.stop() / 1_000_000);

            this.checkInterval = response.getBody().getData();

            totalFetches.mark();
        } catch (Throwable e) {
            LOG.error("MinInterval Entity Fetcher could not fetch, falling back to default/old", e);
            totalErrors.mark();
        }
    }
}
