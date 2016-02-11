package de.zalando.zmon.scheduler.ng.checks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import de.zalando.zmon.scheduler.ng.TokenWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Created by jmussler on 4/2/15.
 */
public class DefaultCheckSource extends CheckSource {

    private final static Logger LOG = LoggerFactory.getLogger(DefaultCheckSource.class);

    private String url;
    private TokenWrapper tokens;
    private ClientHttpRequestFactory clientFactory;

    private static final ObjectMapper mapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper m = new ObjectMapper();
        m.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        return m;
    }

    @Autowired
    public DefaultCheckSource(String name, String url, final TokenWrapper tokens, final ClientHttpRequestFactory clientFactory) {
        super(name);
        this.clientFactory = clientFactory;
        this.url = url;
        this.tokens = tokens;
    }

    private HttpHeaders getWithAuth() {
        HttpHeaders headers = new HttpHeaders();
        if (tokens != null) {
         headers.add("Authorization", "Bearer " + tokens.get());
        }
        return headers;
    }

    @Override
    public Collection<CheckDefinition> getCollection() {

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);

        RestTemplate rt = new RestTemplate(clientFactory);
        rt.getMessageConverters().clear();
        rt.getMessageConverters().add(converter);

        CheckDefinitions defs;
        if(tokens!=null) {
            final String accessToken = tokens.get();
            LOG.info("Querying check definitions with token " + accessToken.substring(0, Math.min(accessToken.length(), 3)) + "..");
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
