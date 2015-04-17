package de.zalando.zmon.scheduler.ng.alerts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by jmussler on 4/17/15.
 */
public class YamlAlertSource extends AlertSource {

    private static final Logger LOG = LoggerFactory.getLogger(YamlAlertSource.class);

    private String fileName;

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public YamlAlertSource(String name, String fileName) {
        super(name);
        this.fileName = fileName;
    }

    public YamlAlertSource(String name, int refresh, String fileName) {
        super(name, refresh);
        this.fileName = fileName;
    }

    @Override
    public Collection<AlertDefinition> getCollection() {
        try {
            List<AlertDefinition> list = mapper.readValue(new File(fileName), new TypeReference<List<AlertDefinition>>(){});
            return list;
        }
        catch (Exception e) {
            LOG.error("", e);
        }
        return new ArrayList<>();
    }
}
