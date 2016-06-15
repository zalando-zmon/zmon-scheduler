package de.zalando.zmon.scheduler.ng;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import java.io.IOException;

public abstract class CeleryWriter {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(CeleryWriter.class);

    public static CeleryWriter create(TaskSerializerType t) {
        switch (t) {
            case COMPRESSED:
                return new CompressedNestedWriter();
            case PLAIN:
            default:
                return new PlainWriter();
        }
    }

    private static class PlainWriter extends CeleryWriter {
        @Override
        public byte[] asCeleryTask(CeleryBody task) {
            final ObjectNode node = mapper.createObjectNode();
            ObjectNode properties = node.putObject("properties");
            properties.put("body_encoding", "nested");

            try {
                node.putPOJO("body", task);
                return mapper.writeValueAsString(node).getBytes();
            } catch (JsonProcessingException e) {
                LOG.error("Serialize failed: {}", task);
                return null;
            }
        }
    }

    private static class CompressedNestedWriter extends CeleryWriter {
        @Override
        public byte[] asCeleryTask(CeleryBody task) {
            final ObjectNode node = mapper.createObjectNode();
            ObjectNode properties = node.putObject("properties");
            properties.put("body_encoding", "nested");

            try {
                node.putPOJO("body", task);

                byte[] result = mapper.writeValueAsString(node).getBytes();
                byte[] compressed = Snappy.compress(result);

                return compressed;
            } catch (JsonProcessingException e) {
                LOG.error("Serialize failed: {}", task);
                return null;
            } catch (IOException ex) {
                LOG.error("Compression failed", ex);
                return null;
            }
        }
    }

    public abstract byte[] asCeleryTask(CeleryBody task);
}
