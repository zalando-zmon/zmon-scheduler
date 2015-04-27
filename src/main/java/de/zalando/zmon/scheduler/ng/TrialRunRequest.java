package de.zalando.zmon.scheduler.ng;

import de.zalando.zmon.scheduler.ng.alerts.Parameter;

import java.util.List;
import java.util.Map;

/**
 * Created by jmussler on 4/27/15.
 */
public class TrialRunRequest {
    public Long interval;
    public String created_by;
    public String name;
    public String id;
    public String check_command;
    public String alert_condition;
    public List<Map<String, String>> entities;
    public List<Map<String, String>> entities_exclude;
    public Map<String, Parameter> parameters;
    public String period;
}
