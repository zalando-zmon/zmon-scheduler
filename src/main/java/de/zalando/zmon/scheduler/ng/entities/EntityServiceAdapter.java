package de.zalando.zmon.scheduler.ng.entities;

import de.zalando.zmon.scheduler.ng.BaseSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jmussler on 4/2/15.
 */
public abstract class EntityServiceAdapter extends BaseSource<Entity> {
    public EntityServiceAdapter(String name) {
        super(name);
    }
}
