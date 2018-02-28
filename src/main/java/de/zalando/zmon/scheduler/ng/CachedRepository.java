package de.zalando.zmon.scheduler.ng;

import io.opentracing.Scope;
import io.opentracing.Tracer;
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
    protected final S registry;
    protected final Tracer tracer;
    protected static final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    private long lastUpdated = 0;

    public CachedRepository(S r, Tracer tracer) {
        assert (null != r);
        this.registry = r;
        this.tracer = tracer;

        LOG.info("starting scheduler for {}", registry.getClass().getName());
        executor.scheduleAtFixedRate(this, 20, 60, TimeUnit.SECONDS);
    }

    protected abstract T getNullObject();

    abstract public void fill();

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void run() {
        String operationName = String.format("repository_update_%s", registry.getClass().getSimpleName());
        try (Scope scope = tracer.buildSpan(operationName).startActive(true)) {
            LOG.info("scheduling update of: {}", registry.getClass());
            fill();
            lastUpdated = System.currentTimeMillis();
        } catch (Throwable e) {
            LOG.error("Error during refresh of {}", registry.getClass(), e);
        }
    }

    public T get(I id) {
        T v = currentMap.get(id);
        if (null == v) {
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
