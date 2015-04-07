package de.zalando.zmon.scheduler.ng.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
