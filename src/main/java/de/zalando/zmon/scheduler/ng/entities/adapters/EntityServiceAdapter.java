package de.zalando.zmon.scheduler.ng.entities.adapters;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by jmussler on 4/2/15.
 */
public class EntityServiceAdapter extends EntityAdapter {

    private URI url;

    private final Timer timer;
    private boolean isFirstLoad = true;

    private RestTemplate restTemplate;

    private static final Logger LOG = LoggerFactory.getLogger(EntityServiceAdapter.class);

    @Autowired
    public EntityServiceAdapter(URI url, MetricRegistry metrics, RestTemplate restTemplate) {
        super("EntityServiceAdapter");
        this.restTemplate = restTemplate;
        LOG.info("configuring entity service url={}", url);
        this.url = url;
        this.timer = metrics.timer("entity-adapter.entity-service");
    }

    private static class BaseEntity extends HashMap<String, Object> {
    }

    private static class BaseEntityList extends ArrayList<BaseEntity> {
    }

    @Override
    public Collection<Entity> getCollection() {
        HttpEntity<String> request;

        LOG.info("Querying entities with token...");
        request = new HttpEntity<>(new HttpHeaders());

        try {
            Timer.Context tC = timer.time();
            ResponseEntity<BaseEntityList> response = restTemplate.exchange(url, HttpMethod.GET, request, BaseEntityList.class);
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
