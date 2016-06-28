package de.zalando.zmon.scheduler.ng.instantevaluations;

import de.zalando.zmon.scheduler.ng.Scheduler;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

/**
 * Created by jmussler on 18.06.16.
 */
@RestController
public class InstantEvaluationAPI {

    @Autowired
    AlertRepository alertRepo;

    @Autowired
    Scheduler scheduler;

    @Autowired
    private InstantEvalForwarder instantEvalForwarder;

    @RequestMapping(value = "/api/v1/instant-evaluations/{dc}/")
    Collection<Integer> getPendingInstantEvaluations(@PathVariable(value = "dc") String dcId) {
        return instantEvalForwarder.getRequests(dcId);
    }

    @RequestMapping(value = "/api/v1/instant-evaluations/")
    Collection<String> getKnownInstantEvalForwardDCs() {
        return instantEvalForwarder.getKnwonDCs();
    }

    @RequestMapping(value = "/api/v1/checks/{id}/instant-eval", method = RequestMethod.POST)
    public void triggerInstantEvaluationByCheck(@PathVariable(value = "id") int checkId) {
        scheduler.executeImmediate(checkId);
        instantEvalForwarder.forwardRequest(checkId);
    }

    @RequestMapping(value = "/api/v1/alerts/{id}/instant-eval", method = RequestMethod.POST)
    public void triggerInstantEvaluation(@PathVariable(value = "id") int id) {
        int checkId = alertRepo.get(id).getCheckDefinitionId();
        scheduler.executeImmediate(checkId);
        instantEvalForwarder.forwardRequest(checkId);
    }
}
