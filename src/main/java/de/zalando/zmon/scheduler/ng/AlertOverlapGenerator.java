package de.zalando.zmon.scheduler.ng;

import de.zalando.zmon.scheduler.ng.alerts.AlertDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jmussler on 20.05.16.
 */
public class AlertOverlapGenerator {

    public static class EntityInfo {
        public String id;

        public EntityInfo(String i) {
            id = i;
        }
    }

    public static class EntityGroup {
        public List<EntityInfo> entities = new ArrayList<>();
        public List<AlertInfo> alerts = new ArrayList<>();
    }

    public static class AlertInfo {
        public String name;
        public int id;

        public AlertInfo(String n, int i) {
            name = n;
            id = i;
        }
    }

    private final EntityRepository entityRepo;
    private final Map<Integer, List<AlertDefinition>> alertRepo;
    private final Map<Integer, AlertDefinition> alertRepoByAlertId;
    private final Map<Integer, CheckDefinition> checkRepo;

    @Autowired
    public AlertOverlapGenerator(EntityRepository entityRepo, Map<Integer, List<AlertDefinition>> alertRepo, Map<Integer, CheckDefinition> checkRepo, Map<Integer, AlertDefinition> alertsById) {
        this.entityRepo = entityRepo;
        this.alertRepo = alertRepo;
        this.checkRepo = checkRepo;
        this.alertRepoByAlertId = alertsById;
    }

    public List<Entity> getFilteredEntities(List<Map<String, String>> filters) {
        Collection<Entity> allEntities = entityRepo.getUnfiltered();
        List<Entity> filteredEntities = new ArrayList<>();

        for (Entity e : allEntities) {
            for (Map<String, String> filter : filters) {
                boolean match = filter(filter, e.getFilterProperties());
                if (match) {
                    filteredEntities.add(e);
                    break;
                }
            }
        }

        return filteredEntities;
    }

    public static boolean filter(Map<String, String> f, Map<String, Object> ps) {
        for (Map.Entry<String, String> entry : f.entrySet()) {
            if (!ps.containsKey(entry.getKey())) {
                return false;
            }

            Object v = ps.get(entry.getKey());
            if (null == v) {
                return false;
            }

            // ignoring collection support for now ( not really in use anymore )
            if (!v.equals(entry.getValue())) {
                return false;
            }
        }

        return true;
    }

    public Map<Entity, Set<Integer>> getOverlaps(List<Map<String, String>> filters) {
        List<Entity> entities = getFilteredEntities(filters);

        Map<Entity, Set<Integer>> alertOverlap = new HashMap<>();

        for (Entity e : entities) {

            Set<Integer> entityAlerts = new TreeSet<>();
            alertOverlap.put(e, entityAlerts);

            for (CheckDefinition cd : checkRepo.values()) {
                boolean matchCheck = false;
                for (Map<String, String> cFilter : cd.getEntities()) {
                    matchCheck |= filter(cFilter, e.getFilterProperties());
                }

                if (matchCheck && alertRepo.containsKey(cd.getId())) {
                    for (AlertDefinition ad : alertRepo.get(cd.getId())) {
                        boolean matchAlert = false || ad.getEntities().isEmpty();
                        boolean matchExclude = false;

                        for (Map<String, String> aFilter : ad.getEntities()) {
                            if (filter(aFilter, e.getFilterProperties())) {
                                matchAlert = true;
                                break;
                            }
                        }

                        if (matchAlert && ad.getEntitiesExclude() != null) {
                            for (Map<String, String> eFilter : ad.getEntitiesExclude()) {
                                if (filter(eFilter, e.getFilterProperties())) {
                                    matchExclude = true;
                                    break;
                                }
                            }
                        }

                        if (matchAlert && !matchExclude) {
                            entityAlerts.add(ad.getId());
                        }
                    }
                }
            }
        }

        return alertOverlap;
    }

    public List<EntityGroup> groupByAlertIds(List<Map<String, String>> filters) {
        Map<Entity, Set<Integer>> alertOverlap = getOverlaps(filters);

        Map<String, Set<Integer>> mapStringToSet = new HashMap<>();
        Map<String, Set<Entity>> entityGroupByAlertIds = new HashMap<>();

        for (Map.Entry<Entity, Set<Integer>> entry : alertOverlap.entrySet()) {
            String setId = entry.getValue().stream().map(x -> "" + x).collect(Collectors.joining(","));
            if (!mapStringToSet.containsKey(setId)) {
                mapStringToSet.put(setId, entry.getValue());
            }

            if (!entityGroupByAlertIds.containsKey(setId)) {
                entityGroupByAlertIds.put(setId, new HashSet<>());
            }

            entityGroupByAlertIds.get(setId).add(entry.getKey());
        }

        List<EntityGroup> groups = new ArrayList<>();
        for (Map.Entry<String, Set<Entity>> entry : entityGroupByAlertIds.entrySet()) {
            EntityGroup g = new EntityGroup();
            for (Integer i : mapStringToSet.get(entry.getKey())) {
                g.alerts.add(new AlertInfo(alertRepoByAlertId.get(i).getName(), i));
            }

            for (Entity e : entry.getValue()) {
                g.entities.add(new EntityInfo(e.getId()));
            }

            groups.add(g);
        }

        return groups;
    }

    public List<EntityGroup> groupByFields(List<Map<String, String>> filters, Set<String> fields) {
        Map<Entity, Set<Integer>> alertOverlap = getOverlaps(filters);
        List<EntityGroup> groups = new ArrayList<>();
        return groups;
    }
}
