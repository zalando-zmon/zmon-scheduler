package de.zalando.zmon.scheduler.ng.entities;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jmussler on 3/31/15.
 */
public class Entity {
    private final Map<String, String> filterProperties = new HashMap<>();
    private final Map<String, Object> properties = new HashMap<>();
    private String id;
    private String adapterName;

    public Entity(String id, String adapterName) {
        this.id = id;
        this.adapterName = adapterName;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getProperties() {
        return filterProperties;
    }

    private void addFilterProperties(Map<String, Object> valueMap) {
        for (Map.Entry<String, Object> e : valueMap.entrySet()) {
            if (e.getValue() instanceof String) {
                filterProperties.put(e.getKey(), (String) e.getValue());
            }
        }
    }

    public void addProperties(Map<String, Object> valueMap) {
        properties.putAll(valueMap);
        addFilterProperties(valueMap);
    }
}
