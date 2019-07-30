package de.zalando.zmon.scheduler.ng.queue;

import de.zalando.zmon.scheduler.ng.Check;
import de.zalando.zmon.scheduler.ng.DefinitionRuntime;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.trailruns.TrialRunRequest;
import org.junit.Test;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class UniversalSelectorTest {
    private Entity entity = new Entity("42");

    @Test
    public void getQueueReturnsQueueBasedOnCheckRuntimeMapping() {
        SchedulerConfig config = new SchedulerConfig() {{
            setUniversalQueueMapping(new HashMap<String, List<Map<String, Object>>>() {{
                put("zmon:python_3", Collections.singletonList(
                        new HashMap<String, Object>() {{
                            put("check_runtime", "PYTHON_3");
                        }}
                ));
            }});
        }};
        Check checkPython3 = mock(Check.class);
        when(checkPython3.getCheckDefinition()).thenReturn(new CheckDefinition() {{
            setRuntime(DefinitionRuntime.PYTHON_3);
        }});
        Check checkRegular = mock(Check.class);

        UniversalSelector selector = new UniversalSelector(config);
        String queuePython3 = selector.getQueue(entity, checkPython3, null, null);
        String queueRegular = selector.getQueue(entity, checkRegular, null, null);

        assertEquals("zmon:python_3", queuePython3);
        assertNull(queueRegular);
    }

    @Test
    public void getQueueReturnsQueueBasedOnTrialRunRuntimeMapping() {
        SchedulerConfig config = new SchedulerConfig() {{
            setUniversalQueueMapping(new HashMap<String, List<Map<String, Object>>>() {{
                put("zmon:trial_run_python_3", Collections.singletonList(
                        new HashMap<String, Object>() {{
                            put("trial_run_runtime", "PYTHON_3");
                        }}
                ));
            }});
        }};
        TrialRunRequest requestPython3 = new TrialRunRequest() {{
            runtime = DefinitionRuntime.PYTHON_3;
        }};
        TrialRunRequest requestRegular = new TrialRunRequest();

        UniversalSelector selector = new UniversalSelector(config);
        String queuePython3 = selector.getQueue(entity, null, null, requestPython3);
        String queueRegular = selector.getQueue(entity, null, null, requestRegular);

        assertEquals("zmon:trial_run_python_3", queuePython3);
        assertNull(queueRegular);
    }
}
