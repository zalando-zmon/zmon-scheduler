package de.zalando.zmon.scheduler.ng;

import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityChangeListener;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;

import java.util.*;

/**
 * Created by jmussler on 18.06.16.
 */
public class DataCenterSubscriber<T> implements EntityChangeListener {

    private final Map<String, List<T>> pendingTasks = new HashMap<>();
    private final Collection<T> emptyList = new ArrayList<>(0);
    private final boolean enabled;

    public DataCenterSubscriber(boolean enabled) {
        this.enabled = enabled;
    }

    public void forwardRequest(T task) {
        if(!enabled) return;

        synchronized (this) {
            for (String k : pendingTasks.keySet()) {
                pendingTasks.get(k).add(task);
            }
        }
    }

    public Collection<T> getRequests(String dcId) {
        synchronized (this) {
            if (!pendingTasks.containsKey(dcId)) {
                return emptyList;
            }

            List<T> tasks = new ArrayList<>(pendingTasks.get(dcId));
            pendingTasks.get(dcId).clear();
            return tasks;
        }
    }

    public Collection<String> getKnwonDCs() {
        synchronized (this) {
            return pendingTasks.keySet();
        }
    }

    @Override
    public void notifyEntityAdd(EntityRepository repo, Entity e) {
        if (e.getFilterProperties().get("type").equals("local")) {
            // local entities depict remote DCs
            if (!pendingTasks.containsKey(e.getId())) {
                synchronized (this) {
                    if (!pendingTasks.containsKey(e.getId())) {
                        pendingTasks.put(e.getId(), new ArrayList<>());
                    }
                }
            }
        }
    }

    @Override
    public void notifyEntityRemove(EntityRepository repo, Entity e) {
        if (e.getFilterProperties().get("type").equals("local")) {
            // local entities depict remote DCs
            if (pendingTasks.containsKey(e.getId())) {
                synchronized (this) {
                    if (pendingTasks.containsKey(e.getId())) {
                        pendingTasks.remove(e.getId());
                    }
                }
            }
        }
    }

    @Override
    public void notifyEntityChange(EntityRepository repo, Entity oldEntity, Entity newEntity) {

    }
}
