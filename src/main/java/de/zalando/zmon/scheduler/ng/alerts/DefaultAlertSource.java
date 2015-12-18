package de.zalando.zmon.scheduler.ng.alerts;

import java.util.Base64;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import org.springframework.web.client.RestTemplate;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

/**
 * Created by jmussler on 4/7/15.
 */
public class DefaultAlertSource extends AlertSource {

    private final MetricRegistry metrics;
    private final Timer timer;

    private final String url;
    private final String user;
    private final String password;
    private final String token;

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAlertSource.class);

    private static final ObjectMapper mapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper m = new ObjectMapper();
        m.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        return m;
    }

    public DefaultAlertSource(final String name, final String url, final String user, final String password,
            final String token, final MetricRegistry metrics) {
        super(name);
        this.metrics = metrics;
        this.user = user;
        this.url = url;
        this.password = password;
        this.token = token;
        this.timer = metrics.timer("alert-adapter." + name);
    }

    public DefaultAlertSource(final String name, final String url, final MetricRegistry metrics) {
        super(name);
        this.metrics = metrics;
        this.user = null;
        this.url = url;
        this.password = null;
        this.token = null;
        this.timer = metrics.timer("alert-adapter." + name);
    }

    private HttpHeaders getWithAuth() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes()));
        return headers;
    }

    private HttpHeaders getWithOAuth2() {
        final HttpHeaders headers = new HttpHeaders();
        final String bearerToken = token;
        headers.add("Authorization", "Bearer " + bearerToken);
        return headers;
    }

    private HttpHeaders getAuthenticationHeader() {
        if (null != token && !"".equals(token)) {
            return getWithOAuth2();
        }

        return getWithAuth();
    }

    @Override
    public Collection<AlertDefinition> getCollection() {
        RestTemplate rt = new RestTemplate();

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);
        rt.getMessageConverters().clear();
        rt.getMessageConverters().add(converter);

        AlertDefinitions defs;
        if ((null != user && !"".equals(user)) || (null != token && !"".equals(token))) {
            LOG.info("Querying alerts with credentials {}", user);

            final HttpEntity<String> request = new HttpEntity<>(getAuthenticationHeader());
            ResponseEntity<AlertDefinitions> response;
            Timer.Context ct = timer.time();
            response = rt.exchange(url, HttpMethod.GET, request, AlertDefinitions.class);
            ct.stop();
            defs = response.getBody();
        } else {
            LOG.info("Querying without credentials");

            Timer.Context ct = timer.time();
            defs = rt.getForObject(url, AlertDefinitions.class);
            ct.stop();
        }

        LOG.info("Got {} alerts from {}", defs.getAlertDefinitions().size(), getName());

        return defs.getAlertDefinitions();
    }
}
