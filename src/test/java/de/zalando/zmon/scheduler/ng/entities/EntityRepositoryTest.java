package de.zalando.zmon.scheduler.ng.entities;

import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;

import io.opentracing.NoopTracerFactory;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jmussler on 02.07.16.
 */
public class EntityRepositoryTest {

    @Test
    public void BaseFilterTest() {
        SchedulerConfig config = new SchedulerConfig();
        config.setEntityBaseFilterStr("[{\"type\":\"host\"}]");

        EntityAdapterRegistry registry = Mockito.mock(EntityAdapterRegistry.class);

        Entity instance = new Entity("instance-1");
        instance.addProperty("type", "instance");

        Entity host = new Entity("host-1");
        host.addProperty("type", "host");

        List<Entity> entities = asList(instance, host);

        EntityAdapter adapter = Mockito.mock(EntityAdapter.class);
        when(adapter.getCollection()).thenReturn(entities);

        when(registry.getSourceNames()).thenReturn(asList("entities"));
        when(registry.get("entities")).thenReturn(adapter);

        EntityRepository repository = new EntityRepository(registry, config, NoopTracerFactory.create());

        assertEquals(1, repository.getCurrentMap().size());
    }

    @Test
    public void TestNoChangeOnException() {
        SchedulerConfig config = new SchedulerConfig();
        config.setEntityBaseFilterStr("[{\"type\":\"host\"}]");

        EntityAdapterRegistry registry = Mockito.mock(EntityAdapterRegistry.class);

        Entity instance = new Entity("instance-1");
        instance.addProperty("type", "instance");

        Entity host = new Entity("host-1");
        host.addProperty("type", "host");

        List<Entity> entities = asList(instance, host);

        EntityAdapter adapter = Mockito.mock(EntityAdapter.class);
        when(adapter.getCollection()).thenReturn(entities).thenThrow(new RuntimeException("Loading of entities failed"));

        when(registry.getSourceNames()).thenReturn(asList("entities"));
        when(registry.get("entities")).thenReturn(adapter);

        EntityRepository repository = new EntityRepository(registry, config, NoopTracerFactory.create());
        assertEquals(1, repository.getCurrentMap().size());

        try {
            repository.fill();
            fail("Exception not thrown");
        }
        catch(Exception t) {
            assertTrue("Exception of unexpected type", t instanceof RuntimeException);
        }

        assertEquals(1, repository.getCurrentMap().size());
    }

    @Test
    public void TestNotify() {
        SchedulerConfig config = new SchedulerConfig();
        config.setEntityBaseFilterStr("[{\"type\":\"host\"}]");

        EntityAdapterRegistry registry = Mockito.mock(EntityAdapterRegistry.class);

        Entity instance = new Entity("instance-1");
        instance.addProperty("type", "instance");

        Entity host1 = new Entity("host-1");
        host1.addProperty("type", "host");

        Entity host1_changed = new Entity("host-1");
        host1_changed.addProperty("type", "host");
        host1_changed.addProperty("traffic", "true");

        Entity host2 = new Entity("host-2");
        host2.addProperty("type", "host");

        Entity host3 = new Entity("host-3");
        host3.addProperty("type", "host");

        List<Entity> entities = asList(instance, host1, host3);
        List<Entity> entities2 = asList(instance, host1_changed, host2);

        EntityAdapter adapter = Mockito.mock(EntityAdapter.class);
        when(adapter.getCollection()).thenReturn(entities).thenReturn(entities2);

        when(registry.getSourceNames()).thenReturn(asList("entities"));
        when(registry.get("entities")).thenReturn(adapter);

        EntityChangeListener listener = Mockito.mock(EntityChangeListener.class);

        EntityRepository repository = new EntityRepository(registry, config, NoopTracerFactory.create());
        repository.registerListener(listener);
        assertEquals(2, repository.getCurrentMap().size());

        repository.fill();
        assertEquals(2, repository.getCurrentMap().size());

        verify(listener).notifyEntityAdd(eq(repository), eq(host2));
        verify(listener).notifyEntityRemove(eq(repository), eq(host3));
        verify(listener).notifyEntityChange(eq(repository), eq(host1), eq(host1_changed));
    }
}
