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

    @Autowired(required=false)
    public CheckSourceRegistry(SchedulerConfig config, final TokenWrapper tokens, ClientHttpRequestFactory clientFactory) {
        DefaultCheckSource source = new DefaultCheckSource("check-source", config.controller_url()+ (config.urls_without_rest() ? "" : "/rest") + "/api/v1/checks/all-active-check-definitions", tokens, clientFactory);
        register(source);
    }

    @Autowired(required=false)
    public CheckSourceRegistry(ZalandoCheckConfig config, SchedulerConfig schedulerConfig, ClientHttpRequestFactory clientFactory) {

        if(config.controller()!=null && config.controller().getUrl()!=null && !"".equals(config.controller().url())) {
            ZalandoControllerConfig conf = config.controller();

            DefaultCheckSource source = new DefaultCheckSource(conf.name(), conf.url(), null, clientFactory);
            register(source);
        }
    }
}
