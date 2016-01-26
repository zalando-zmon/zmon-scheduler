package de.zalando.zmon.scheduler.ng.entities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.zalando.zmon.scheduler.ng.CachedRepository;
import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xerial.snappy.Snappy;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jmussler on 4/7/15.
 */
@Component
public class EntityRepository extends CachedRepository<String, EntityAdapterRegistry, Entity> {

    private static final Logger LOG = LoggerFactory.getLogger(EntityRepository.class);

    private List<Map<String,String>> baseFilter = null;
    private final String skipField;
    private final List<EntityChangeListener> changeListeners = new ArrayList<>();

    // Need this to write globally aware change listener
    private Map<String, Entity> unfilteredEntities;

    private JedisPool redisPool = null;
    private String redis_properties_key = null;

    public synchronized void registerListener(EntityChangeListener l) {
        LOG.info("Registering entity change listener ({}, {})", l.getClass(), currentMap.size());
        Map<String, Entity> m = unfilteredEntities;
        for(String k : m.keySet()) {
            l.notifyEntityAdd(this, m.get(k));
        }
        changeListeners.add(l);
    }

    private synchronized List<EntityChangeListener> getCurrentListeners() {
        return new ArrayList<>(changeListeners);
    }

    private void createAutoCompleteData() {
        if(redisPool == null || redis_properties_key == null || redis_properties_key.equals("")) {
            return;
        }

        try {
            LOG.info("Creating auto complete data for front end");
            Map<String, Map<String, Set<Object>>> typeMap = new HashMap<>();
            for (Entity e : unfilteredEntities.values()) {
                String type = (String) e.getFilterProperties().get("type");
                if (null != type) {
                    Map<String, Set<Object>> typeData = typeMap.get(type);
                    if (null == typeData) {
                        typeData = new HashMap<>();
                        typeMap.put(type, typeData);
                    }

                    for (String k : e.getFilterProperties().keySet()) {
                        Set<Object> values = typeData.get(k);
                        if (null == values) {
                            values = new HashSet<>();
                            typeData.put(k, values);
                        }
                        values.add(e.getFilterProperties().get(k));
                    }
                }
            }

            typeMap.remove("GLOBAL");

            ObjectMapper mapper = new ObjectMapper();
            String v = mapper.writeValueAsString(typeMap);
            Jedis jedis = redisPool.getResource();

            try {
                jedis.set(redis_properties_key.getBytes(), Snappy.compress(v.getBytes("UTF-8")));
            }
            finally {
                redisPool.returnResource(jedis);
            }
            LOG.info("Done writing auto complete data for front end");
        }
        catch(Exception ex) {
            LOG.error("Error during generating auto complete data");
        }
    }

    public Collection<Entity> getUnfiltered() {
        return unfilteredEntities.values();
    }

    @Override
    protected synchronized void fill() {
        Map<String, Entity> m = new HashMap<>();
        Map<String, Entity> mUnfiltered = new HashMap<>();

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

                mUnfiltered.put(e.getId(), e);
            }
        }

        List<EntityChangeListener> currentListeners = getCurrentListeners();

        Set<String> currentIds = unfilteredEntities.keySet();
        Set<String> futureIds = mUnfiltered.keySet();
        Set<String> removedIds = currentIds.stream().filter(x->!futureIds.contains(x)).collect(Collectors.toSet());
        LOG.info("Number of entities removed globaly: {}", removedIds.size());

        // now using unfiltered, thus code should solely rely on passed entity
        for(String k : removedIds) {
            for(EntityChangeListener l : currentListeners) {
                l.notifyEntityRemove(this, unfilteredEntities.get(k));
            }
        }

        Set<String> addedIds = futureIds.stream().filter(x->!currentIds.contains(x)).collect(Collectors.toSet());
        LOG.info("Numberof entities added globaly: {}", addedIds.size());

        currentMap = m;
        unfilteredEntities = mUnfiltered;

        for(String k : addedIds) {
            for(EntityChangeListener l : currentListeners) {
                l.notifyEntityAdd(this, unfilteredEntities.get(k));
            }
        }

        createAutoCompleteData();

        LOG.info("Entity Repository refreshed: {} known filtered entities / {} total", currentMap.size(), unfilteredEntities.size());
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

        if(config.entity_properties_key()!=null && !"".equals(config.entity_properties_key())) {
            this.redisPool = new JedisPool(config.redis_host(), config.redis_port());
            this.redis_properties_key = config.entity_properties_key();
        }

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
        unfilteredEntities = new HashMap<>();
        fill();
    }

    public EntityRepository(EntityAdapterRegistry registry) {
        super(registry);

        skipField = null;

        baseFilter = new ArrayList<>();
        currentMap = new HashMap<>();
        unfilteredEntities = new HashMap<>();

        fill();
    }
}
