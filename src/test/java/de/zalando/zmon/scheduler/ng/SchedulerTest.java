package de.zalando.zmon.scheduler.ng;

import de.zalando.zmon.scheduler.ng.alerts.AlertDefinition;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import de.zalando.zmon.scheduler.ng.queue.QueueSelector;
import de.zalando.zmon.scheduler.ng.scheduler.Scheduler;
import de.zalando.zmon.scheduler.ng.trailruns.TrialRunRequest;

import com.codahale.metrics.MetricRegistry;
import io.opentracing.NoopTracerFactory;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SchedulerTest {

    @Test
    public void scheduleOne() throws InterruptedException {
        final CheckRepository checkRepo = mock(CheckRepository.class);
        CheckDefinition check = new CheckDefinition();
        check.setId(1);
        check.setInterval(1L);
        Map<String, String> includeFilter = new HashMap<>();
        includeFilter.put("type", "host");
        check.setEntities(asList(includeFilter));
        when(checkRepo.get(1)).thenReturn(check);

        Map<String, String> excludeFilter = new HashMap<>();
        excludeFilter.put("id", "entity-3");

        final EntityRepository entityRepo = mock(EntityRepository.class);
        Entity entity1= new Entity("entity-1");
        entity1.addProperty("type", "host");

        Entity entity2 = new Entity("entity-2");
        entity2.addProperty("type", "instance");

        Entity entity3 = new Entity("entity-3");
        entity3.addProperty("type", "host");

        when(entityRepo.get()).thenReturn(asList(entity1, entity2, entity3));

        final AlertRepository alertRepo = mock(AlertRepository.class);
        AlertDefinition alert = new AlertDefinition();
        alert.setId(2);
        alert.setEntities(asList());
        alert.setEntitiesExclude(asList(excludeFilter));

        when(alertRepo.getByCheckId(check.getId())).thenReturn(asList(alert));
        when(alertRepo.get(alert.getId())).thenReturn(alert);

        QueueSelector queueSelector = mock(QueueSelector.class);
        SchedulerConfig config = new SchedulerConfig();
        MetricRegistry metricRegistry = new MetricRegistry();
        Scheduler scheduler = new Scheduler(alertRepo, checkRepo, entityRepo, queueSelector, config, metricRegistry, NoopTracerFactory.create());

        // now schedule our check
        long beforeSchedule = System.currentTimeMillis();
        scheduler.scheduleCheck(check.getId());

        // verify that our entity was writen to the "queue"
        verify(queueSelector, timeout(2000)).execute(eq(entity1), any(), any(), gt(beforeSchedule));

        // non matching entity not executed
        verify(queueSelector, Mockito.after(2000).never()).execute(eq(entity2), any(), any(), gt(beforeSchedule));

        // excluded entity not executed
        verify(queueSelector, Mockito.after(2000).never()).execute(eq(entity3), any(), any(), gt(beforeSchedule));
    }

    @Test
    public void scheduleTrialRun() {
        final EntityRepository entityRepo = mock(EntityRepository.class);
        Entity entity1 = new Entity("included-entity");
        entity1.addProperty("type", "host");

        Entity entity2 = new Entity("included-entity");
        entity2.addProperty("type", "instance");

        Entity entityExcluded = new Entity("excluded-entity");
        entityExcluded.addProperty("type", "host");
        when(entityRepo.get()).thenReturn(asList(entity1, entity2, entityExcluded));

        QueueSelector queueSelector = mock(QueueSelector.class);
        SchedulerConfig config = new SchedulerConfig();
        MetricRegistry metricRegistry = new MetricRegistry();
        Scheduler scheduler = new Scheduler(null, null, entityRepo, queueSelector, config, metricRegistry, NoopTracerFactory.create());

        TrialRunRequest request = new TrialRunRequest();
        request.id = "test";
        request.interval = 60L;
        Map<String, String> includeFilter = new HashMap<>();
        includeFilter.put("type", "host");
        request.entities = asList(includeFilter);

        Map<String,String> excludeFilter = new HashMap<>();
        excludeFilter.put("id", "excluded-entity");
        request.entitiesExclude = asList(excludeFilter);

        scheduler.scheduleTrialRun(request);

        verify(queueSelector).execute(eq(entity1), any(), eq("zmon:queue:default"));
        verify(queueSelector, never()).execute(eq(entity2), any(), eq("zmon:queue:default"));
        verify(queueSelector, never()).execute(eq(entityExcluded), any(), eq("zmon:queue:default"));
    }

}
