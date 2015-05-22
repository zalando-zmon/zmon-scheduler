package de.zalando.zmon.scheduler.ng;

import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityChangeListener;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by jmussler on 5/22/15.
 */
@Component
public class InstantEvalForwarder implements EntityChangeListener {

    private final Map<String, List<Integer>> pendingTasks = new HashMap<>();

    public void forwardRequest(int checkId) {
        synchronized(this) {
            for(String k : pendingTasks.keySet()) {
                pendingTasks.get(k).add(checkId);
            }
        }
    }

    public List<Integer> getRequests(String dcId) {
        synchronized(this) {
            List<Integer> taskIds = new ArrayList<>(pendingTasks.get(dcId));
            pendingTasks.get(dcId).clear();
            return taskIds;
        }
    }

    public Collection<String> getKnwonDCs() {
        synchronized (this) {
            return pendingTasks.keySet();
        }
    }

    @Override
    public void notifyEntityRemove(EntityRepository repo, Entity e) {
        if(e.getFilterProperties().get("type").equals("local")) {
            // local entities depict remote DCs
            if(!pendingTasks.containsKey(e.getId())) {
                synchronized(this) {
                    if(!pendingTasks.containsKey(e.getId())) {
                        pendingTasks.remove(e.getId());
                    }
                }
            }
        }
    }

    @Override
    public void notifyEntityChange(EntityRepository repo, Entity e) {

    }

    @Override
    public void notifyEntityAdd(EntityRepository repo, Entity e) {
        if(e.getFilterProperties().get("type").equals("local")) {
            // local entities depict remote DCs
            if(!pendingTasks.containsKey(e.getId())) {
                synchronized(this) {
                    if(!pendingTasks.containsKey(e.getId())) {
                        pendingTasks.put(e.getId(), new ArrayList<>());
                    }
                }
            }
        }
    }
}
