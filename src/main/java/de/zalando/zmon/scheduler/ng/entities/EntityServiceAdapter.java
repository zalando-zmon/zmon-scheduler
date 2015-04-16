package de.zalando.zmon.scheduler.ng.entities;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import de.zalando.zmon.scheduler.ng.BaseSource;
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
    private String user;
    private String password;

    private final MetricRegistry metrics;
    private final Timer timer;

    private static final Logger LOG = LoggerFactory.getLogger(EntityServiceAdapter.class);

    public EntityServiceAdapter(String url, String user, String password, MetricRegistry metrics) {
        super("EntityServiceAdapter");
        this.url = url;
        this.user = user;
        this.password = password;
        this.metrics = metrics;
        this.timer = metrics.timer("entity-adapter.entity-service");
    }

    private static class BaseEntity extends HashMap<String, Object> {}
    private static class BaseEntityList extends ArrayList<BaseEntity> {}

    private HttpHeaders getWithAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString((user+":"+password).getBytes()));
        return headers;
    }

    @Override
    public Collection<Entity> getCollection() {
        RestTemplate rt = new RestTemplate();
        HttpEntity<String> request = new HttpEntity<>(getWithAuth());

        Timer.Context tC = timer.time();
        ResponseEntity<BaseEntityList> response = rt.exchange(url + "/rest/api/v1/entities/", HttpMethod.GET, request, BaseEntityList.class);
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
