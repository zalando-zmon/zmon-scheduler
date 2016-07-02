package de.zalando.zmon.scheduler.ng.entities;

import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

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

        EntityRepository repository = new EntityRepository(registry, config);

        assertEquals(1, repository.getCurrentMap().size());
    }
}
