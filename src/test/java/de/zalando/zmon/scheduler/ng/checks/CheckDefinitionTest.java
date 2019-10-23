package de.zalando.zmon.scheduler.ng.checks;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CheckDefinitionTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testJsonParsing() throws IOException {
        String json = "{\n" +
                "    \"id\": 123,\n" +
                "    \"name\": \"SomeName\",\n" +
                "    \"description\": \"Track errors in logs\",\n" +
                "    \"technical_details\": null,\n" +
                "    \"potential_analysis\": null,\n" +
                "    \"potential_impact\": null,\n" +
                "    \"potential_solution\": null,\n" +
                "    \"owning_team\": \"someteam\",\n" +
                "    \"entities\": [\n" +
                "        {\n" +
                "            \"cluster_id\": \"aws:260111690941:eu-central-1:kube-1\",\n" +
                "            \"lifecycle_status\": \"ready\",\n" +
                "            \"type\": \"kubernetes_cluster\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"interval\": 120,\n" +
                "    \"command\": \"check command\",\n" +
                "    \"status\": \"ACTIVE\",\n" +
                "    \"source_url\": null,\n" +
                "    \"last_modified_by\": \"someone\",\n" +
                "    \"last_modified\": 1556539968226,\n" +
                "    \"criticality\": \"important\",\n" +
                "    \"unknown_field\": \"some_value\",\n" +
                "    \"deleted\": false\n" +
                "}";

        CheckDefinition check = mapper.readValue(json, CheckDefinition.class);
        assertEquals("SomeName", check.getName());
    }
}
