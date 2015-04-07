package de.zalando.zmon.scheduler.ng.entities;

import de.zalando.zmon.scheduler.ng.BaseSource;
import de.zalando.zmon.scheduler.ng.Source;
import de.zalando.zmon.scheduler.ng.SourceRegistry;

import java.util.List;

/**
 * Created by jmussler on 3/27/15.
 */
public abstract class EntityAdapter extends BaseSource<Entity> {
    public EntityAdapter(String name) {
        super(name);
    }
}
