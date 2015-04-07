package de.zalando.zmon.scheduler.ng;

/**
 * Created by jmussler on 4/7/15.
 */
public abstract class BaseSource<T> implements Source<T> {
    private final int refreshCycle;
    private final String name;

    public BaseSource(String n) {
        name = n;
        refreshCycle = 300;
    }

    public BaseSource(String n, int refresh) {
        name = n;
        refreshCycle = refresh;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getRefreshCycle() {
        return refreshCycle;
    }
}
