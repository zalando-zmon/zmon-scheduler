package de.zalando.zmon.scheduler.ng.checks;

/**
 * Created by jmussler on 4/2/15.
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.zalando.zmon.scheduler.ng.DefinitionStatus;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;
import java.util.Map;

// TODO check command encoding
@XmlAccessorType(XmlAccessType.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckDefinition {

    @XmlElement(required = true)
    private Integer id;

    @XmlElement(required = true)
    private String name;

    @XmlElement(required = false)
    private Boolean deleted;

    @XmlElement
    private String description;

    @XmlElement
    private String technicalDetails;

    @XmlElement
    private String potentialAnalysis;

    @XmlElement
    private String potentialImpact;

    @XmlElement
    private String potentialSolution;

    @XmlElement
    private String owningTeam;

    @XmlElement(required = true)
    private List<Map<String, String>> entities;

    @XmlElement(required = true)
    private Long interval;

    @XmlElement(required = true)
    private String command;

    @XmlElement
    private DefinitionStatus status;

    @XmlElement
    private String sourceUrl;

    // setting this to minimum to currently always trigger check refresh until API services this field
    private long lastModified = 0;

    @XmlElement
    private String lastModifiedBy;

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getTechnicalDetails() {
        return technicalDetails;
    }

    public void setTechnicalDetails(final String technicalDetails) {
        this.technicalDetails = technicalDetails;
    }

    public String getPotentialAnalysis() {
        return potentialAnalysis;
    }

    public void setPotentialAnalysis(final String potentialAnalysis) {
        this.potentialAnalysis = potentialAnalysis;
    }

    public String getPotentialImpact() {
        return potentialImpact;
    }

    public void setPotentialImpact(final String potentialImpact) {
        this.potentialImpact = potentialImpact;
    }

    public String getPotentialSolution() {
        return potentialSolution;
    }

    public void setPotentialSolution(final String potentialSolution) {
        this.potentialSolution = potentialSolution;
    }

    public String getOwningTeam() {
        return owningTeam;
    }

    public void setOwningTeam(final String owningTeam) {
        this.owningTeam = owningTeam;
    }

    public List<Map<String, String>> getEntities() {
        return entities;
    }

    public void setEntities(final List<Map<String, String>> entities) {
        this.entities = entities;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(final Long interval) {
        this.interval = interval;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(final String command) {
        this.command = command;
    }

    public DefinitionStatus getStatus() {
        return status;
    }

    public void setStatus(final DefinitionStatus status) {
        this.status = status;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(final String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CheckDefinition [id=");
        builder.append(id);
        builder.append(", name=");
        builder.append(name);
        builder.append(", description=");
        builder.append(description);
        builder.append(", technicalDetails=");
        builder.append(technicalDetails);
        builder.append(", potentialAnalysis=");
        builder.append(potentialAnalysis);
        builder.append(", potentialImpact=");
        builder.append(potentialImpact);
        builder.append(", potentialSolution=");
        builder.append(potentialSolution);
        builder.append(", owningTeam=");
        builder.append(owningTeam);
        builder.append(", entities=");
        builder.append(entities);
        builder.append(", interval=");
        builder.append(interval);
        builder.append(", command=");
        builder.append(command);
        builder.append(", status=");
        builder.append(status);
        builder.append(", sourceUrl=");
        builder.append(sourceUrl);
        builder.append(", lastModifiedBy=");
        builder.append(lastModifiedBy);
        builder.append("]");
        return builder.toString();
    }

    public boolean compareForCheckUpdate(CheckDefinition b) {
        if (entities != null) {
            return entities.equals(b.entities);
        }

        return b.entities != null;

    }

}
