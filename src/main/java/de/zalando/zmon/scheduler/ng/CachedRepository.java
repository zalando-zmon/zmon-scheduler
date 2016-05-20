package de.zalando.zmon.scheduler.ng;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by jmussler on 4/7/15.
 */
public abstract class CachedRepository<I, S, T> implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(CachedRepository.class);

    protected Map<I, T> currentMap;
    protected S registry;
    protected static final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    private long lastUpdated = 0;

    public CachedRepository(S r) {
        registry = r;
        executor.scheduleAtFixedRate(this,180,60, TimeUnit.SECONDS);
    }

    protected abstract T getNullObject();

    abstract protected void fill();

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void run() {
        try {
            LOG.info("scheduling update of: {}", registry.getClass());
            fill();
            lastUpdated = System.currentTimeMillis();
        }
        catch(Throwable e) {
            LOG.error("Error during refresh of {}", registry.getClass(), e);
        }
    }

    public T get(I id) {
        T v = currentMap.get(id);
        if(null==v) {
            return getNullObject();
        }
        return v;
    }

    public Collection<T> get() {
        return currentMap.values();
    }

    public Map<I, T> getCurrentMap() {
        return currentMap;
    }
}
