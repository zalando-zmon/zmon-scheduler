package de.zalando.zmon.scheduler.ng.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jmussler on 3/31/15.
 */
public class Entity {
    private final Map<String, Object> filterProperties = new HashMap<>();
    private final Map<String, Object> properties = new HashMap<>();
    private String id;
    private String adapterName;

    public Entity(String id, String adapterName) {
        this.id = id;
        this.adapterName = adapterName;

        filterProperties.put("id", id);
        properties.put("id", id);
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getFilterProperties() {
        return filterProperties;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    private void addFilterProperties(Map<String, Object> valueMap) {
        for (Map.Entry<String, Object> e : valueMap.entrySet()) {
            if (e.getValue() instanceof String) {
                filterProperties.put(e.getKey(), e.getValue());
            } else if (e.getValue() instanceof Integer) {
                filterProperties.put(e.getKey(), e.getValue() + "");
            } else if (e.getValue() instanceof java.util.Collection) {
                List<Object> list = new ArrayList<>();
                for (Object o : (java.util.List) e.getValue()) {
                    if (o instanceof String || o instanceof Integer)
                        list.add(o);
                }
                filterProperties.put(e.getKey(), list);
            }
        }
    }

    public void addProperty(String key, Object value) {
        properties.put(key, value);
        if (value instanceof String) {
            filterProperties.put(key, value);
        } else if (value instanceof Integer) {
            filterProperties.put(key, value);
        }
    }

    public void addProperties(Map<String, Object> valueMap) {
        properties.putAll(valueMap);
        addFilterProperties(valueMap);
    }
}
