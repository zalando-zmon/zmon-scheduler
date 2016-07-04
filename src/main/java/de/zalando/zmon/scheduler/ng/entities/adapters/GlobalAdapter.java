package de.zalando.zmon.scheduler.ng.entities.adapters;

import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by jmussler on 4/2/15.
 */
public class GlobalAdapter extends EntityAdapter {
    public GlobalAdapter() {
        super("global-adapter");
    }

    private final Logger LOG = LoggerFactory.getLogger(GlobalAdapter.class);

    private final static Entity GLOBAL_ENTITY = new Entity("GLOBAL", "global-adapter");
    private final static List<Entity> GLOBAL_COLLECTION = new ArrayList<>(1);

    static {
        Map<String, Object> p = new HashMap<>();
        p.put("type", "GLOBAL");
        GLOBAL_ENTITY.addProperties(p);
        GLOBAL_COLLECTION.add(GLOBAL_ENTITY);
    }

    @Override
    public Collection<Entity> getCollection() {
        LOG.info("Returning 1 global entity");
        return GLOBAL_COLLECTION;
    }
}
