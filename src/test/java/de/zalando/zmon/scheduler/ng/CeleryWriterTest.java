package de.zalando.zmon.scheduler.ng;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentracing.NoopTracerFactory;
import org.junit.Test;
import org.xerial.snappy.Snappy;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class CeleryWriterTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void plainWriterProducesJSONWithBodyAndTrace() throws IOException {
        CeleryWriter writer = CeleryWriter.create(TaskSerializerType.PLAIN);
        byte[] message = writer.asCeleryTask(new CeleryBody(), NoopTracerFactory.create());
        ObjectNode node = mapper.readValue(message, ObjectNode.class);

        assertEquals(true, node.has("body"));
        assertEquals(true, node.has("properties"));
        assertEquals(true, node.get("properties").has("trace"));
    }


    @Test
    public void plainWriterProducesJSONWithoutTraceIfNoTracerProvided() throws IOException {
        CeleryWriter writer = CeleryWriter.create(TaskSerializerType.PLAIN);
        byte[] message = writer.asCeleryTask(new CeleryBody(), null);
        ObjectNode node = mapper.readValue(message, ObjectNode.class);

        assertEquals(true, node.has("body"));
        assertEquals(true, node.has("properties"));
        assertEquals(false, node.get("properties").has("trace"));
    }

    @Test
    public void compressedWriterProducesJSONWithBodyAndTrace() throws IOException {
        CeleryWriter writer = CeleryWriter.create(TaskSerializerType.COMPRESSED);
        byte[] encoded = writer.asCeleryTask(new CeleryBody(), NoopTracerFactory.create());
        byte[] message = Snappy.uncompress(encoded);

        ObjectNode node = mapper.readValue(message, ObjectNode.class);

        assertEquals(true, node.has("body"));
        assertEquals(true, node.has("properties"));
        assertEquals(true, node.get("properties").has("trace"));
    }

    @Test
    public void compressedWriterProducesJSONWithoutTraceIfNoTracerProvided() throws IOException {
        CeleryWriter writer = CeleryWriter.create(TaskSerializerType.COMPRESSED);
        byte[] encoded = writer.asCeleryTask(new CeleryBody(), null);
        byte[] message = Snappy.uncompress(encoded);

        ObjectNode node = mapper.readValue(message, ObjectNode.class);

        assertEquals(true, node.has("body"));
        assertEquals(true, node.has("properties"));
        assertEquals(false, node.get("properties").has("trace"));
    }
}
