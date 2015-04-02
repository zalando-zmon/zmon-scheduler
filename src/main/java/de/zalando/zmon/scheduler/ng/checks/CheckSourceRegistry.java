package de.zalando.zmon.scheduler.ng.checks;

import de.zalando.zmon.scheduler.ng.ZalandoCheckConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jmussler on 4/2/15.
 */
@Component
public class CheckSourceRegistry {

    private final Map<String, CheckSource> registry = new HashMap<>();

    public CheckSourceRegistry() {

    }

    @Autowired
    public CheckSourceRegistry(ZalandoCheckConfig config) {
        if(config.controller()!=null && config.controller().getUrl()!=null && !"".equals(config.controller().url())) {
            DefaultCheckSource source = new DefaultCheckSource(config.controller().name(), config.controller().url(),config.controller().user(),config.controller().password());
            registerSource(source);
        }
    }

    public void registerSource(CheckSource source) {
        registry.put(source.getName(), source);
    }

    public Collection<String> getSources() {
        return registry.keySet();
    }

    public CheckSource getCheckSource(String name) {
        return registry.get(name);
    }
}
