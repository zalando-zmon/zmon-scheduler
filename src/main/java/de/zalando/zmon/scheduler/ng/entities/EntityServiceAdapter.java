package de.zalando.zmon.scheduler.ng.entities;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import de.zalando.zmon.scheduler.ng.TokenWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Created by jmussler on 4/2/15.
 */
public class EntityServiceAdapter extends EntityAdapter {

    private String url;
    private TokenWrapper tokens;

    private final MetricRegistry metrics;
    private final Timer timer;

    private static final Logger LOG = LoggerFactory.getLogger(EntityServiceAdapter.class);

    public EntityServiceAdapter(String url, MetricRegistry metrics, TokenWrapper tokens) {
        super("EntityServiceAdapter");
        this.url = url;
        this.tokens = tokens;
        this.metrics = metrics;
        this.timer = metrics.timer("entity-adapter.entity-service");
    }

    private static class BaseEntity extends HashMap<String, Object> {}
    private static class BaseEntityList extends ArrayList<BaseEntity> {}

    private HttpHeaders getWithAuth() {
        HttpHeaders headers = new HttpHeaders();

        if (tokens != null) {
            headers.add("Authorization", "Bearer " + tokens.get());
        }

        return headers;
    }

    @Override
    public Collection<Entity> getCollection() {
        RestTemplate rt = new RestTemplate();
        HttpEntity<String> request;

        if(tokens != null) {
            final String accessToken = tokens.get();
            LOG.info("Querying entities with token " + accessToken.substring(0, Math.min(accessToken.length(), 3)) + "..");
            request = new HttpEntity<>(getWithAuth());
        }
        else {
            // FIXME: this branch is never reached
            LOG.info("Querying entity service");
            request = new HttpEntity<>(new HttpHeaders());
        }

        Timer.Context tC = timer.time();
        ResponseEntity<BaseEntityList> response = rt.exchange(url + "/api/v1/entities/", HttpMethod.GET, request, BaseEntityList.class);
        LOG.info("Entity Service Adapter used: {}ms", tC.stop() / 1000000);

        BaseEntityList list = response.getBody();
        List<Entity> entityList = new ArrayList<>(list.size());

        for(BaseEntity base: list) {
            Entity entity = new Entity((String) base.get("id"), this.getName());
            entity.addProperties(base);
            entityList.add(entity);
        }

        return entityList;
    }
}
