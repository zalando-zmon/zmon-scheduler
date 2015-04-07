package de.zalando.zmon.scheduler.ng;

/**
 * Created by jmussler on 3/26/15.
 */

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.zalando.zmon.scheduler.ng.alerts.AlertDefinition;
import de.zalando.zmon.scheduler.ng.alerts.AlertSourceRegistry;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckSourceRegistry;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityAdapterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RestController
@EnableAutoConfiguration
@EnableConfigurationProperties
@Configuration
@ComponentScan
public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    @Autowired
    EntityAdapterRegistry entityRegistry;

    @Autowired
    CheckSourceRegistry checkSourceRegistry;

    @Autowired
    AlertSourceRegistry alertSourceRegistry;

    @Autowired
    MetricRegistry metrics;

    @Autowired
    ObjectMapper mapper;

    @RequestMapping(value="/api/v1/entity-adapter", method=RequestMethod.GET)
    Collection<String> getAdapters() {
        return entityRegistry.getSourceNames();
    }

    @RequestMapping(value="/api/v1/entity-adapter/{name}", method=RequestMethod.GET)
    Collection<Entity> getEntities(@PathVariable(value = "name") String name) {
        return entityRegistry.get(name).getCollection();
    }

    @RequestMapping(value="/api/v1/check-source", method=RequestMethod.GET)
    Collection<String> getCheckSources() {
        return checkSourceRegistry.getSourceNames();
    }

    @RequestMapping(value="/api/v1/check-source/{name}", method=RequestMethod.GET)
    Collection<CheckDefinition> getChecks(@PathVariable(value="name") String name) {
        return checkSourceRegistry.get(name).getCollection();
    }

    @RequestMapping(value="/api/v1/alert-source", method=RequestMethod.GET)
    Collection<String> getAlertSources() {
        return alertSourceRegistry.getSourceNames();
    }

    @RequestMapping(value="/api/v1/alert-source/{name}", method=RequestMethod.GET)
    Collection<AlertDefinition> getAlerts(@PathVariable(value="name") String name) {
        return alertSourceRegistry.get(name).getCollection();
    }

    private static class Test2 {
        public String fieldName = "name";
        public String fieldFirstName ="FirstName";
    }
    private static class Test1 {
        public String abc ="abc";
        public Test2 fieldTest = new Test2();
    }

    @RequestMapping(value="/api/v1/test", method=RequestMethod.GET)
    Test1 sgetTestObject() {
        return new Test1();
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }
}
