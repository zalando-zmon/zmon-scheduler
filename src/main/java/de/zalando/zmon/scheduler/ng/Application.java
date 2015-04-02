package de.zalando.zmon.scheduler.ng;

/**
 * Created by jmussler on 3/26/15.
 */

import com.codahale.metrics.MetricRegistry;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
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
    MetricRegistry metrics;

    @Autowired
    Scheduler scheduler;

    @RequestMapping(value="/api/v1/entity-adapter", method=RequestMethod.GET)
    Collection<String> getAdapters() {
        return entityRegistry.getRegisteredAdapters();
    }

    @RequestMapping(value="/api/v1/entity-adapter/{name}", method=RequestMethod.GET)
    List<Entity> getEntities(@PathVariable(value = "name") String name) {
        return entityRegistry.getAdapter(name).getEntities();
    }

    @RequestMapping(value="/api/v1/check-source", method=RequestMethod.GET)
    Collection<String> getSources() {
        return checkSourceRegistry.getSources();
    }

    @RequestMapping(value="/api/v1/check-source/{name}", method=RequestMethod.GET)
    Collection<CheckDefinition> getChecks(@PathVariable(value="name") String name) {
        return checkSourceRegistry.getCheckSource(name).getCheckData();
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }
}
