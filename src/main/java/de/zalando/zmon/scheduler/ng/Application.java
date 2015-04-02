package de.zalando.zmon.scheduler.ng;

/**
 * Created by jmussler on 3/26/15.
 */

import com.codahale.metrics.MetricRegistry;
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

    @Autowired(required = false)
    ZalandoConfig zConfig;

    @Autowired
    TestConfig testConfig;

    @Autowired
    EntityAdapterRegistry registry;

    @Autowired
    MetricRegistry metrics;

    @Autowired
    Scheduler scheduler;

    @RequestMapping(value="/api/v1/adapter", method=RequestMethod.GET)
    Collection<String> getAdapters() {
        return registry.getRegisteredAdapters();
    }

    @RequestMapping(value="/api/v1/adapter/{name}", method=RequestMethod.GET)
    List<Entity> getEntities(@PathVariable(value = "name") String name) {
        return registry.getAdapter(name).getEntities();
    }

    @RequestMapping(value="/api/v1/testconfig", method=RequestMethod.GET)
    TestConfig getTestConfig() {
        return testConfig;
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }
}
