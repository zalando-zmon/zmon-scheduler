package de.zalando.zmon.scheduler.ng.entities;

import de.zalando.zmon.scheduler.ng.Entity;
import de.zalando.zmon.scheduler.ng.EntityAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Created by jmussler on 4/1/15.
 */
public class CmdbAdapter implements EntityAdapter {

    private final String url;
    private final String user;
    private final String password;

    private static Logger LOG = LoggerFactory.getLogger(CmdbAdapter.class);

    public CmdbAdapter(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
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
    public List<Entity> getEntities() {

        RestTemplate rt = new RestTemplate();
        HttpEntity<String> request = new HttpEntity<>(getWithAuth());
        BaseEntityList list = rt.postForObject(url, request, BaseEntityList.class);
        List<Entity> entityList = new ArrayList<>(list.size());

        for(BaseEntity base: list) {
            String hostName = (String) base.get("hostname");
            if(hostName == null || "".equals(hostName)) {
                continue;
            }

            if(base.containsKey("physical_machine")) {
                Map<String, Object> physicalMachine = (Map<String, Object>) base.get("physical_machine");
                if(physicalMachine.containsKey("data_center_code")) {
                    base.put("data_center_code", physicalMachine.get("data_center_code"));
                }

                if(physicalMachine.containsKey("physical_machine_model")) {
                    base.put("model", physicalMachine.get("physical_machine_model"));
                }
                base.remove("physical_machine");
            }

            base.put("type","host");

            Entity entity = new Entity(hostName, "CmdbAdapter");
            entity.addProperties(base);
            entityList.add(entity);
        }

        return entityList;
    }
}
