package de.zalando.zmon.scheduler.ng.alerts;

import de.zalando.zmon.scheduler.ng.CachedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by jmussler on 4/7/15.
 */
@Component
public class AlertRepository extends CachedRepository<Integer, AlertSourceRegistry, AlertDefinition> {

    private Map<Integer, List<AlertDefinition>> byCheckId;

    private void fill() {
        Map<Integer, AlertDefinition> m = new HashMap<>();
        Map<Integer, List<AlertDefinition>> newByCheckId = new HashMap<>();

        for(String name : registry.getSourceNames()) {
            for(AlertDefinition ad: registry.get(name).getCollection()) {
                m.put(ad.getId(), ad);
                if(newByCheckId.containsKey(ad.getCheckDefinitionId())) {
                    newByCheckId.get(ad.getCheckDefinitionId()).add(ad);
                }
                else {
                    List<AlertDefinition> ads = new ArrayList<>(1);
                    ads.add(ad);
                    newByCheckId.put(ad.getCheckDefinitionId(), ads);
                }
            }
        }

        byCheckId = newByCheckId;
        currentMap = m;
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
    public AlertRepository(AlertSourceRegistry registry) {
        super(registry);
        currentMap = new HashMap<>();
        byCheckId = new HashMap<>();
        fill();
    }

    private final static List<AlertDefinition> EMPTY_LIST = new ArrayList<>(0);

    public Collection<AlertDefinition> getByCheckId(Integer id) {
        if(byCheckId.containsKey(id)) {
            return byCheckId.get(id);
        }
        else {
            return EMPTY_LIST;
        }
    }
}
