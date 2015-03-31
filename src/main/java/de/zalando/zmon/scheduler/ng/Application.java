package de.zalando.zmon.scheduler.ng;

/**
 * Created by jmussler on 3/26/15.
 */

import com.codahale.metrics.MetricRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;

@RestController
@EnableAutoConfiguration
@Configuration
@ComponentScan
public class Application {

    @Autowired
    MetricRegistry metrics;

    @Autowired
    Scheduler scheduler;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }
}
