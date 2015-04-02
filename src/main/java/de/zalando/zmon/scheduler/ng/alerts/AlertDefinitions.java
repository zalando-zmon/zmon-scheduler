package de.zalando.zmon.scheduler.ng.alerts;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

/**
 * Created by jmussler on 4/2/15.
 */
@XmlAccessorType(XmlAccessType.NONE)
public class AlertDefinitions {

    @XmlElement
    private Long snapshotId;

    @XmlElement
    private List<AlertDefinition> alertDefinitions;

    public Long getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(final Long snapshotId) {
        this.snapshotId = snapshotId;
    }

    public List<AlertDefinition> getAlertDefinitions() {
        return alertDefinitions;
    }

    public void setAlertDefinitions(final List<AlertDefinition> alertDefinitions) {
        this.alertDefinitions = alertDefinitions;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("AlertDefinitions [snapshotId=");
        builder.append(snapshotId);
        builder.append(", alertDefinitions=");
        builder.append(alertDefinitions);
        builder.append("]");
        return builder.toString();
    }

}
