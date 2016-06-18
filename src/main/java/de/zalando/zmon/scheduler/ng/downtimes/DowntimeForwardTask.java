package de.zalando.zmon.scheduler.ng.downtimes;

/**
 * Created by jmussler on 18.06.16.
 */
public class DowntimeForwardTask {

    private DowntimeRequest request;
    private DowntimeTaskType type;
    private String groupId;
    private String id;

    public DowntimeForwardTask() {
    }

    public static DowntimeForwardTask DeleteGroupTask(String groupId) {
        DowntimeForwardTask t = new DowntimeForwardTask();
        t.setType(DowntimeTaskType.DELETE_GROUP);
        t.setGroupId(groupId);
        return t;
    }

    public static DowntimeForwardTask NewDowntimeTask(DowntimeRequest request) {
        DowntimeForwardTask t = new DowntimeForwardTask();
        t.setType(DowntimeTaskType.NEW);
        t.setRequest(request);
        return t;
    }

    public static DowntimeForwardTask DeleteDowntimeTask(String id) {
        DowntimeForwardTask t = new DowntimeForwardTask();
        t.setType(DowntimeTaskType.DELETE);
        t.setId(id);
        return t;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DowntimeRequest getRequest() {
        return request;
    }

    public void setRequest(DowntimeRequest request) {
        this.request = request;
    }

    public DowntimeTaskType getType() {
        return type;
    }

    public void setType(DowntimeTaskType type) {
        this.type = type;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
