package de.zalando.zmon.scheduler.ng.entities.adapters;

import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityAdapter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by jmussler on 4/2/15.
 */
public class EmptyAdapter extends EntityAdapter {
    public EmptyAdapter() {
        super("empty-entity-adapter");
    }

    @Override
    public Collection<Entity> getCollection() {
        return new ArrayList<>(0);
    }
}
