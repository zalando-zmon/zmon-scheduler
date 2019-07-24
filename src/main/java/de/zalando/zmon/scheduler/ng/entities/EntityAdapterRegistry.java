package de.zalando.zmon.scheduler.ng.entities;

import de.zalando.zmon.scheduler.ng.entities.adapters.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.SourceRegistry;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

/**
 * Created by jmussler on 4/1/15.
 */
@Component
public class EntityAdapterRegistry extends SourceRegistry<EntityAdapter> {

    private final static Logger LOG = LoggerFactory.getLogger(EntityAdapterRegistry.class);

    private final static EntityAdapter EMPTY_ADAPTER = new EmptyAdapter();

    public EntityAdapterRegistry(MetricRegistry metrics) {
    }

    @Autowired(required = false)
    public EntityAdapterRegistry(SchedulerConfig config, MetricRegistry metrics, RestTemplate restTemplate) {
        register(EMPTY_ADAPTER);

        if (config.isEnableGlobalEntity()) {
            register(new GlobalAdapter());
        }

        if (config.getEntityServiceUrl() != null && !config.getEntityServiceUrl().equals("")) {
            String entityServiceUrl = config.getEntityServiceUrl() + "/api/v1/entities";
            try {
                if (config.getEntityBaseFilterStr() != null && !"".equals(config.getEntityBaseFilterStr()) && config.isBaseFilterForward()) {
                    entityServiceUrl = entityServiceUrl + "?query=" + URLEncoder.encode(config.getEntityBaseFilterStr(), "UTF-8");
                } else if (config.getEntitySkipOnField() != null && !"".equals(config.getEntitySkipOnField()) {
                    try {
                        entityServiceUrl = entityServiceUrl + "?exclude=" + URLEncoder.encode(config.getEntitySkipOnField(), "UTF-8");
                    } catch(UnsupportedEncodingException ex) {
                        LOG.error("Encoding of skip filter query param failed");
                    }
                } else if (config.getEntityBaseFilter() != null && config.isBaseFilterForward()) {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        entityServiceUrl = entityServiceUrl + "?query=" + URLEncoder.encode(mapper.writeValueAsString(config.getEntityBaseFilter()), "UTF-8");
                    } catch (JsonProcessingException ex) {
                        LOG.error("Could not serialize base filter: {}", ex.getMessage());
                    }
                }
            }
            catch(UnsupportedEncodingException ex) {
                LOG.error("Encoding of base filter query param failed");
            }

            EntityServiceAdapter e = new EntityServiceAdapter(URI.create(entityServiceUrl), metrics, restTemplate);
            register(e);
        }

        if (config.getDummyCities() != null && !config.getDummyCities().equals("")) {
            register(new YamlEntityAdapter("dummy-cities", config.getDummyCities(), "city"));
        }
    }

}
