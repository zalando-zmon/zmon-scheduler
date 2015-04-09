package de.zalando.zmon.scheduler.ng;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Created by jmussler on 3/31/15.
 */
public class CeleryWriter {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(CeleryWriter.class);

    private final JsonNode EMPTY_NODE = mapper.createObjectNode();

    public String asCeleryTask(CeleryBody task) {
        final ObjectNode node = mapper.createObjectNode();
        node.set("headers", EMPTY_NODE);
        node.put("content-type", "application-json");
        node.put("content-encoding", "utf-8");

        ObjectNode properties = node.putObject("properties");
        properties.put("body_encoding","base64");
        properties.put("correlation_id", task.id);

        ObjectNode deliveryInfo = properties.putObject("delivery_info");
        deliveryInfo.put("priority", 0);
        deliveryInfo.put("routing_key", "default");
        deliveryInfo.put("exchange", "zmon");

        try {
            String bodyString = mapper.writeValueAsString(task);
            String encoded = Base64.getEncoder().encodeToString(bodyString.getBytes(StandardCharsets.UTF_8));
            node.put("body", encoded);

            String result = mapper.writeValueAsString(node);
            return result;
        } catch (JsonProcessingException e) {
            LOG.error("Serialize failed: {}", task);
            return null;
        }
    }
}
