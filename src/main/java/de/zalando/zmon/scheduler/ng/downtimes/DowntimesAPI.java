package de.zalando.zmon.scheduler.ng.downtimes;

import de.zalando.zmon.scheduler.ng.Scheduler;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Created by jmussler on 18.06.16.
 */
@RestController
public class DowntimesAPI {

    @Autowired
    Scheduler scheduler;

    @Autowired
    AlertRepository alertRepo;

    @Autowired
    DowntimeService downtimeService;

    @Autowired
    DowntimeForwarder downtimeForwarder;

    @RequestMapping(value = "/api/v1/downtimes/{dc}/")
    Collection<DowntimeForwardTask> getPendingDowntimes(@PathVariable(value = "dc") String dcId) {
        return downtimeForwarder.getRequests(dcId);
    }

    @RequestMapping(value = "/api/v1/downtimes")
    Collection<String> getKnownDowntimeForwardDCs() {
        return downtimeForwarder.getKnwonDCs();
    }

    @RequestMapping(value = "/api/v1/downtimes", method = RequestMethod.POST)
    DowntimeRequestResult postDowntime(@RequestBody DowntimeRequest request) {
        DowntimeRequestResult result = downtimeService.storeDowntime(request);

        // trigger evaluation locally
        for(DowntimeAlertRequest r : request.getDowntimeEntities()) {
            scheduler.executeImmediate(alertRepo.get(r.getAlertId()).getCheckDefinitionId());
        }
        downtimeForwarder.forwardRequest(DowntimeForwardTask.NewDowntimeTask(request));

        return result;
    }

    @RequestMapping(value = "/api/v1/downtimes/{id}", method = RequestMethod.DELETE)
    void deleteDowntime(@PathVariable(value = "id") String id) {
        Set<String> ids = new TreeSet<>();
        ids.add(id);
        downtimeService.deleteDowntimes(ids);
        downtimeForwarder.forwardRequest(DowntimeForwardTask.DeleteDowntimeTask(ids));
    }

    @RequestMapping(value = "/api/v1/downtime-groups/{groupdId}", method = RequestMethod.DELETE)
    void deleteDowntimeGroup(@PathVariable(value = "id") String groupId) {
        downtimeService.deleteDowntimeGroup(groupId);
        downtimeForwarder.forwardRequest(DowntimeForwardTask.DeleteGroupTask(groupId));
    }
}
