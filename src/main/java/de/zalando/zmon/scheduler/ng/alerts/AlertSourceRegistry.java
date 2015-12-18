package de.zalando.zmon.scheduler.ng.alerts;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import com.codahale.metrics.MetricRegistry;

import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.SourceRegistry;
import de.zalando.zmon.scheduler.ng.ZalandoAlertConfig;
import de.zalando.zmon.scheduler.ng.ZalandoControllerConfig;

/**
 * Created by jmussler on 4/7/15.
 */
@Component
public class AlertSourceRegistry extends SourceRegistry<AlertSource> {

    private final MetricRegistry metrics;

    public AlertSourceRegistry(final MetricRegistry metrics) {
        this.metrics = metrics;
    }

    @Autowired(required = false)
    public AlertSourceRegistry(final SchedulerConfig config, final MetricRegistry metrics) {
        this.metrics = metrics;

        final DefaultAlertSource source = new DefaultAlertSource("alert-source",
                config.controller_url() + "/rest/api/v1/checks/all-active-alert-definitions", config.controller_user(),
                config.controller_password(), config.controller_token(), metrics);
        register(source);
    }

    @Autowired(required = false)
    public AlertSourceRegistry(final ZalandoAlertConfig zConfig, final SchedulerConfig config,
            final MetricRegistry metrics) {
        this.metrics = metrics;

        if (zConfig.controller() != null && zConfig.controller().getUrl() != null
                && !"".equals(zConfig.controller().url())) {
            final ZalandoControllerConfig conf = zConfig.controller();
            final DefaultAlertSource source = new DefaultAlertSource(conf.name(), conf.url(), conf.user(),
                    conf.password(), conf.token(), metrics);
            register(source);
        }
    }
}
