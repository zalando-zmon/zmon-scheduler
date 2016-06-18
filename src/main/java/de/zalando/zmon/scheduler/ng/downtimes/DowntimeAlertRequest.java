package de.zalando.zmon.scheduler.ng.downtimes;

import java.util.List;

/**
 * Created by jmussler on 18.06.16.
 */
public class DowntimeAlertRequest {
    private int alertId;
    private List<String> entityIds;

    public DowntimeAlertRequest() {
    }

    public int getAlertId() {
        return alertId;
    }

    public void setAlertId(int alertId) {
        this.alertId = alertId;
    }

    public List<String> getEntityIds() {
        return entityIds;
    }

    public void setEntityIds(List<String> entityIds) {
        this.entityIds = entityIds;
    }
}
