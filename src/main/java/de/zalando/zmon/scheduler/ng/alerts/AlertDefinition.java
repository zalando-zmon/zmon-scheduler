package de.zalando.zmon.scheduler.ng.alerts;

/**
 * Created by jmussler on 4/2/15.
 */

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import de.zalando.zmon.scheduler.ng.DefinitionStatus;

@XmlAccessorType(XmlAccessType.NONE)
public class AlertDefinition {

    @XmlElement(required = true)
    private Integer id;

    @XmlElement(required = true)
    private String name;

    @XmlElement
    private String description;

    @XmlElement
    private String team;

    @XmlElement
    private String responsibleTeam;

    /* Map passing
     * JAXB also doesn't support Maps.  It handles Lists great, but Maps are
     * not supported directly. Use of a XmlAdapter to map the maps into beans that JAXB can use.
     */

    private List<Map<String, String>> entities;

    private List<Map<String, String>> entitiesExclude;

    @XmlElement(required = true)
    private String condition;

    @XmlElementWrapper(name = "notifications")
    @XmlElement(name = "notification")
    private List<String> notifications;

    @XmlElement(required = true)
    private Integer checkDefinitionId;

    @XmlElement
    private DefinitionStatus status;

    @XmlElement
    private Integer priority;

    private Date lastModified;

    private String lastModifiedBy;

    @XmlElement
    private String period;

    private Boolean template;

    private Integer parentId;

    @Valid
    private Map<String, Parameter> parameters;

    private List<String> tags;

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

    public String getTeam() {
        return team;
    }

    public void setTeam(final String team) {
        this.team = team;
    }

    public String getResponsibleTeam() {
        return responsibleTeam;
    }

    public void setResponsibleTeam(final String responsibleTeam) {
        this.responsibleTeam = responsibleTeam;
    }

    public List<Map<String, String>> getEntities() {
        return entities;
    }

    public void setEntities(final List<Map<String, String>> entities) {
        this.entities = entities;
    }

    public List<Map<String, String>> getEntitiesExclude() {
        return entitiesExclude;
    }

    public void setEntitiesExclude(final List<Map<String, String>> entitiesExclude) {
        this.entitiesExclude = entitiesExclude;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(final String condition) {
        this.condition = condition;
    }

    public List<String> getNotifications() {
        return notifications;
    }

    public void setNotifications(final List<String> notifications) {
        this.notifications = notifications;
    }

    public Integer getCheckDefinitionId() {
        return checkDefinitionId;
    }

    public void setCheckDefinitionId(final Integer checkDefinitionId) {
        this.checkDefinitionId = checkDefinitionId;
    }

    public DefinitionStatus getStatus() {
        return status;
    }

    public void setStatus(final DefinitionStatus status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(final Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(final String period) {
        this.period = period;
    }

    public Boolean getTemplate() {
        return template;
    }

    public void setTemplate(final Boolean template) {
        this.template = template;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(final Integer parentId) {
        this.parentId = parentId;
    }

    public Map<String, Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(final Map<String, Parameter> parameters) {
        this.parameters = parameters;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(final List<String> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AlertDefinition{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", team='").append(team).append('\'');
        sb.append(", responsibleTeam='").append(responsibleTeam).append('\'');
        sb.append(", entities=").append(entities);
        sb.append(", entitiesExclude=").append(entitiesExclude);
        sb.append(", condition='").append(condition).append('\'');
        sb.append(", notifications=").append(notifications);
        sb.append(", checkDefinitionId=").append(checkDefinitionId);
        sb.append(", status=").append(status);
        sb.append(", priority=").append(priority);
        sb.append(", lastModified=").append(lastModified);
        sb.append(", lastModifiedBy='").append(lastModifiedBy).append('\'');
        sb.append(", period='").append(period).append('\'');
        sb.append(", template=").append(template);
        sb.append(", parentId=").append(parentId);
        sb.append(", parameters=").append(parameters);
        sb.append(", tags=").append(tags);
        sb.append('}');
        return sb.toString();
    }

    public boolean compareForAlertUpdate(AlertDefinition b) {
        if (checkDefinitionId != b.checkDefinitionId) {
            return true;
        }

        if (entities == null && b.entities != null) {
            return true;
        }

        if (entitiesExclude == null && b.entitiesExclude != null) {
            return true;
        }

        // we dont need to overly precise here, triggering cleanup too often, e.g. on filter/property reorder should not happen that often
        if(entities != null && !entities.equals(b.entities)) {
            return true;
        }

        if(entitiesExclude != null && !entitiesExclude.equals(b.entitiesExclude)) {
            return true;
        }

        return false;
    }
}
