package de.zalando.zmon.scheduler.ng.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Created by jmussler on 4/2/15.
 */
public class DefaultCheckSource extends CheckSource {

    private final static Logger LOG = LoggerFactory.getLogger(DefaultCheckSource.class);

    private String url;
    private String user;
    private String password;

    public DefaultCheckSource(String name, String url) {
        super(name);
        this.url = url;
    }

    private static final ObjectMapper mapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper m = new ObjectMapper();
        m.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        return m;
    }

    public DefaultCheckSource(String name, String url, String user, String password) {
        super(name);
        this.url = url;
        this.user = user;
        this.password = password;
    }

    private HttpHeaders getWithAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString((user+":"+password).getBytes()));
        return headers;
    }

    @Override
    public Collection<CheckDefinition> getCollection() {

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);
        RestTemplate rt = new RestTemplate();
        rt.getMessageConverters().clear();
        rt.getMessageConverters().add(converter);

        CheckDefinitions defs;
        if(null!=user && !"".equals(user)) {
            LOG.info("Querying checks with credentials {}", user);
            HttpEntity<String> request = new HttpEntity<>(getWithAuth());
            ResponseEntity<CheckDefinitions> response;
            response = rt.exchange(url, HttpMethod.GET, request, CheckDefinitions.class);
            defs = response.getBody();
        } else {
            LOG.info("Querying without credentials");
            defs = rt.getForObject(url, CheckDefinitions.class);
        }
        LOG.info("Got {} checks from {}", defs.getCheckDefinitions().size(), getName());

        return defs.getCheckDefinitions();
    }
}
