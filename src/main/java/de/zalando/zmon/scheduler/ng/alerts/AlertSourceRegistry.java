package de.zalando.zmon.scheduler.ng.alerts;

import com.codahale.metrics.MetricRegistry;
import de.zalando.zmon.scheduler.ng.SourceRegistry;
import de.zalando.zmon.scheduler.ng.ZalandoAlertConfig;
import de.zalando.zmon.scheduler.ng.ZalandoControllerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by jmussler on 4/7/15.
 */
@Component
public class AlertSourceRegistry extends SourceRegistry<AlertSource> {

    private MetricRegistry metrics = null;

    @Autowired
    public AlertSourceRegistry(ZalandoAlertConfig config,  MetricRegistry metrics) {
        this.metrics = metrics;

        if(config.controller()!=null && config.controller().getUrl()!=null && !"".equals(config.controller().url())) {
            ZalandoControllerConfig conf = config.controller();
            DefaultAlertSource source = new DefaultAlertSource(conf.name(), conf.url(), conf.user(), conf.password(), metrics);
            register(source);
        }
    }
}
