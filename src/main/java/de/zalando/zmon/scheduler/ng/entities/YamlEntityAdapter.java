package de.zalando.zmon.scheduler.ng.entities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
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

    public interface IdGenerator {
        String f(Map<String, Object> e);
    }

    private static final Logger LOG = LoggerFactory.getLogger(YamlEntityAdapter.class);

    private String fileName;
    private String type = null;
    private IdGenerator idGenerator = m -> (String) m.get("id");

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    static {
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }

    public YamlEntityAdapter(String name, String fileName, String type, IdGenerator g) {
        super(name);
        this.fileName = fileName;
        this.type = type;
        this.idGenerator = g;
    }

    public YamlEntityAdapter(String name, String fileName, String type) {
        super(name);
        this.fileName = fileName;
        this.type = type;
    }

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
                String id = idGenerator.f(m);
                if(null==id || id.equals("")) continue;
                Entity e = new Entity(id, getName());
                if(type!=null) {
                    m.put("type", type);
                }
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
