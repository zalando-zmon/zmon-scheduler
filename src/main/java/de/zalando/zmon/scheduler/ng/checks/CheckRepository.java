package de.zalando.zmon.scheduler.ng.checks;

import de.zalando.zmon.scheduler.ng.CachedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jmussler on 4/7/15.
 */
@Component
public class CheckRepository extends CachedRepository<Integer, CheckSourceRegistry, CheckDefinition> {
    private void fill() {
        Map<Integer, CheckDefinition> m = new HashMap<>();

        for(String name : registry.getSourceNames()) {
            for(CheckDefinition cd: registry.get(name).getCollection()) {
                m.put(cd.getId(), cd);
            }
        }

        currentMap = m;
    }

    @Autowired
    public CheckRepository(CheckSourceRegistry registry) {
        super(registry);
        currentMap = new HashMap<>();
        fill();
    }
}
