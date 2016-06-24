package de.zalando.zmon.scheduler.ng.checks;

import com.codahale.metrics.MetricRegistry;
import de.zalando.zmon.scheduler.ng.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;

/**
 * Created by jmussler on 4/2/15.
 */
@Component
public class CheckSourceRegistry extends SourceRegistry<CheckSource> {

    public CheckSourceRegistry(MetricRegistry metrics) {

    }

    @Autowired
    public CheckSourceRegistry(SchedulerConfig config, final TokenWrapper tokens, ClientHttpRequestFactory clientFactory) {
        final String url = config.controller_url() + (config.urls_without_rest() ? "" : "/rest") + "/api/v1/checks/all-active-check-definitions";
        final DefaultCheckSource source = new DefaultCheckSource("check-source", url, tokens, clientFactory);
        register(source);
    }

}
