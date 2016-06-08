package de.zalando.zmon.scheduler.ng;

import de.zalando.zmon.scheduler.ng.alerts.AlertDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.entities.Entity;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

/**
 * Created by jmussler on 08.06.16.
 */
@RunWith(MockitoJUnitRunner.class)
public class FilterTest {

    AlertDefinition emptyAlerty = when(mock(AlertDefinition.class).getId()).thenReturn(1).getMock();

    CheckDefinition emptyCheck = when(mock(CheckDefinition.class).getId()).thenReturn(1).getMock();

    private final static Map<String, Object> simpleEntityProperties = new HashMap<String , Object>() {{
        put("id",  "entity-1");
        put("type", "host");
        put("application", "zmon-scheduler");
    }};

    private final static Map<String, String> typeHostFilter = new HashMap<String , String>() {{
        put("type", "host");
    }};

    private final static Map<String, String> typeApplicationFilter = new HashMap<String , String>() {{
        put("type", "application");
    }};

    Entity simpleEntity = when(mock(Entity.class).getFilterProperties()).thenReturn(simpleEntityProperties).getMock();

    CheckDefinition hostCheck = when(mock(CheckDefinition.class).getEntities()).thenReturn(asList(typeHostFilter)).getMock();

    CheckDefinition applicationCheck = when(mock(CheckDefinition.class).getEntities()).thenReturn(asList(typeApplicationFilter)).getMock();

    CheckDefinition bothCheck = when(mock(CheckDefinition.class).getEntities()).thenReturn(asList(typeApplicationFilter, typeHostFilter)).getMock();

    AlertDefinition hostAlert = when(mock(AlertDefinition.class).getEntities()).thenReturn(asList(typeHostFilter)).getMock();

    AlertDefinition applicationAlert = when(mock(AlertDefinition.class).getEntities()).thenReturn(asList(typeApplicationFilter)).getMock();

    AlertDefinition bothAlert = when(mock(AlertDefinition.class).getEntities()).thenReturn(asList(typeApplicationFilter, typeHostFilter)).getMock();

    @Test
    public void testEmptyAlertFilter() {
        boolean matchesEmptyAlert = AlertOverlapGenerator.matchAlertFilter(emptyAlerty, simpleEntity);
        assertEquals(true, matchesEmptyAlert);
    }

    @Test
    public void testEmptyCheck() {
        boolean matchesEmptyCheck = AlertOverlapGenerator.matchCheckFilter(emptyCheck, simpleEntity);
        assertEquals(false, matchesEmptyCheck);
    }

    @Test
    public void testCheckFilterMatch() {
        boolean matchHostCheck = AlertOverlapGenerator.matchCheckFilter(hostCheck, simpleEntity);
        assertEquals(true, matchHostCheck);

        boolean matchApplicationCheck = AlertOverlapGenerator.matchCheckFilter(applicationCheck, simpleEntity);
        assertEquals(false, matchApplicationCheck);

        boolean matchBothCheck = AlertOverlapGenerator.matchCheckFilter(bothCheck, simpleEntity);
        assertEquals(true, matchBothCheck);
    }

    @Test
    public void testAlertFilterMatch() {
        boolean matchHostAlert = AlertOverlapGenerator.matchAlertFilter(hostAlert, simpleEntity);
        assertEquals(true, matchHostAlert);

        boolean matchApplicationAlert = AlertOverlapGenerator.matchAlertFilter(applicationAlert, simpleEntity);
        assertEquals(false, matchApplicationAlert);

        boolean matchBothAlert = AlertOverlapGenerator.matchAlertFilter(bothAlert, simpleEntity);
        assertEquals(true, matchBothAlert);
    }
}
