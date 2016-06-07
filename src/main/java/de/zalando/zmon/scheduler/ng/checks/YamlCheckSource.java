package de.zalando.zmon.scheduler.ng.checks;

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

/**
 * Created by jmussler on 4/17/15.
 */
public class YamlCheckSource extends CheckSource {

    private static final Logger LOG = LoggerFactory.getLogger(YamlCheckSource.class);

    private String fileName;

    public YamlCheckSource(String name, String fileName) {
        super(name);
        this.fileName = fileName;
    }

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    static {
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }

    @Override
    public Collection<CheckDefinition> getCollection() {
        try {
            List<CheckDefinition> list = mapper.readValue(new File(fileName), new TypeReference<List<CheckDefinition>>() {
            });
            return list;
        } catch (Exception e) {
            LOG.error("", e);
        }
        return new ArrayList<>();
    }
}
