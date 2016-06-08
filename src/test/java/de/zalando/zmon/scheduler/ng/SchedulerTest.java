package de.zalando.zmon.scheduler.ng;

import com.codahale.metrics.MetricRegistry;
import de.zalando.zmon.scheduler.ng.alerts.AlertDefinition;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class SchedulerTest {

    @Test
    public void scheduleOne() throws InterruptedException {
        final CheckRepository checkRepo = mock(CheckRepository.class);
        CheckDefinition check = new CheckDefinition();
        check.setId(1);
        check.setInterval(1L);
        Map<String, String> includeFilter = new HashMap<>();
        includeFilter.put("id", "myent");
        check.setEntities(asList(includeFilter));
        when(checkRepo.get(1)).thenReturn(check);

        final EntityRepository entityRepo = mock(EntityRepository.class);
        Entity entity = new Entity("myent", "test");
        when(entityRepo.get()).thenReturn(asList(entity));

        final AlertRepository alertRepo = mock(AlertRepository.class);
        AlertDefinition alert = new AlertDefinition();
        alert.setId(2);
        alert.setEntities(asList());
        when(alertRepo.getByCheckId(check.getId())).thenReturn(asList(alert));
        when(alertRepo.get(alert.getId())).thenReturn(alert);

        QueueSelector queueSelector = mock(QueueSelector.class);
        SchedulerConfig config = new SchedulerConfig();
        MetricRegistry metricRegistry = new MetricRegistry();
        Scheduler scheduler = new Scheduler(alertRepo, checkRepo, entityRepo, queueSelector, config, metricRegistry);

        // now schedule our check
        long beforeSchedule = System.currentTimeMillis();
        scheduler.scheduleCheck(check.getId());

        // verify that our entity was writen to the "queue"
        verify(queueSelector, timeout(2000)).execute(eq(entity), any(), any(), gt(beforeSchedule));
    }

    @Test
    public void scheduleTrialRun() {
        final EntityRepository entityRepo = mock(EntityRepository.class);
        Entity entity = new Entity("myent", "test");
        when(entityRepo.get()).thenReturn(asList(entity));

        QueueSelector queueSelector = mock(QueueSelector.class);
        SchedulerConfig config = new SchedulerConfig();
        MetricRegistry metricRegistry = new MetricRegistry();
        Scheduler scheduler = new Scheduler(null, null, entityRepo, queueSelector, config, metricRegistry);

        TrialRunRequest request = new TrialRunRequest();
        request.id = "test";
        request.interval = 60L;
        Map<String, String> includeFilter = new HashMap<>();
        includeFilter.put("id", "myent");
        request.entities = asList(includeFilter);
        scheduler.scheduleTrialRun(request);

        verify(queueSelector).execute(any(), eq("zmon:queue:default"), eq(entity));
    }

}
