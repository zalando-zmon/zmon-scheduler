package de.zalando.zmon.scheduler.ng.entities;

import de.zalando.zmon.scheduler.ng.CachedRepository;
import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jmussler on 4/7/15.
 */
@Component
public class EntityRepository extends CachedRepository<String, EntityAdapterRegistry, Entity> {

    private final List<Map<String,String>> baseFilter;

    @Override
    protected void fill() {
        Map<String, Entity> m = new HashMap<>();

        for(String name : registry.getSourceNames()) {
            for(Entity e: registry.get(name).getCollection()) {
                if(baseFilter.size()>0) {
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

        // TODO build cleanup of old entities here, or notify something responsible
        currentMap = m;
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

        baseFilter = config.entity_base_filter();

        currentMap = new HashMap<>();
        fill();
    }

    public EntityRepository(EntityAdapterRegistry registry) {
        super(registry);

        baseFilter = new ArrayList<>();
        currentMap = new HashMap<>();

        fill();
    }
}
