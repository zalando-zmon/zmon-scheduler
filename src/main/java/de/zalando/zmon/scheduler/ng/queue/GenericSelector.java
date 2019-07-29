package de.zalando.zmon.scheduler.ng.queue;

import de.zalando.zmon.scheduler.ng.Alert;
import de.zalando.zmon.scheduler.ng.Check;
import de.zalando.zmon.scheduler.ng.DefinitionRuntime;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.trailruns.TrialRunRequest;

import java.util.*;

public class GenericSelector implements Selector {
    private SchedulerConfig config;

    public GenericSelector(SchedulerConfig config) {
        this.config = config;
    }

    private Map<String, Object> buildContext(Check check, TrialRunRequest request) {
        Map<String, Object> context = new HashMap<>();
        // Trial Run
        Optional<TrialRunRequest> optionalRequest = Optional.ofNullable(request);
        context.put("trial_run", optionalRequest.isPresent());
        optionalRequest
                .map(req -> req.runtime)
                .map(DefinitionRuntime::name)
                .ifPresent(runtime -> context.put("trial_run_runtime", runtime));

        // Check
        Optional<CheckDefinition> checkDefinition = Optional.ofNullable(check).map(Check::getCheckDefinition);
        checkDefinition
                .map(CheckDefinition::getId)
                .ifPresent(id -> context.put("check_id", id));
        checkDefinition
                .map(CheckDefinition::getSourceUrl)
                .ifPresent(url -> context.put("check_url", url));
        checkDefinition
                .map(CheckDefinition::getRuntime)
                .map(DefinitionRuntime::name)
                .ifPresent(runtime -> context.put("check_runtime", runtime));

        return context;
    }

    @Override
    public String getQueue(Entity entity, Check check, Collection<Alert> alerts, TrialRunRequest request) {
        Map<String, Object> context = buildContext(check, request);

        for (Map.Entry<String, List<Map<String, String>>> entry : config.getGenericQueueMapping().entrySet()) {
            String queue = entry.getKey();
            List<Map<String, String>> queueConditions = entry.getValue();
            for (Map<String, String> condition : queueConditions) {
                if (context.entrySet().containsAll(condition.entrySet())) {
                    return queue;
                }
            }
        }

        return null;
    }
}
