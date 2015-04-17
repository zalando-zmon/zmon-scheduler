package de.zalando.zmon.scheduler.ng.checks;

import com.codahale.metrics.MetricRegistry;
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

    public CheckSourceRegistry() {

    }

    public CheckSourceRegistry(MetricRegistry metrics) {

    }

    @Autowired
    public CheckSourceRegistry(ZalandoCheckConfig config) {


        if(config.controller()!=null && config.controller().getUrl()!=null && !"".equals(config.controller().url())) {
            ZalandoControllerConfig conf = config.controller();

            DefaultCheckSource source = new DefaultCheckSource(conf.name(), conf.url(), conf.user(), conf.password());
            register(source);
        }
    }
}
