package de.zalando.zmon.scheduler.ng.cleanup;

import de.zalando.zmon.scheduler.ng.alerts.AlertDefinition;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.cleanup.AlertChangeCleaner;
import de.zalando.zmon.scheduler.ng.cleanup.EntityChangedCleaner;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityChangeListener;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    Entity simpleEntity = when(mock(Entity.class).getFilterProperties()).thenReturn(simpleEntityProperties).getMock();
    Entity simpleEntityChanged = when(mock(Entity.class).getFilterProperties()).thenReturn(simpleEntityPropertiesChanged).getMock();

    CheckDefinition hostCheck = when(mock(CheckDefinition.class).getEntities()).thenReturn(asList(typeHostFilter)).getMock();

    AlertDefinition hostAlert = when(mock(AlertDefinition.class).getEntities()).thenReturn(asList(typeHostFilter)).getMock();

    @Test
    public void testTrigger() {
        AlertRepository alertRepo = mock(AlertRepository.class);
        CheckRepository checkRepo = mock(CheckRepository.class);
        AlertChangeCleaner alertCleaner = mock(AlertChangeCleaner.class);
        EntityChangedCleaner cleaner = new EntityChangedCleaner(alertRepo, checkRepo, alertCleaner);
        cleaner.no

    }
}
