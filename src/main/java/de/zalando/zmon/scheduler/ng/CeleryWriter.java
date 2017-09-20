package de.zalando.zmon.scheduler.ng;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapInjectAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
        public byte[] asCeleryTask(CeleryBody task, Tracer tracer) {
            final ObjectNode node = mapper.createObjectNode();
            ObjectNode properties = node.putObject("properties");
            properties.put("body_encoding", "nested");


            try {
                Map<String, String> map = getSpanData(tracer);
                properties.set("trace", mapper.valueToTree(map));
            } catch (JsonProcessingException e) {
                LOG.error("Preparing a trace failed: {}", e.toString());
            }

            try {
                return mapper.writeValueAsString(node).getBytes();
            } catch (JsonProcessingException e) {
                LOG.error("Serialize failed for task {}: {}", task, e.getMessage());
                return null;
            }
        }

        private Map<String, String> getSpanData(Tracer tracer) throws JsonProcessingException {
            Tracer.SpanBuilder spanBuilder = tracer.buildSpan("queue_processing")
                    .asChildOf(tracer.activeSpan());
            Span span = spanBuilder.startManual();
            Map<String, String> map = new HashMap<>();
            TextMapInjectAdapter textMap = new TextMapInjectAdapter(map);
            tracer.inject(span.context(), Format.Builtin.TEXT_MAP, textMap);

            return map;
        }
    }

    private static class CompressedNestedWriter extends PlainWriter {
        @Override
        public byte[] asCeleryTask(CeleryBody task, Tracer tracer) {
            try {
                byte[] result = super.asCeleryTask(task, tracer);
                if (result == null) {
                    return null;
                }

                return Snappy.compress(result);
            } catch (IOException ex) {
                LOG.error("Compression failed", ex);
                return null;
            }
        }
    }

    public abstract byte[] asCeleryTask(CeleryBody task, Tracer tracer);
}
