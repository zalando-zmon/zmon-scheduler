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
        Map<Integer, List<AlertDefinition>> mCheckId = new HashMap<>();

        for(String name : registry.getSourceNames()) {
            for(AlertDefinition ad: registry.get(name).getCollection()) {
                m.put(ad.getId(), ad);
                if(mCheckId.containsKey(ad.getCheckDefinitionId())) {
                    mCheckId.get(ad.getCheckDefinitionId()).add(ad);
                }
                else {
                    List<AlertDefinition> ads = new ArrayList<>(3);
                    ads.add(ad);
                    mCheckId.put(ad.getCheckDefinitionId(), ads);
                }
            }
        }

        currentMap = m;
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
