package de.zalando.zmon.scheduler.ng.checks;

/**
 * Created by jmussler on 4/2/15.
 */

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.NONE)
public class CheckDefinitions {

    @XmlElement
    private Long snapshotId;

    @XmlElement
    private List<CheckDefinition> checkDefinitions = new ArrayList<>();

    public Long getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(final Long snapshotId) {
        this.snapshotId = snapshotId;
    }

    public List<CheckDefinition> getCheckDefinitions() {
        return checkDefinitions;
    }

    public void setCheckDefinitions(final List<CheckDefinition> checkDefinitions) {
        this.checkDefinitions = checkDefinitions;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CheckDefinitions [snapshotId=");
        builder.append(snapshotId);
        builder.append(", checkDefinitions=");
        builder.append(checkDefinitions);
        builder.append("]");
        return builder.toString();
    }

}
