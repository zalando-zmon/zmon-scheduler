package de.zalando.zmon.scheduler.ng.entities;

import com.codahale.metrics.MetricRegistry;
import de.zalando.zmon.scheduler.ng.ZalandoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by jmussler on 4/1/15.
 */
@Component
public class EntityAdapterRegistry {

    private final static Logger LOG = LoggerFactory.getLogger(EntityAdapterRegistry.class);

    private final Map<String, EntityAdapter> adapters = new HashMap<>();

    public void registerAdapter(EntityAdapter a) {
        LOG.info("Register entity adapter: {}", a.getName());
        adapters.put(a.getName(), a);
    }

    private final static EntityAdapter EMPTY_ADAPTER = new EmptyAdapter();

    private final MetricRegistry metrics;

    public EntityAdapterRegistry(MetricRegistry metrics) {
        this.metrics = metrics;
        registerAdapter(EMPTY_ADAPTER);
    }

    @Autowired
    public EntityAdapterRegistry(ZalandoConfig zConfig, MetricRegistry metrics) {
        this.metrics = metrics;

        if(zConfig.cmdb != null && zConfig.cmdb.url != null) {
            CmdbAdapter a = new CmdbAdapter(zConfig.cmdb.url, zConfig.cmdb.user, zConfig.cmdb.password, metrics);
            registerAdapter(a);
        }

        if(zConfig.deployctl != null && zConfig.deployctl.url != null) {
            DeployCtlInstanceAdapter a = new DeployCtlInstanceAdapter(metrics);
            registerAdapter(a);
        }
    }

    public EntityAdapter getAdapter(String name) {
        if(!adapters.containsKey(name)) return EMPTY_ADAPTER;
        return adapters.get(name);
    }

    public Collection<String> getRegisteredAdapters() {
        return adapters.keySet();
    }

}
