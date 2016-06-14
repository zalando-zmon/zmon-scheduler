package de.zalando.zmon.scheduler.ng.alerts;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


/**
 * Created by jmussler on 08.06.16.
 */
public class AlertPropertiesChangedTest {

    private final static Map<String, String> hostFilterProperties = new HashMap<String , String>() {{
        put("id",  "entity-1");
        put("type", "host");
        put("application", "zmon-scheduler");
    }};

    private final static Map<String, String> applicationFilterProperties = new HashMap<String , String>() {{
        put("id",  "entity-1");
        put("type", "application");
        put("application", "zmon-scheduler");
    }};

    private final static Map<String, String> typeHostFilter = new HashMap<String , String>() {{
        put("type", "host");
    }};

    private final static Map<String, String> typeHostFilterTwo = new HashMap<String , String>() {{
        put("type", "host");
    }};

    private static AlertDefinition emptyAlertFilter = new AlertDefinition();
    private static AlertDefinition emptyAlertFilter2 = new AlertDefinition();
    private static AlertDefinition oneFilter = new AlertDefinition();
    private static AlertDefinition twoFilter = new AlertDefinition();
    private static AlertDefinition twoFilterOther = new AlertDefinition();
    private static AlertDefinition nullFilter = new AlertDefinition();
    private static AlertDefinition hostFilter = new AlertDefinition();
    private static AlertDefinition applicationFilter = new AlertDefinition();

    private static AlertDefinition excludeFilter = new AlertDefinition();
    private static AlertDefinition excludeFilterChanged = new AlertDefinition();
    private static AlertDefinition nullExcludeFilter = new AlertDefinition();


    @BeforeClass
    public static void setupMocks() {
        oneFilter.setEntities(Arrays.asList(typeHostFilter));
        twoFilter.setEntities(Arrays.asList(typeHostFilter, typeHostFilter));
        twoFilterOther.setEntities(Arrays.asList(typeHostFilterTwo, typeHostFilterTwo));
        nullFilter.setEntities(null);

        hostFilter.setEntities(Arrays.asList(hostFilterProperties));
        applicationFilter.setEntities(Arrays.asList(applicationFilterProperties));

        excludeFilter.setEntitiesExclude(Arrays.asList(hostFilterProperties));
        excludeFilterChanged.setEntitiesExclude(Arrays.asList(applicationFilterProperties));
        nullExcludeFilter.setEntitiesExclude(null);
    }

    @Test
    public void testEntitiesFilterEmpty() {
        assertEquals(false, emptyAlertFilter.compareForAlertUpdate(emptyAlertFilter2));
    }

    @Test
    public void testTwoNonEmpty() {
        assertEquals(true, oneFilter.compareForAlertUpdate(twoFilter));
        assertEquals(true, twoFilter.compareForAlertUpdate(oneFilter));
        assertEquals(false, twoFilter.compareForAlertUpdate(twoFilter));
    }

    @Test
    public void testNullTrue() {
        assertEquals(true, nullFilter.compareForAlertUpdate(twoFilter));
        assertEquals(true, twoFilter.compareForAlertUpdate(nullFilter));
    }

    @Test
    public void testFilterPropertiesChange() {
        assertEquals(true, hostFilter.compareForAlertUpdate(applicationFilter));
    }

    @Test
    public void testFilterRemoveChange() {
        assertEquals(true, twoFilter.compareForAlertUpdate(oneFilter));
    }

    @Test
    public void testNoChange() {
        assertEquals(false, twoFilterOther.compareForAlertUpdate(twoFilter));
    }

    @Test
    public void testExcludeFilters() {
        assertEquals(false, excludeFilter.compareForAlertUpdate(excludeFilter));
        assertEquals(true, excludeFilter.compareForAlertUpdate(excludeFilterChanged));
        assertEquals(true, excludeFilter.compareForAlertUpdate(nullExcludeFilter));
        assertEquals(true, nullExcludeFilter.compareForAlertUpdate(excludeFilter));
    }
}
