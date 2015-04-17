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

    @Override
    protected void fill() {
        Map<String, Entity> m = new HashMap<>();

        for(String name : registry.getSourceNames()) {
            for(Entity e: registry.get(name).getCollection()) {
                m.put(e.getId(), e);
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
    public EntityRepository(EntityAdapterRegistry registry) {
        super(registry);
        currentMap = new HashMap<>();
        fill();
    }
}
