package de.zalando.zmon.scheduler.ng.trailruns;

import de.zalando.zmon.scheduler.ng.DefinitionRuntime;
import de.zalando.zmon.scheduler.ng.alerts.Parameter;

import java.util.List;
import java.util.Map;

/**
 * Created by jmussler on 4/27/15.
 */
public class TrialRunRequest {
    public Long interval;
    public String createdBy;
    public String name;
    public String id;
    public String checkCommand;
    public String alertCondition;
    public DefinitionRuntime runtime;
    public List<Map<String, String>> entities;
    public List<Map<String, String>> entitiesExclude;
    public Map<String, Parameter> parameters;
    public String period;
}
