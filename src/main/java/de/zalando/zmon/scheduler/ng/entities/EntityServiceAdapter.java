package de.zalando.zmon.scheduler.ng.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jmussler on 4/2/15.
 */
public class EntityServiceAdapter implements EntityAdapter {

    @Override
    public String getName() {
        return "EntityService";
    }

    @Override
    public List<Entity> getEntities() {
        return new ArrayList<>();
    }
}
