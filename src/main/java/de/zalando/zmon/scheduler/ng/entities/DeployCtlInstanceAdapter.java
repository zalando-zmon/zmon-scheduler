package de.zalando.zmon.scheduler.ng.entities;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Created by jmussler on 4/2/15.
 */
public class DeployCtlInstanceAdapter extends EntityAdapter {

    private final static Logger LOG = LoggerFactory.getLogger(DeployCtlInstanceAdapter.class);

    private MetricRegistry metrics;
    private Timer timer;

    private String url;
    private String user;
    private String password;

    private static class BaseEntity extends HashMap<String, Object> {}
    private static class BaseEntityList extends ArrayList<BaseEntity> {}

    private static final List<String> FIELDS = Arrays.asList("environment","host","instance","path","project","project_organization","project_type","url","load_balancer_status");
    private static final List<String> ZOMCAT_TYPES = Arrays.asList("maven-war","maven-pom","maven-grails-app");
    
    public DeployCtlInstanceAdapter(String url, String user, String password, MetricRegistry metrics) {
        super("DeployCtlAdapter");
        this.url = url;
        this.user = user;
        this.password = password;
        this.metrics = metrics;
        this.timer = metrics.timer("entity-adapter.deployctlinstances");
    }

    private HttpHeaders getWithAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString((user+":"+password).getBytes()));
        return headers;
    }

    @Override
    public Collection<Entity> getCollection() {

        RestTemplate rt = new RestTemplate();
        HttpEntity<String> request = new HttpEntity<>(getWithAuth());

        LOG.info("Querying deployctl with credentials {}", user);
        Timer.Context tC = timer.time();
        BaseEntityList list = rt.postForObject(url, request, BaseEntityList.class);
        LOG.info("DeployCtlInstance Adapter used: {}ms", tC.stop() / 1000000);

        List<Entity> entities = new ArrayList<>();

        for(BaseEntity base : list) {
            if(!base.get("status").equals("ALLOCATED")) continue;
            if(base.get("instance").equals("9999")) continue;

            Entity entity = new Entity(base.get("host")+":"+base.get("instance"), "DeployCtlInstanceAdapter");

            if(base.containsKey("current")) {
                Map<String, Object> current = (Map<String, Object>) base.get("current");
                if(null != current) {
                    String lbStatus = (String) current.get("load_balancer_status");
                    if(null != lbStatus) {
                        base.put("load_balancer_status", lbStatus);
                    }
                }
            }

            Set<String> baseKeys = new HashSet<>(base.keySet());
            for(String k : baseKeys) {
                if(!FIELDS.contains(k)) {
                    base.remove(k);
                }
            }

            if(ZOMCAT_TYPES.contains(base.get("project_type"))) {
                base.put("type", "zomcat");
            }
            else {
                base.put("type", base.get("project_type"));
            }

            base.put("environment", Environments.getNormalized((String) base.get("environment")));
            base.put("team", Teams.getNormalizedTeam((String) base.get("project_organization")));

            entity.addProperties(base);
            entities.add(entity);
        }

        return entities;
    }
}
