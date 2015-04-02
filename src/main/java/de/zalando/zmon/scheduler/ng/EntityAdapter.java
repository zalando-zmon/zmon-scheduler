package de.zalando.zmon.scheduler.ng;

import java.util.List;

/**
 * Created by jmussler on 3/27/15.
 */
public interface EntityAdapter {
    List<Entity> getEntities();
    String getName();
}
