package de.zalando.zmon.scheduler.ng.trailruns;

import de.zalando.zmon.scheduler.ng.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * Created by jmussler on 18.06.16.
 */
@RestController
public class TrialRunAPI {

    @Autowired
    Scheduler scheduler;

    @Autowired
    private TrialRunForwarder trialRunForwarder;

    @RequestMapping(value = "/api/v1/trial-runs/")
    Collection<String> getKnownTrialRunDCs() {
        return trialRunForwarder.getKnwonDCs();
    }

    @RequestMapping(value = "/api/v1/trial-runs/{dc}/")
    Collection<TrialRunRequest> getPendingTrialRuns(@PathVariable(value = "dc") String dcId) {
        return trialRunForwarder.getRequests(dcId);
    }

    @RequestMapping(value = "/api/v1/trial-runs", method = RequestMethod.POST)
    public void postTrialRun(@RequestBody TrialRunRequest trialRun) {
        scheduler.scheduleTrialRun(trialRun);
        trialRunForwarder.forwardRequest(trialRun);
    }
}
