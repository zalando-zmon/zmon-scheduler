package de.zalando.zmon.scheduler.ng.queue;

import de.zalando.zmon.scheduler.ng.Check;
import de.zalando.zmon.scheduler.ng.CommandSerializer;
import de.zalando.zmon.scheduler.ng.DefinitionRuntime;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.trailruns.TrialRunRequest;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

public class QueueSelectorTest {
    private SchedulerConfig config;
    private QueueWriter mockWriter;
    private QueueSelector queueSelector;
    private Entity entity = new Entity("42");

    @Before
    public void setUp() {
        config = new SchedulerConfig();
        config.setDefaultQueue("default_queue");
        config.setTrialRunQueue("default_trial_run_queue");
        mockWriter = mock(QueueWriter.class);
        CommandSerializer mockSerializer = mock(CommandSerializer.class);

        queueSelector = new QueueSelector(mockWriter, config, mockSerializer);
    }

    @Test
    public void executeTrialRunWritesToDefaultQueueWithNoMappings() {
        TrialRunRequest request = new TrialRunRequest();

        queueSelector.executeTrialRun(entity, request);

        verify(mockWriter, times(1)).exec(eq("default_trial_run_queue"), any());
    }

    @Test
    public void executeWritesToDefaultQueueWithNoMappings() {
        Check check = mock(Check.class);

        queueSelector.execute(entity, check, null, 0);

        verify(mockWriter, times(1)).exec(eq("default_queue"), any());
    }

    @Test
    public void executeWritesToQueueMappedWithGenericSelector() {
        config.setUniversalQueueMapping(new HashMap<String, List<Map<String, Object>>>() {{
            put("zmon:python_3", Collections.singletonList(
                    new HashMap<String, Object>() {{
                        put("check_runtime", "PYTHON_3");
                    }}
            ));
        }});
        Check check = mock(Check.class);
        when(check.getCheckDefinition()).thenReturn(new CheckDefinition() {{
            setRuntime(DefinitionRuntime.PYTHON_3);
        }});

        queueSelector.execute(entity, check, null, 0);

        verify(mockWriter, times(1)).exec(eq("zmon:python_3"), any());
    }
}
