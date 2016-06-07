package de.zalando.zmon.scheduler.ng;

/**
 * Created by jmussler on 4/7/15.
 */
public abstract class BaseSource<T> implements Source<T> {
    private final String name;

    public BaseSource(String n) {
        name = n;
    }

    @Override
    public String getName() {
        return name;
    }
}
