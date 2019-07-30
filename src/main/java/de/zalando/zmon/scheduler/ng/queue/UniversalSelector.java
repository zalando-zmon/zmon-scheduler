package de.zalando.zmon.scheduler.ng.queue;

import de.zalando.zmon.scheduler.ng.Alert;
import de.zalando.zmon.scheduler.ng.Check;
import de.zalando.zmon.scheduler.ng.DefinitionRuntime;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.trailruns.TrialRunRequest;

import java.util.*;

/**
 * Maps check/trial runs to specific queues based on their attributes.
 *
 * Available attributes:
 * - trial_run (boolean - if execution is trial run or not),
 * - trial_run_runtime/check_runtime (String - execution worker version/runtime: PYTHON_2 or PYTHON_3),
 * - check_id (int - check's id),
 * - check_url (String - check's source url).
 *
 * Mapping is specified with {@link SchedulerConfig#setUniversalQueueMapping}.
 * Example (format):
 * <code>
 *   zmon:queue:python3:
 *     - check_runtime: PYTHON_3
 *     - check_id: 5
 *   zmon:queue:arbitrary:
 *     - check_id: 3
 *       check_url: https://google.com
 * </code>
 * Mentioned configuration translates to:
 * - whenever check's runtime is Python 3 OR check's id is 5 schedule it to zmon:queue:python3 queue,
 * - whenever check's id is 3 AND its source url is https://google.com schedule it to zmon:queue:arbitrary queue.
 */
public class UniversalSelector implements Selector {
    private SchedulerConfig config;

    public UniversalSelector(SchedulerConfig config) {
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

        for (Map.Entry<String, List<Map<String, Object>>> entry : config.getUniversalQueueMapping().entrySet()) {
            String queue = entry.getKey();
            List<Map<String, Object>> queueConditions = entry.getValue();
            for (Map<String, Object> condition : queueConditions) {
                if (context.entrySet().containsAll(condition.entrySet())) {
                    return queue;
                }
            }
        }

        return null;
    }
}
