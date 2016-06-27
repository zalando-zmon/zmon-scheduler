package de.zalando.zmon.scheduler.ng.downtimes;

import java.util.Map;

/**
 * Created by jmussler on 18.06.16.
 */
public class DowntimeAlertRequest {
    private int alertId;

    // Stores entityId -> UUID map, UUID from controller to delete individual downtimes
    private Map<String, String> entityIds;

    public DowntimeAlertRequest() {
    }

    public int getAlertId() {
        return alertId;
    }

    public void setAlertId(int alertId) {
        this.alertId = alertId;
    }

    public Map<String, String> getEntityIds() {
        return entityIds;
    }

    public void setEntityIds(Map<String, String> entityIds) {
        this.entityIds = entityIds;
    }
}
