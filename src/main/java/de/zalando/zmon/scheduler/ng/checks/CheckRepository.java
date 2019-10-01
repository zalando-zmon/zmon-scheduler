package de.zalando.zmon.scheduler.ng.checks;

import de.zalando.zmon.scheduler.ng.CachedRepository;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;

import io.opentracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by jmussler on 4/7/15.
 */
@Component
public class CheckRepository extends CachedRepository<Integer, CheckSourceRegistry, CheckDefinition> {

    private final Set<CheckChangeListener> listeners = new HashSet<>();
    private final MinIntervalEntityFetcher minIntervalEntityFetcher;

    public synchronized void registerListener(CheckChangeListener l) {
        listeners.add(l);
    }

    private void update(Map<Integer, CheckDefinition> oldMap, Map<Integer, CheckDefinition> newMap) {
        List<Integer> intervalChanged = new ArrayList<>();
        List<Integer> newChecks = new ArrayList<>();
        List<Integer> removedChecks = new ArrayList<>();

        for (Integer id : oldMap.keySet()) {
            if (newMap.containsKey(id)) {
                if (!oldMap.get(id).getInterval().equals(newMap.get(id).getInterval())) {
                    intervalChanged.add(id);
                }
            } else {
                removedChecks.add(id);
            }
        }

        for (Integer id : newMap.keySet()) {
            if (!oldMap.containsKey(id)) {
                newChecks.add(id);
            }
        }

        for (CheckChangeListener l : listeners) {
            for (Integer id : newChecks) {
                l.notifyNewCheck(this, id);
            }

            for (Integer id : removedChecks) {
                l.notifyDeleteCheck(this, id);
            }

            for (Integer id : intervalChanged) {
                l.notifyCheckIntervalChange(this, id);
            }
        }
    }

    @Override
    public synchronized void fill() {
        Map<Integer, CheckDefinition> m = new HashMap<>();

        minIntervalEntityFetcher.fetch();
        MinIntervalEntityFetcher.MinCheckIntervalData minIntervalData = minIntervalEntityFetcher.getCheckInterval();

        for (String name : registry.getSourceNames()) {
            for (CheckDefinition cd : registry.get(name).getCollection()) {
                Integer id = cd.getId();
                Long interval = cd.getInterval();
                if (interval < minIntervalData.getMinCheckInterval()) {
                    if (minIntervalData.getWhitelistedChecks().contains(id)) {
                        if (interval < minIntervalData.getMinWhitelistedCheckInterval()) {
                            cd.setInterval(minIntervalData.getMinWhitelistedCheckInterval());
                        }
                    } else {
                        cd.setInterval(minIntervalData.getMinCheckInterval());
                    }
                }
                m.put(id, cd);
            }
        }
        Map<Integer, CheckDefinition> oldMap = currentMap;
        currentMap = m; // switching here to new map instance before notifications
        update(oldMap, currentMap);
    }

    @Override
    protected CheckDefinition getNullObject() {
        return null;
    }

    @Autowired
    public CheckRepository(CheckSourceRegistry registry, Tracer tracer, MinIntervalEntityFetcher minIntervalEntityFetcher) {
        super(registry, tracer);
        this.currentMap = new HashMap<>();
        this.minIntervalEntityFetcher = minIntervalEntityFetcher;
        fill();
    }
}
