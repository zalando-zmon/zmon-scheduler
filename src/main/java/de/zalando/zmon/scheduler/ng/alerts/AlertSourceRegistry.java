package de.zalando.zmon.scheduler.ng.alerts;

import com.codahale.metrics.MetricRegistry;
import de.zalando.zmon.scheduler.ng.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;

/**
 * Created by jmussler on 4/7/15.
 */
@Component
public class AlertSourceRegistry extends SourceRegistry<AlertSource> {

    private static final Logger LOG = LoggerFactory.getLogger(AlertSourceRegistry.class);

    private final MetricRegistry metrics;

    public AlertSourceRegistry(final MetricRegistry metrics) {
        this.metrics = metrics;
    }

    @Autowired(required = false)
    public AlertSourceRegistry(final SchedulerConfig config, final MetricRegistry metrics, final TokenWrapper tokens, ClientHttpRequestFactory clientFactory) {
        this.metrics = metrics;

        final String url = config.controller_url() + "/api/v1/checks/all-active-alert-definitions";
        final DefaultAlertSource source = new DefaultAlertSource("alert-source", url, metrics, tokens, clientFactory);

        register(source);
    }

}
