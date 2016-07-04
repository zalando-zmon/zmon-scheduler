package de.zalando.zmon.scheduler.ng.cleanup;

import de.zalando.zmon.scheduler.ng.alerts.AlertDefinition;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Created by jmussler on 08.06.16.
 */
public class EntityPropertyChangeTest {

    private final static Map<String, Object> simpleEntityProperties = new HashMap<String , Object>() {{
        put("id",  "entity-1");
        put("type", "host");
        put("application", "zmon-scheduler");
    }};

    private final static Map<String, Object> simpleEntityPropertiesChanged = new HashMap<String , Object>() {{
        put("id",  "entity-1");
        put("type", "application");
        put("application", "zmon-scheduler");
    }};

    private final static Map<String, String> typeHostFilter = new HashMap<String , String>() {{
        put("type", "host");
    }};

    static CheckDefinition hostCheck = when(mock(CheckDefinition.class).getEntities()).thenReturn(asList(typeHostFilter)).getMock();
    static AlertDefinition hostAlert = when(mock(AlertDefinition.class).getEntities()).thenReturn(asList(typeHostFilter)).getMock();

    static AlertRepository alertRepo = mock(AlertRepository.class);
    static CheckRepository checkRepo = mock(CheckRepository.class);
    static EntityRepository entityRepo = mock(EntityRepository.class);

    static AlertChangedCleaner alertCleaner = mock(AlertChangedCleaner.class);
    static EntityChangedCleaner cleaner = new EntityChangedCleaner(alertRepo, checkRepo, alertCleaner);

    static Entity simpleEntity = when(mock(Entity.class).getFilterProperties()).thenReturn(simpleEntityProperties).getMock();
    static Entity simpleEntityChanged = when(mock(Entity.class).getFilterProperties()).thenReturn(simpleEntityPropertiesChanged).getMock();

    @BeforeClass
    public static void setupMocks() {
        when(hostCheck.getId()).thenReturn(1);
        when(hostAlert.getId()).thenReturn(1);
        when(hostAlert.getCheckDefinitionId()).thenReturn(1);

        when(checkRepo.get()).thenReturn(asList(hostCheck));
        when(alertRepo.getByCheckId(anyInt())).thenReturn(asList(hostAlert));
    }

    @Test
    public void testTrigger() {
        reset(alertCleaner);
        cleaner.notifyEntityChangeNoWait(entityRepo, simpleEntity, simpleEntityChanged);
        verify(alertCleaner, timeout(1000).times(1)).notifyAlertChange(hostAlert);
    }

    @Test
    public void testNoTrigger() {
        reset(alertCleaner);
        cleaner.notifyEntityChangeNoWait(entityRepo, simpleEntity, simpleEntity);
        verify(alertCleaner, Mockito.after(1000).atMost(0)).notifyAlertChange(hostAlert);
    }
}
