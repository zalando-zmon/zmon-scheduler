package de.zalando.zmon.scheduler.ng.entities;

import com.codahale.metrics.MetricRegistry;
import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.SourceRegistry;
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
public class EntityAdapterRegistry extends SourceRegistry<EntityAdapter> {

    private final static Logger LOG = LoggerFactory.getLogger(EntityAdapterRegistry.class);

    private final static EntityAdapter EMPTY_ADAPTER = new EmptyAdapter();

    private final MetricRegistry metrics;

    public EntityAdapterRegistry(MetricRegistry metrics) {
        this.metrics = metrics;
    }

    @Autowired(required=false)
    public EntityAdapterRegistry(SchedulerConfig config, MetricRegistry metrics) {
        this.metrics = metrics;
        register(EMPTY_ADAPTER);

        if(config.enable_global_entity()) {
            register(new GlobalAdapter());
        }

        if(config.entity_service_url() !=null) {
            EntityServiceAdapter e = new EntityServiceAdapter(config.entity_service_url(), config.entity_service_user(), config.entity_service_password(), metrics);
            register(e);
        }

        if(config.dummy_cities()!=null && !config.dummy_cities().equals("")) {
            register(new YamlEntityAdapter("dummy-cities",config.dummy_cities(),"city"));
        }
    }

    @Autowired(required=false)
    public EntityAdapterRegistry(SchedulerConfig config, ZalandoConfig zConfig, MetricRegistry metrics) {
        this.metrics = metrics;

        if(config.enable_global_entity()) {
            register(new GlobalAdapter());
        }

        if(config.dummy_cities()!=null && !config.dummy_cities().equals("")) {
            register(new YamlEntityAdapter("dummy-cities",config.dummy_cities(),"city"));
        }

        if(zConfig.cmdb != null && zConfig.cmdb.url != null) {
            CmdbAdapter c = new CmdbAdapter(zConfig.cmdb.url, zConfig.cmdb.user, zConfig.cmdb.password, metrics);
            register(c);
        }

        if(zConfig.deployctl != null && zConfig.deployctl.url != null) {
            DeployCtlInstanceAdapter d = new DeployCtlInstanceAdapter(zConfig.deployctl.url, zConfig.deployctl.user, zConfig.deployctl.password, metrics);
            register(d);
        }

        if(zConfig.entityservice != null && zConfig.entityservice.url != null) {
            EntityServiceAdapter e = new EntityServiceAdapter(zConfig.entityservice.url, zConfig.entityservice.user, zConfig.entityservice.password, metrics);
            register(e);
        }

        if(zConfig.ddscluster != null && zConfig.ddscluster.url != null) {
            DDSClusterAdapter dds = new DDSClusterAdapter(zConfig.ddscluster.url, metrics);
            register(dds);
        }
    }

}
