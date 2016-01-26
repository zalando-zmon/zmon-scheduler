package de.zalando.zmon.scheduler.ng.alerts;

import de.zalando.zmon.scheduler.ng.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import com.codahale.metrics.MetricRegistry;

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
    public AlertSourceRegistry(final SchedulerConfig config, final MetricRegistry metrics, final TokenWrapper tokens) {
        this.metrics = metrics;

        final DefaultAlertSource source = new DefaultAlertSource("alert-source",
                config.controller_url() + (config.urls_without_rest() ? "" : "/rest") + "/api/v1/checks/all-active-alert-definitions", metrics, tokens);

        register(source);
    }

    @Autowired(required = false)
    public AlertSourceRegistry(final ZalandoAlertConfig zConfig, final SchedulerConfig config,
            final MetricRegistry metrics) {
        this.metrics = metrics;

        if (zConfig.controller() != null && zConfig.controller().getUrl() != null
                && !"".equals(zConfig.controller().url())) {
            final ZalandoControllerConfig conf = zConfig.controller();
            final DefaultAlertSource source = new DefaultAlertSource(conf.name(), conf.url(), metrics, null);
            register(source);
        }
    }
}
