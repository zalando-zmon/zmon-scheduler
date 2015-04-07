package de.zalando.zmon.scheduler.ng.alerts;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Collection;
import java.util.List;

/**
 * Created by jmussler on 4/7/15.
 */
public class DefaultAlertSource implements AlertSource {

    private final MetricRegistry metrics;
    private final Timer timer;

    private final String url;
    private final String user;
    private final String password;
    private final String name;

    private final static Logger LOG = LoggerFactory.getLogger(DefaultAlertSource.class);

    private static final ObjectMapper mapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper m = new ObjectMapper();
        m.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        return m;
    }

    public DefaultAlertSource(String name, String url, String user, String password, MetricRegistry metrics) {
        this.metrics = metrics;
        this.user = user;
        this.url = url;
        this.password = password;
        this.name = name;
        this.timer = metrics.timer("alert-adapter."+name);
    }

    public DefaultAlertSource(String name, String url, MetricRegistry metrics) {
        this.metrics = metrics;
        this.user = null;
        this.url = url;
        this.password = null;
        this.name = name;
        this.timer = metrics.timer("alert-adapter."+name);
    }


    private HttpHeaders getWithAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString((user+":"+password).getBytes()));
        return headers;
    }

    @Override
    public Collection<AlertDefinition> getAlerts() {
        RestTemplate rt = new RestTemplate();

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);
        rt.getMessageConverters().clear();
        rt.getMessageConverters().add(converter);

        AlertDefinitions defs;
        if(null!=user && !"".equals(user)) {
            LOG.info("Querying alerts with credentials");
            HttpEntity<String> request = new HttpEntity<>(getWithAuth());
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

    @Override
    public String getName() {
        return name;
    }
}

