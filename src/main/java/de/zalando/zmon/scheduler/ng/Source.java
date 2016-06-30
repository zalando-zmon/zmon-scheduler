package de.zalando.zmon.scheduler.ng;

import java.util.Collection;

/**
 * Created by jmussler on 4/7/15.
 */
public interface Source<T> {
    String getName();

    Collection<T> getCollection();
}
