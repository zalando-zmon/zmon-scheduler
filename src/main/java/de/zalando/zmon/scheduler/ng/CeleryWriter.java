package de.zalando.zmon.scheduler.ng;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Created by jmussler on 3/31/15.
 */
public abstract class CeleryWriter {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final JsonNode EMPTY_NODE = mapper.createObjectNode();

    private static final Logger LOG = LoggerFactory.getLogger(CeleryWriter.class);

    public static CeleryWriter create(TaskSerializerType t) {
        switch (t) {
            case COMPRESSED_BODY:
                return new CompressedBodySerializer();
            case COMPRESSED_NESTED:
                return new CompressedNestedWriter();
            case PLAIN:
                return new PlainWriter();
        }
        return new ClassicSerializer();
    }

    private static class ClassicSerializer extends CeleryWriter {
        @Override
        public byte[] asCeleryTask(CeleryBody task) {
            final ObjectNode node = mapper.createObjectNode();
            node.set("headers", EMPTY_NODE);

            ObjectNode properties = node.putObject("properties");
            properties.put("body_encoding", "base64");
            properties.put("correlation_id", task.id);

            ObjectNode deliveryInfo = properties.putObject("delivery_info");
            deliveryInfo.put("priority", 0);
            deliveryInfo.put("routing_key", "default");
            deliveryInfo.put("exchange", "zmon");

            try {
                String bodyString = mapper.writeValueAsString(task);
                String encoded = Base64.getEncoder().encodeToString(bodyString.getBytes(StandardCharsets.UTF_8));
                node.put("body", encoded);

                return mapper.writeValueAsBytes(node);
            } catch (JsonProcessingException e) {
                LOG.error("Serialize failed: {}", task);
                return null;
            }
        }
    }

    private static class CompressedBodySerializer extends CeleryWriter {
        @Override
        public byte[] asCeleryTask(CeleryBody task) {
            final ObjectNode node = mapper.createObjectNode();
            node.set("headers", EMPTY_NODE);

            ObjectNode properties = node.putObject("properties");
            properties.put("body_encoding", "snappy");
            properties.put("correlation_id", task.id);

            ObjectNode deliveryInfo = properties.putObject("delivery_info");
            deliveryInfo.put("priority", 0);
            deliveryInfo.put("routing_key", "default");
            deliveryInfo.put("exchange", "zmon");

            try {
                String bodyBytes = mapper.writeValueAsString(task);
                byte[] compressed = Snappy.compress(bodyBytes.getBytes());
                node.put("body", compressed); // will be written to base64 by jackson

                return mapper.writeValueAsString(node).getBytes();
            } catch (JsonProcessingException e) {
                LOG.error("Serialize failed: {}", task);
                return null;
            } catch (IOException ex) {
                LOG.error("Compression failed", ex);
                return null;
            }
        }
    }

    private static class PlainWriter extends CeleryWriter {
        @Override
        public byte[] asCeleryTask(CeleryBody task) {
            final ObjectNode node = mapper.createObjectNode();
            node.set("headers", EMPTY_NODE);
            ObjectNode properties = node.putObject("properties");
            properties.put("body_encoding", "nested");
            properties.put("correlation_id", task.id);

            ObjectNode deliveryInfo = properties.putObject("delivery_info");
            deliveryInfo.put("priority", 0);
            deliveryInfo.put("routing_key", "default");
            deliveryInfo.put("exchange", "zmon");

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
            node.set("headers", EMPTY_NODE);

            ObjectNode properties = node.putObject("properties");
            properties.put("body_encoding", "nested");
            properties.put("correlation_id", task.id);

            ObjectNode deliveryInfo = properties.putObject("delivery_info");
            deliveryInfo.put("priority", 0);
            deliveryInfo.put("routing_key", "default");
            deliveryInfo.put("exchange", "zmon");

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
