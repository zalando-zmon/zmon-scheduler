package de.zalando.zmon.scheduler.ng.queue;

import de.zalando.zmon.scheduler.ng.Alert;
import de.zalando.zmon.scheduler.ng.Check;
import de.zalando.zmon.scheduler.ng.DefinitionRuntime;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.entities.Entity;

import java.util.*;

public class GenericSelector implements Selector {
    private SchedulerConfig config;

    public GenericSelector(SchedulerConfig config) {
        this.config = config;
    }

    private Map<String, String> buildContext(Check check) {
        Map<String, String> context = new HashMap<>();
        Optional<Check> optionalCheck = Optional.ofNullable(check);
        Optional<CheckDefinition> checkDefinition = optionalCheck.map(Check::getCheckDefinition);

        context.put("trial_run", String.valueOf(!optionalCheck.isPresent()));
        optionalCheck
                .map(Check::getId)
                .map(String::valueOf)
                .ifPresent(id -> context.put("check_id", id));
        checkDefinition
                .map(CheckDefinition::getRuntime)
                .map(DefinitionRuntime::name)
                .ifPresent(runtime -> context.put("check_runtime", runtime));
        checkDefinition
                .map(CheckDefinition::getSourceUrl)
                .ifPresent(url -> context.put("check_url", url));
        // TODO: cover more cases here and eventually replace all the other selectors

        return context;
    }

    @Override
    public String getQueue(Entity entity, Check check, Collection<Alert> alerts) {
        Map<String, String> context = buildContext(check);

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
