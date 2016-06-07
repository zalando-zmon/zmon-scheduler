package de.zalando.zmon.scheduler.ng.entities;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jmussler on 4/20/15.
 */
public class DDSDatabaseAdapter extends EntityAdapter {

    private String url;
    private final MetricRegistry metrics;
    private final Timer timer;

    private static final Logger LOG = LoggerFactory.getLogger(DDSDatabaseAdapter.class);

    public DDSDatabaseAdapter(String url, MetricRegistry metrics) {
        super("DDSDatabaseAdapter");
        this.url = url;
        this.metrics = metrics;
        timer = metrics.timer("entity-adapter.dds.databases");
    }

    private static class BaseEntity extends HashMap<String, Object> {
    }

    private static class BaseEntityList extends ArrayList<BaseEntity> {
    }

    @Override
    public Collection<Entity> getCollection() {
        RestTemplate rt = new RestTemplate();

        Timer.Context tC = timer.time();
        BaseEntityList list = rt.getForObject(url, BaseEntityList.class);
        LOG.info("DDS Database Adapter used: {}ms", tC.stop() / 1000000);

        List<Entity> entities = new ArrayList<>();

        Set<String> seenIds = new HashSet<>();

        for (BaseEntity base : list) {
            List<String> parts = Arrays.asList((String) base.get("name"), (String) base.get("environment"), (String) base.get("role"), (String) base.get("slave_type"));
            String entityId = parts.stream().filter(x -> x != null).collect(Collectors.joining("-"));

            if (seenIds.contains(entityId)) {
                continue;
            }
            seenIds.add(entityId);

            Entity e = new Entity(entityId, "DDSDatabaseAdapter");
            base.put("environment", Environments.getNormalized((String) base.get("environment")));
            base.remove("id");
            if (!base.containsKey("pci")) {
                base.put("pci", "false");
            } else {
                Object o = base.get("pci");
                if (o instanceof Boolean) {
                    if ((Boolean) o) {
                        base.put("pci", "true");
                    } else {
                        base.put("pci", "false");
                    }
                }
            }
            e.addProperties(base);
            entities.add(e);
        }

        return entities;
    }
}
