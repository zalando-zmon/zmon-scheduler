package de.zalando.zmon.scheduler.ng.entities;

import de.zalando.zmon.scheduler.ng.AlertOverlapGenerator;
import de.zalando.zmon.scheduler.ng.CachedRepository;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xerial.snappy.Snappy;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jmussler on 4/7/15.
 */
@Component
public class EntityRepository extends CachedRepository<String, EntityAdapterRegistry, Entity> {

    private static final Logger LOG = LoggerFactory.getLogger(EntityRepository.class);

    private List<Map<String, String>> baseFilter = null;
    private final String skipField;
    private final List<EntityChangeListener> changeListeners = new ArrayList<>();

    // Need this to write globally aware change listener
    private Map<String, Entity> unfilteredEntities;

    private final String redisHost;
    private final int redisPort;
    private String redisPropertiesKey = null;

    private final ObjectMapper mapper = new ObjectMapper();

    public EntityRepository(EntityAdapterRegistry registry, Tracer tracer) {
        super(registry, tracer);

        skipField = null;

        baseFilter = new ArrayList<>();
        currentMap = new HashMap<>();
        unfilteredEntities = new HashMap<>();

        redisPort = 0;
        redisHost = null;

        fill();
    }

    @Autowired
    public EntityRepository(EntityAdapterRegistry registry, SchedulerConfig config, Tracer tracer) {
        super(registry, tracer);

        this.skipField = config.getEntitySkipOnField();

        this.redisHost = config.getRedisHost();
        this.redisPort = config.getRedisPort();
        this.redisPropertiesKey = config.getEntityPropertiesKey();

        if (config.getEntityBaseFilter() == null && config.getEntityBaseFilterStr() != null) {
            ObjectMapper m = new ObjectMapper();
            try {
                baseFilter = m.readValue(config.getEntityBaseFilterStr(), new TypeReference<List<Map<String, String>>>() {
                });
            } catch (IOException e) {
                LOG.error("failed to read string for base config", e);
            }
        } else {
            baseFilter = config.getEntityBaseFilter();
        }

        currentMap = new HashMap<>();
        unfilteredEntities = new HashMap<>();
        fill();
    }

    public synchronized void registerListener(EntityChangeListener l) {
        LOG.info("Registering entity change listener: type={} count={}", l.getClass().getCanonicalName(), currentMap.size());
        Map<String, Entity> m = unfilteredEntities;
        for (String k : m.keySet()) {
            l.notifyEntityAdd(this, m.get(k));
        }
        changeListeners.add(l);
    }

    private synchronized List<EntityChangeListener> getCurrentListeners() {
        return new ArrayList<>(changeListeners);
    }

    private void createAutoCompleteData() {
        if (redisPropertiesKey == null || redisPropertiesKey.equals("")) {
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

            String v = mapper.writeValueAsString(typeMap);

            try (Jedis jedis = new Jedis(redisHost, redisPort)) {
                jedis.set(redisPropertiesKey.getBytes(), Snappy.compress(v.getBytes("UTF-8")));
            }

            LOG.info("Done writing auto complete data for front end");
        } catch (Exception ex) {
            LOG.error("Error during generating auto complete data");
        }
    }

    public Collection<Entity> getUnfiltered() {
        return unfilteredEntities.values();
    }

    @Override
    public synchronized void fill() {
        Map<String, Entity> m = new HashMap<>();
        Map<String, Entity> mUnfiltered = new HashMap<>();

        for (String name : registry.getSourceNames()) {
            for (Entity e : registry.get(name).getCollection()) {

                if (null != skipField && e.getFilterProperties().containsKey(skipField)) {
                    // SKIP ( use this for DC vs AWS Distinction as legacy entities do not have skipField set )
                } else if (null != baseFilter && baseFilter.size() > 0) {
                    if (AlertOverlapGenerator.matchAnyFilter(baseFilter, e.getFilterProperties())) {
                        m.put(e.getId(), e);
                    }
                } else {
                    m.put(e.getId(), e);
                }

                mUnfiltered.put(e.getId(), e);
            }
        }

        List<EntityChangeListener> currentListeners = getCurrentListeners();

        Set<String> currentIds = unfilteredEntities.keySet();
        Set<String> futureIds = mUnfiltered.keySet();
        Set<String> removedIds = currentIds.stream().filter(x -> !futureIds.contains(x)).collect(Collectors.toSet());
        Set<String> changedFilterProperties = futureIds.stream().filter(x -> unfilteredEntities.containsKey(x) && !mUnfiltered.get(x).getFilterProperties().equals(unfilteredEntities.get(x).getFilterProperties())).collect(Collectors.toSet());
        Set<String> addedIds = futureIds.stream().filter(x -> !currentIds.contains(x)).collect(Collectors.toSet());

        LOG.info("Number of entities removed globaly: {}", removedIds.size());
        LOG.info("Number of entities added globaly: {}", addedIds.size());
        LOG.info("Number of entities with changed filter properties: {}", changedFilterProperties.size());

        // switch to new entities, so all other code/calls use up to date data
        currentMap = m;
        Map<String, Entity> oldUnfiltered = unfilteredEntities;
        unfilteredEntities = mUnfiltered;

        for (String k : addedIds) {
            for (EntityChangeListener l : currentListeners) {
                l.notifyEntityAdd(this, unfilteredEntities.get(k));
            }
        }

        // now using unfiltered, thus code should solely rely on passed entity
        for (String k : removedIds) {
            for (EntityChangeListener l : currentListeners) {
                l.notifyEntityRemove(this, oldUnfiltered.get(k));
            }
        }

        for (String k : changedFilterProperties) {
            for (EntityChangeListener l : currentListeners) {
                l.notifyEntityChange(this, oldUnfiltered.get(k), unfilteredEntities.get(k));
            }
        }

        createAutoCompleteData();
        LOG.info("Entity Repository refreshed: {} known filtered entities / {} total", currentMap.size(), unfilteredEntities.size());
    }

    private static final Entity NULL_ENTITY;

    static {
        NULL_ENTITY = new Entity("--NULL--ENTITY--");
    }

    @Override
    protected Entity getNullObject() {
        return NULL_ENTITY;
    }
}
