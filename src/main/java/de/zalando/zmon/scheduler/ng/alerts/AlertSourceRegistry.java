package de.zalando.zmon.scheduler.ng.alerts;

import com.codahale.metrics.MetricRegistry;
import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.SourceRegistry;
import de.zalando.zmon.scheduler.ng.ZalandoAlertConfig;
import de.zalando.zmon.scheduler.ng.ZalandoControllerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Created by jmussler on 4/7/15.
 */
@Component
public class AlertSourceRegistry extends SourceRegistry<AlertSource> {

    private final MetricRegistry metrics;

    public AlertSourceRegistry(MetricRegistry metrics) {
        this.metrics= metrics;
    }

    @Autowired(required=false)
    public AlertSourceRegistry(SchedulerConfig config, MetricRegistry metrics) {
        this.metrics = metrics;
        DefaultAlertSource source = new DefaultAlertSource("alert-source", config.controller_url()+"/rest/api/v1/checks/all-active-alert-definitions", config.controller_user(), config.controller_password(), metrics);
        register(source);
    }

    @Autowired(required=false)
    public AlertSourceRegistry(ZalandoAlertConfig zConfig, SchedulerConfig config, MetricRegistry metrics) {
        this.metrics = metrics;

        if(zConfig.controller()!=null && zConfig.controller().getUrl()!=null && !"".equals(zConfig.controller().url())) {
            ZalandoControllerConfig conf = zConfig.controller();
            DefaultAlertSource source = new DefaultAlertSource(conf.name(), conf.url(), conf.user(), conf.password(), metrics);
            register(source);
        }
    }
}
