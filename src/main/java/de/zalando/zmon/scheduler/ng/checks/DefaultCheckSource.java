package de.zalando.zmon.scheduler.ng.checks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;

/**
 * Created by jmussler on 4/2/15.
 */
public class DefaultCheckSource implements CheckSource {

    private final static Logger LOG = LoggerFactory.getLogger(DefaultCheckSource.class);

    private String name;
    private String url;
    private String user;
    private String password;

    public DefaultCheckSource(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public DefaultCheckSource(String name, String url, String user, String password) {
        this.name = name;
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
    public String getName() {
        return name;
    }

    @Override
    public List<CheckDefinition> getCheckData() {
        RestTemplate rt = new RestTemplate();

        CheckDefinitions defs;
        if(null!=user && !"".equals(user)) {
            LOG.info("Querying checks with credentials");
            HttpEntity<String> request = new HttpEntity<>(getWithAuth());
            ResponseEntity<CheckDefinitions> response;
            response = rt.exchange(url, HttpMethod.GET, request, CheckDefinitions.class);
            defs = response.getBody();
        } else {
            LOG.info("Querying without credentials");
            defs = rt.getForObject(url, CheckDefinitions.class);
        }

        return defs.getCheckDefinitions();
    }
}
