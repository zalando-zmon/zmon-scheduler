package de.zalando.zmon.scheduler.ng.downtimes;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jmussler on 27.06.16.
 */
public class DowntimeRequestResult {
    public String groupId;
    public Map<String, String> ids = new HashMap<>();

    public DowntimeRequestResult() {
    }

    public DowntimeRequestResult(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Map<String, String> getIds() {
        return ids;
    }

    public void setIds(Map<String, String> ids) {
        this.ids = ids;
    }
}
