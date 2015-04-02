package de.zalando.zmon.scheduler.ng.entities;

import de.zalando.zmon.scheduler.ng.Entity;
import de.zalando.zmon.scheduler.ng.EntityAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jmussler on 4/2/15.
 */
public class EmptyAdapter implements EntityAdapter {
    @Override
    public String getName() {
        return "EmptyAdapter";
    }

    @Override
    public List<Entity> getEntities() {
        return new ArrayList<>(0);
    }
}
