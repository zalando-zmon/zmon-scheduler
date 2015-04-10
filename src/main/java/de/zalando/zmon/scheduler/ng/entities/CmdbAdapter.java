package de.zalando.zmon.scheduler.ng.entities;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Created by jmussler on 4/1/15.
 */
public class CmdbAdapter extends EntityAdapter {

    private final String url;
    private final String user;
    private final String password;
    private final MetricRegistry metrics;
    private final Timer timer;

    private static final List<String> FIELDS = Arrays.asList("teams","data_center_code","host","id","host_role_id","role_name","type","external_ip","internal_ip","virt_type","physcial_machine");

    private static final Logger LOG = LoggerFactory.getLogger(CmdbAdapter.class);

    public CmdbAdapter(String url, String user, String password, MetricRegistry metrics) {
        super("CmdbAdapter");
        this.url = url;
        this.user = user;
        this.password = password;
        this.metrics = metrics;
        this.timer = metrics.timer("entity-adapter.cmdb");
    }

    private static class BaseEntity extends HashMap<String, Object> {}
    private static class BaseEntityList extends ArrayList<BaseEntity> {}

    private HttpHeaders getWithAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString((user+":"+password).getBytes()));
        return headers;
    }

    @Override
    public String getName() {
        return "CmdbAdapter";
    }

    @Override
    public Collection<Entity> getCollection() {
        LOG.info("Loading collection...");


        RestTemplate rt = new RestTemplate();
        HttpEntity<String> request = new HttpEntity<>(getWithAuth());

        Timer.Context tC = timer.time();
        BaseEntityList list = rt.postForObject(url, request, BaseEntityList.class);
        LOG.info("Cmdb Adapter used: {}ms", tC.stop() / 1000000);

        List<Entity> entityList = new ArrayList<>(list.size());

        for(BaseEntity base: list) {
            String hostName = (String) base.get("hostname");
            if(hostName == null || "".equals(hostName)) {
                continue;
            }

            if(base.containsKey("physical_machine")) {
                Map<String, Object> physicalMachine = (Map<String, Object>) base.get("physical_machine");
                if(null!=physicalMachine && physicalMachine.containsKey("data_center_code")) {
                    base.put("data_center_code", physicalMachine.get("data_center_code"));
                }

                if(null != physicalMachine && physicalMachine.containsKey("physical_machine_model")) {
                    base.put("model", physicalMachine.get("physical_machine_model"));
                }
                base.remove("physical_machine");
            }

            Set<String> keys = new HashSet<>(base.keySet());
            for(String k : keys) {
                if(!FIELDS.contains(k)) {
                    base.remove(k);
                }
            }

            base.put("id", hostName);
            base.put("host", hostName);
            base.put("type","host");

            Entity entity = new Entity(hostName, "CmdbAdapter");
            entity.addProperties(base);
            entityList.add(entity);
        }

        return entityList;
    }
}
