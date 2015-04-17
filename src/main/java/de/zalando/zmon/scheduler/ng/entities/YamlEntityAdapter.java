package de.zalando.zmon.scheduler.ng.entities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by jmussler on 4/17/15.
 */
public class YamlEntityAdapter extends EntityAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(YamlEntityAdapter.class);

    private String fileName;

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public YamlEntityAdapter(String name, String fileName) {
        super(name);
        this.fileName = fileName;
    }

    @Override
    public Collection<Entity> getCollection() {
        try {
            List<Map<String,Object>> list = mapper.readValue(new File(fileName), new TypeReference<List<Map<String,Object>>>(){});
            List<Entity> entityList = new ArrayList<>();

            for(Map<String,Object> m : list) {
                Entity e = new Entity((String)m.get("id"), getName());
                e.addProperties(m);
                entityList.add(e);
            }

            return entityList;
        }
        catch (Exception e) {
            LOG.error("", e);
        }
        return new ArrayList<>();
    }
}
