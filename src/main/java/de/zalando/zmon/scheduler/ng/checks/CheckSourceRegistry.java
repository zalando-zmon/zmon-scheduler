package de.zalando.zmon.scheduler.ng.checks;

import com.codahale.metrics.MetricRegistry;
import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.SourceRegistry;
import de.zalando.zmon.scheduler.ng.ZalandoCheckConfig;
import de.zalando.zmon.scheduler.ng.ZalandoControllerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jmussler on 4/2/15.
 */
@Component
public class CheckSourceRegistry extends SourceRegistry<CheckSource> {

    public CheckSourceRegistry(MetricRegistry metrics) {

    }

    @Autowired(required=false)
    public CheckSourceRegistry(SchedulerConfig config) {
        DefaultCheckSource source = new DefaultCheckSource("check-source", config.controller_url()+"/rest/api/v1/checks/all-active-check-definitions",config.controller_user(),config.controller_password());
        register(source);
    }

    @Autowired(required=false)
    public CheckSourceRegistry(ZalandoCheckConfig config) {

        if(config.controller()!=null && config.controller().getUrl()!=null && !"".equals(config.controller().url())) {
            ZalandoControllerConfig conf = config.controller();

            DefaultCheckSource source = new DefaultCheckSource(conf.name(), conf.url(), conf.user(), conf.password());
            register(source);
        }
    }
}
