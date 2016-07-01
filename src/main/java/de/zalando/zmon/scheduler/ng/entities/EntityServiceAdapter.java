package de.zalando.zmon.scheduler.ng.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import de.zalando.zmon.scheduler.ng.TokenWrapper;

/**
 * Created by jmussler on 4/2/15.
 */
public class EntityServiceAdapter extends EntityAdapter {

    private String url;
    private TokenWrapper tokens;

    private final Timer timer;
    private boolean isFirstLoad = true;

    private ClientHttpRequestFactory clientFactory;

    private static final Logger LOG = LoggerFactory.getLogger(EntityServiceAdapter.class);

    @Autowired
    public EntityServiceAdapter(String url, MetricRegistry metrics, TokenWrapper tokens, ClientHttpRequestFactory clientFactory) {
        super("EntityServiceAdapter");
        this.clientFactory = clientFactory;
        LOG.info("configuring entity service url={}", url);
        this.url = url;
        this.tokens = tokens;
        this.timer = metrics.timer("entity-adapter.entity-service");
    }

    private static class BaseEntity extends HashMap<String, Object> {
    }

    private static class BaseEntityList extends ArrayList<BaseEntity> {
    }

    private HttpHeaders getWithAuth() {
        HttpHeaders headers = new HttpHeaders();

        if (tokens != null) {
            headers.add("Authorization", "Bearer " + tokens.get());
        }

        return headers;
    }

    @Override
    public Collection<Entity> getCollection() {
        RestTemplate rt = new RestTemplate(clientFactory);
        HttpEntity<String> request;

        final String accessToken = tokens.get();
        LOG.info("Querying entities with token " + accessToken.substring(0, Math.min(accessToken.length(), 3)) + "..");
        request = new HttpEntity<>(getWithAuth());

        try {
            Timer.Context tC = timer.time();
            ResponseEntity<BaseEntityList> response = rt.exchange(url, HttpMethod.GET, request, BaseEntityList.class);
            LOG.info("Entity Service Adapter used: {}ms", tC.stop() / 1000000);

            BaseEntityList list = response.getBody();
            List<Entity> entityList = new ArrayList<>(list.size());

            for (BaseEntity base : list) {
                Entity entity = new Entity((String) base.get("id"), this.getName());
                entity.addProperties(base);
                entityList.add(entity);
            }

            isFirstLoad = false;
            return entityList;
        } catch (Throwable t) {
            LOG.error("Failed to get entities: {}", t.getMessage());
            if (!isFirstLoad) {
                // rethrow, continue to used already loaded entities
                throw t;
            }
        }
        return new ArrayList<>(0);
    }
}
