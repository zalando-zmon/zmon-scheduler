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
public class TrialRunForwarder implements EntityChangeListener {

    private final Map<String, List<TrialRunRequest>> pendingTrialRuns = new HashMap<>();

    public void forwardRequest(TrialRunRequest trialRunRequest) {
        synchronized(this) {
            for(String k : pendingTrialRuns.keySet()) {
                pendingTrialRuns.get(k).add(trialRunRequest);
            }
        }
    }

    public List<TrialRunRequest> getRequests(String dcId) {
        synchronized(this) {
            List<TrialRunRequest> trialRuns = new ArrayList<>(pendingTrialRuns.get(dcId));
            pendingTrialRuns.get(dcId).clear();
            return trialRuns;
        }
    }

    public Collection<String> getKnwonDCs() {
        synchronized (this) {
            return pendingTrialRuns.keySet();
        }
    }

    @Override
    public void notifyEntityAdd(EntityRepository repo, Entity e) {
        if(e.getFilterProperties().get("type").equals("local")) {
            // local entities depict remote DCs
            if(!pendingTrialRuns.containsKey(e.getId())) {
                synchronized(this) {
                    if(!pendingTrialRuns.containsKey(e.getId())) {
                        pendingTrialRuns.put(e.getId(), new ArrayList<>());
                    }
                }
            }
        }
    }

    @Override
    public void notifyEntityRemove(EntityRepository repo, Entity e) {
        if(e.getFilterProperties().get("type").equals("local")) {
            // local entities depict remote DCs
            if(!pendingTrialRuns.containsKey(e.getId())) {
                synchronized(this) {
                    if(!pendingTrialRuns.containsKey(e.getId())) {
                        pendingTrialRuns.remove(e.getId());
                    }
                }
            }
        }

    }

    @Override
    public void notifyEntityChange(EntityRepository repo, Entity e) {

    }
}
