package de.zalando.zmon.scheduler.ng.downtimes;

import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Created by jmussler on 18.06.16.
 */
@RestController
public class DowntimesAPI {

    private final Logger log = LoggerFactory.getLogger(DowntimesAPI.class);

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
        downtimeForwarder.forwardRequest(DowntimeForwardTask.NewDowntimeTask(request));

        // trigger evaluation locally
        for (DowntimeAlertRequest r : request.getDowntimeEntities()) {
            scheduler.executeImmediate(alertRepo.get(r.getAlertId()).getCheckDefinitionId());
        }

        return result;
    }

    @RequestMapping(value = "/api/v1/downtimes/{id}", method = RequestMethod.DELETE)
    void deleteDowntime(@PathVariable(value = "id") String id) {
        log.info("Deleting downtime: id={}", id);
        Set<String> ids = new TreeSet<>();
        ids.add(id);
        downtimeForwarder.forwardRequest(DowntimeForwardTask.DeleteDowntimeTask(ids));
        downtimeService.deleteDowntimes(ids);
    }

    @RequestMapping(value = "/api/v1/downtime-groups/{groupId}", method = RequestMethod.DELETE)
    void deleteDowntimeGroup(@PathVariable(value = "groupId") String groupId) {
        log.info("Deleting downtime-group: groupId={}", groupId);
        downtimeForwarder.forwardRequest(DowntimeForwardTask.DeleteGroupTask(groupId));
        downtimeService.deleteDowntimeGroup(groupId);
    }
}
