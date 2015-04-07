package de.zalando.zmon.scheduler.ng.entities;

import de.zalando.zmon.scheduler.ng.Source;
import de.zalando.zmon.scheduler.ng.SourceRegistry;

import java.util.List;

/**
 * Created by jmussler on 3/27/15.
 */
public interface EntityAdapter extends Source {
    List<Entity> getEntities();
}
