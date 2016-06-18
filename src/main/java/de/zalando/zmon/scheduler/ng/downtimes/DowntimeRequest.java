package de.zalando.zmon.scheduler.ng.downtimes;

import java.util.List;

/**
 * Created by jmussler on 18.06.16.
 */
public class DowntimeRequest {
    private String comment;
    private Long startTime;
    private Long endTime;
    private String createdBy;
    private List<DowntimeAlertRequest> downtimeEntities;

    public DowntimeRequest() {
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public List<DowntimeAlertRequest> getDowntimeEntities() {
        return downtimeEntities;
    }

    public void setDowntimeEntities(List<DowntimeAlertRequest> downtimeEntities) {
        this.downtimeEntities = downtimeEntities;
    }
}
