package de.zalando.zmon.scheduler.ng.entities;

import com.codahale.metrics.MetricRegistry;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.SourceRegistry;
import de.zalando.zmon.scheduler.ng.TokenWrapper;
import de.zalando.zmon.scheduler.ng.config.ZalandoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;

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

    @Autowired(required = false)
    public EntityAdapterRegistry(SchedulerConfig config, MetricRegistry metrics, TokenWrapper tokens, ClientHttpRequestFactory clientFactory) {
        this.metrics = metrics;
        register(EMPTY_ADAPTER);

        if (config.isEnableGlobalEntity()) {
            register(new GlobalAdapter());
        }

        if (config.getEntityServiceUrl() != null && !config.getEntityServiceUrl().equals("")) {
            final String entityServiceUrl = config.getEntityServiceUrl() + "/api/v1/entities/";
            EntityServiceAdapter e = new EntityServiceAdapter(entityServiceUrl, metrics, tokens, clientFactory);
            register(e);
        }

        if (config.getDummyCities() != null && !config.getDummyCities().equals("")) {
            register(new YamlEntityAdapter("dummy-cities", config.getDummyCities(), "city"));
        }
    }

    @Autowired(required = false)
    public EntityAdapterRegistry(SchedulerConfig config, ZalandoConfig zConfig, MetricRegistry metrics, TokenWrapper tokens, ClientHttpRequestFactory clientFactory) {
        this.metrics = metrics;

        if (config.isEnableGlobalEntity()) {
            register(new GlobalAdapter());
        }

        if (config.getDummyCities() != null && !config.getDummyCities().equals("")) {
            register(new YamlEntityAdapter("dummy-cities", config.getDummyCities(), "city", m -> (m.get("country") + "-" + m.get("city"))));
        }

        if (zConfig.cmdb != null && zConfig.cmdb.url != null) {
            CmdbAdapter c = new CmdbAdapter(zConfig.cmdb.url, zConfig.cmdb.user, zConfig.cmdb.password, metrics);
            register(c);
        }

        if (zConfig.deployctl != null && zConfig.deployctl.url != null) {
            DeployCtlInstanceAdapter d = new DeployCtlInstanceAdapter(zConfig.deployctl.url, zConfig.deployctl.user, zConfig.deployctl.password, metrics);
            register(d);
        }

        if (zConfig.projects != null && zConfig.projects.url != null) {
            DeployCtlProjectAdapter d = new DeployCtlProjectAdapter(zConfig.projects.url, zConfig.projects.user, zConfig.projects.password, metrics);
            register(d);
        }

        if (zConfig.entityservice != null && zConfig.entityservice.url != null) {
            EntityServiceAdapter e = new EntityServiceAdapter(zConfig.entityservice.url + "/api/v1/entities/", metrics, tokens, clientFactory);
            register(e);
        }

        if (zConfig.ddscluster != null && zConfig.ddscluster.url != null) {
            DDSClusterAdapter dds = new DDSClusterAdapter(zConfig.ddscluster.url, metrics);
            register(dds);
        }

        if (zConfig.ddsdatabase != null && zConfig.ddsdatabase.url != null) {
            DDSDatabaseAdapter dds = new DDSDatabaseAdapter(zConfig.ddsdatabase.url, metrics);
            register(dds);
        }
    }
}
