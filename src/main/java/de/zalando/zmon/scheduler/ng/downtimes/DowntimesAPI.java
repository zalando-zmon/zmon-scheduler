package de.zalando.zmon.scheduler.ng.downtimes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * Created by jmussler on 18.06.16.
 */
@RestController
public class DowntimesAPI {

    @Autowired
    DowntimeForwarder downtimeForwarder;

    @RequestMapping(value = "/api/v1/downtimes/{dc}/")
    Collection<DowntimeRequest> getPendingDowntimes(@PathVariable(value = "dc") String dcId) {
        return downtimeForwarder.getRequests(dcId);
    }

    @RequestMapping(value = "/api/v1/downtimes")
    Collection<String> getKnownDowntimeForwardDCs() {
        return downtimeForwarder.getKnwonDCs();
    }

    @RequestMapping(value = "/api/v1/downtimes", method = RequestMethod.POST)
    void postDowntime(@RequestBody DowntimeRequest request) {
        downtimeForwarder.forwardRequest(request);
    }

}
