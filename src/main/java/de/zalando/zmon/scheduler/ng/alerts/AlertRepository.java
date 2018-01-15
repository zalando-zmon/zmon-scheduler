package de.zalando.zmon.scheduler.ng.alerts;

import de.zalando.zmon.scheduler.ng.CachedRepository;

import io.opentracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jmussler on 4/7/15.
 */
@Component
public class AlertRepository extends CachedRepository<Integer, AlertSourceRegistry, AlertDefinition> {

    private Map<Integer, List<AlertDefinition>> byCheckId;

    private final Set<AlertChangeListener> changeListeners = new HashSet<>();

    public void registerChangeListener(AlertChangeListener listener) {
        changeListeners.add(listener);
    }

    @Override
    public synchronized void fill() {
        Map<Integer, AlertDefinition> m = new HashMap<>();
        Map<Integer, List<AlertDefinition>> newByCheckId = new HashMap<>();

        for (String name : registry.getSourceNames()) {
            Collection<AlertDefinition> alerts = registry.get(name).getCollection();
            if (null == alerts) {
                continue;
            }

            for (AlertDefinition ad : alerts) {
                m.put(ad.getId(), ad);
                if (newByCheckId.containsKey(ad.getCheckDefinitionId())) {
                    newByCheckId.get(ad.getCheckDefinitionId()).add(ad);
                } else {
                    List<AlertDefinition> ads = new ArrayList<>(1);
                    ads.add(ad);
                    newByCheckId.put(ad.getCheckDefinitionId(), ads);
                }
            }
        }

        List<AlertDefinition> changedAlerts = new ArrayList<>();
        List<AlertDefinition> deletedAlerts = new ArrayList<>();
        List<AlertDefinition> addedAlerts = new ArrayList<>();

        for (Map.Entry<Integer, AlertDefinition> e : m.entrySet()) {
            if (currentMap.containsKey(e.getKey())) {
                if (currentMap.get(e.getKey()).compareForAlertUpdate(e.getValue())) {
                    changedAlerts.add(e.getValue());
                }
            } else {
                addedAlerts.add(e.getValue());
            }
        }

        for (Map.Entry<Integer, AlertDefinition> e : currentMap.entrySet()) {
            if (!m.containsKey(e.getKey())) {
                deletedAlerts.add(e.getValue());
            }
        }

        byCheckId = newByCheckId;
        currentMap = m;

        // we notify after update with the new state
        // main purpose is now delayed cleanup of alert filter changes
        for (AlertChangeListener l : changeListeners) {
            for (AlertDefinition ad : changedAlerts) {
                l.notifyAlertChange(ad);
            }
        }
    }

    @Override
    protected AlertDefinition getNullObject() {
        return NULL_OBJ;
    }

    private static final AlertDefinition NULL_OBJ;

    static {
        NULL_OBJ = new AlertDefinition();
        NULL_OBJ.setCheckDefinitionId(0);
        NULL_OBJ.setEntities(new ArrayList<>(0));
        NULL_OBJ.setEntitiesExclude(new ArrayList<>(0));
    }

    @Autowired
    public AlertRepository(AlertSourceRegistry registry, Tracer tracer) {
        super(registry, tracer);
        currentMap = new HashMap<>();
        byCheckId = new HashMap<>();
        fill();
    }

    private final static List<AlertDefinition> EMPTY_LIST = new ArrayList<>(0);

    public Collection<AlertDefinition> getByCheckId(Integer id) {
        if (byCheckId.containsKey(id)) {
            return byCheckId.get(id);
        } else {
            return EMPTY_LIST;
        }
    }

    public Map<Integer, List<AlertDefinition>> getByCheckId() {
        return byCheckId;
    }
}
