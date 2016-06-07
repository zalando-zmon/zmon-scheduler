package de.zalando.zmon.scheduler.ng.entities;

import de.zalando.zmon.scheduler.ng.BaseSource;

/**
 * Created by jmussler on 3/27/15.
 */
public abstract class EntityAdapter extends BaseSource<Entity> {
    public EntityAdapter(String name) {
        super(name);
    }
}
