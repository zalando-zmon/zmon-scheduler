package de.zalando.zmon.scheduler.ng.alerts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.codahale.metrics.MetricRegistry;

import de.zalando.zmon.scheduler.ng.SourceRegistry;
import de.zalando.zmon.scheduler.ng.TokenWrapper;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;

/**
 * Created by jmussler on 4/7/15.
 */
@Component
public class AlertSourceRegistry extends SourceRegistry<AlertSource> {

    @Autowired
    public AlertSourceRegistry(final SchedulerConfig config, final MetricRegistry metrics, final TokenWrapper tokens, RestTemplate restTemplate) {

        final String url = config.getControllerUrl() + "/api/v1/checks/all-active-alert-definitions";
        final DefaultAlertSource source = new DefaultAlertSource("alert-source", url, metrics, tokens, restTemplate);

        register(source);
    }

}
