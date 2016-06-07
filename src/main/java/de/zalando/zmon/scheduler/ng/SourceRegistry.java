package de.zalando.zmon.scheduler.ng;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jmussler on 4/7/15.
 */
public class SourceRegistry<T extends Source> {
    private final Map<String, T> registry = new HashMap<>();

    public void register(T source) {
        registry.put(source.getName(), source);
    }

    public T get(String s) {
        return registry.get(s);
    }

    public Collection<String> getSourceNames() {
        return registry.keySet();
    }
}
