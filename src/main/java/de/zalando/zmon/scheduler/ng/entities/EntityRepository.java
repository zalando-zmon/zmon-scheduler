package de.zalando.zmon.scheduler.ng.entities;

import de.zalando.zmon.scheduler.ng.CachedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jmussler on 4/7/15.
 */
@Component
public class EntityRepository extends CachedRepository<String, EntityAdapterRegistry, Entity> {

    private void fill() {
        Map<String, Entity> m = new HashMap<>();

        for(String name : registry.getSourceNames()) {
            for(Entity e: registry.get(name).getCollection()) {
                m.put(e.getId(), e);
            }
        }

        currentMap = m;
    }

    @Autowired
    public EntityRepository(EntityAdapterRegistry registry) {
        super(registry);
        currentMap = new HashMap<>();
        fill();
    }
}
