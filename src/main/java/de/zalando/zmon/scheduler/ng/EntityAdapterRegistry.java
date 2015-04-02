package de.zalando.zmon.scheduler.ng;

import de.zalando.zmon.scheduler.ng.entities.CmdbAdapter;
import de.zalando.zmon.scheduler.ng.entities.DeployCtlInstanceAdapter;
import de.zalando.zmon.scheduler.ng.entities.EmptyAdapter;
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

    public EntityAdapterRegistry() {
        registerAdapter(EMPTY_ADAPTER);
    }

    @Autowired
    public EntityAdapterRegistry(ZalandoConfig zConfig) {
        if(zConfig.cmdb != null && zConfig.cmdb.url != null) {
            CmdbAdapter a = new CmdbAdapter(zConfig.cmdb.url, zConfig.cmdb.user, zConfig.cmdb.password);
            registerAdapter(a);
        }

        if(zConfig.deployctl != null && zConfig.deployctl.url != null) {
            DeployCtlInstanceAdapter a = new DeployCtlInstanceAdapter();
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
