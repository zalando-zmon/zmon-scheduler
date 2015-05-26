package de.zalando.zmon.scheduler.ng.entities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.zalando.zmon.scheduler.ng.CachedRepository;
import de.zalando.zmon.scheduler.ng.InstantEvalForwarder;
import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.event.ChangeListener;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jmussler on 4/7/15.
 */
@Component
public class EntityRepository extends CachedRepository<String, EntityAdapterRegistry, Entity> {

    private List<Map<String,String>> baseFilter = null;
    private final String skipField;
    private static final Logger LOG = LoggerFactory.getLogger(EntityRepository.class);

    private List<EntityChangeListener> changeListeners = new ArrayList<>();

    public synchronized void registerListener(EntityChangeListener l) {
        LOG.info("Registering entity change listener ({})", l.getClass());
        Map<String, Entity> m = currentMap;
        for(String k : m.keySet()) {
            l.notifyEntityAdd(this, m.get(k));
        }
        changeListeners.add(l);
    }

    private synchronized List<EntityChangeListener> getCurrentListeners() {
        return new ArrayList<>(changeListeners);
    }

    @Override
    protected void fill() {
        Map<String, Entity> m = new HashMap<>();

        for(String name : registry.getSourceNames()) {
            for(Entity e: registry.get(name).getCollection()) {

                // try to map dc code and external ip if host is known ( relies on ordering of adapters :( and does not work for entity service )
                Map<String, Object> p = e.getProperties();
                if(p.containsKey("host") && !p.containsKey("external_ip")) {
                    String host = (String) p.get("host");
                    Entity hostEntity = m.get(host);
                    if(null != hostEntity) {
                        String externalIp = (String)hostEntity.getProperties().get("external_ip");
                        if(null != externalIp) {
                            e.addProperty("external_ip", externalIp);
                        }
                    }
                }

                if(p.containsKey("host") && !p.containsKey("data_center_code")) {
                    String host = (String) p.get("host");
                    Entity hostEntity = m.get(host);
                    if(null != hostEntity) {
                        String dataCenterCode = (String)hostEntity.getProperties().get("data_center_code");
                        if(null != dataCenterCode) {
                            e.addProperty("data_center_code", dataCenterCode);
                        }
                    }
                }

                if(null != skipField && e.getFilterProperties().containsKey(skipField)) {
                    // SKIP ( use this for DC vs AWS Distinction as legacy entities do not have skipField set )
                }
                else if(null != baseFilter && baseFilter.size()>0) {
                    for(Map<String,String> f : baseFilter) {
                        if (filter.overlaps(f, e.getFilterProperties())) {
                            m.put(e.getId(), e);
                        }
                    }
                }
                else {
                    m.put(e.getId(), e);
                }
            }
        }

        List<EntityChangeListener> currentListeners = getCurrentListeners();

        Set<String> currentIds = currentMap.keySet();
        Set<String> futureIds = m.keySet();
        Set<String> removedIds = currentIds.stream().filter(x->!futureIds.contains(x)).collect(Collectors.toSet());

        // removing with old entity map still in place, so executed code on same thread sees entities
        for(String k : removedIds) {
            for(EntityChangeListener l : currentListeners) {
                l.notifyEntityRemove(this, currentMap.get(k));
            }
        }
        Set<String> addedIds = futureIds.stream().filter(x->!currentIds.contains(x)).collect(Collectors.toSet());

        currentMap = m;

        // doing this after the switch, so code executed in this thread during notification will see proper entity map
        for(String k : addedIds) {
            for(EntityChangeListener l : currentListeners) {
                l.notifyEntityAdd(this, currentMap.get(k));
            }
        }
    }

    private static final Entity NULL_ENTITY;

    static {
        NULL_ENTITY = new Entity("--NULL--ENTITY--", "--ENTITY--REPO--");
    }

    @Override
    protected Entity getNullObject() {
        return NULL_ENTITY;
    }

    @Autowired
    public EntityRepository(EntityAdapterRegistry registry, SchedulerConfig config) {
        super(registry);

        this.skipField = config.entity_skip_on_field();

        if(config.entity_base_filter()==null && config.entity_base_filter_str()!=null) {
            ObjectMapper m = new ObjectMapper();
            try {
                baseFilter = m.readValue(config.entity_base_filter_str(), new TypeReference<List<Map<String,String>>>() {});
            } catch (IOException e) {
                LOG.error("failed to read string for base config", e);
            }
        }
        else {
            baseFilter = config.entity_base_filter();
        }

        currentMap = new HashMap<>();
        fill();
    }

    public EntityRepository(EntityAdapterRegistry registry) {
        super(registry);

        skipField = null;

        baseFilter = new ArrayList<>();
        currentMap = new HashMap<>();

        fill();
    }
}
