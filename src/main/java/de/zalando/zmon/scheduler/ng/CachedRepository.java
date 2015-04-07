package de.zalando.zmon.scheduler.ng;

import java.util.Collection;
import java.util.Map;

/**
 * Created by jmussler on 4/7/15.
 */
public abstract class CachedRepository<I, S, T> {
    protected Map<I, T> currentMap;
    protected S registry;

    public CachedRepository(S r) {
        registry = r;
    }

    T get(I id) {
        return currentMap.get(id);
    }

    Collection<T> get() {
        return currentMap.values();
    }
}
