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
    private static final String TRACE_OPERATION_NAME = "queue_processing";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(CeleryWriter.class);

    protected final Tracer tracer;

    public static CeleryWriter create(TaskSerializerType t, Tracer tracer) {
        switch (t) {
            case COMPRESSED:
                return new CompressedNestedWriter(tracer);
            case PLAIN:
            default:
                return new PlainWriter(tracer);
        }
    }

    private CeleryWriter(Tracer tracer) {
        this.tracer = tracer;
    }

    private static class PlainWriter extends CeleryWriter {
        private PlainWriter(final Tracer tracer) {
            super(tracer);
        }

        @Override
        public byte[] asCeleryTask(CeleryBody task) {
            final ObjectNode node = mapper.createObjectNode();
            ObjectNode properties = node.putObject("properties");
            properties.put("body_encoding", "nested");

            try {
                Map<String, String> map = getSpanData(tracer);
                properties.set("trace", mapper.valueToTree(map));
            } catch (Throwable e) {
                LOG.error("Preparing a trace failed: {}", e.toString());
            }

            try {
                node.putPOJO("body", task);
                return mapper.writeValueAsString(node).getBytes();
            } catch (JsonProcessingException e) {
                LOG.error("Serialize failed for task {}: {}", task, e.getMessage());
                return null;
            }
        }

        private Map<String, String> getSpanData(Tracer tracer) throws JsonProcessingException {
            Span span = tracer.buildSpan(TRACE_OPERATION_NAME)
                    .asChildOf(tracer.activeSpan())
                    .start();

            Map<String, String> map = new HashMap<>();
            TextMapInjectAdapter textMap = new TextMapInjectAdapter(map);
            tracer.inject(span.context(), Format.Builtin.TEXT_MAP, textMap);
            span.finish();

            return map;
        }
    }

    private static class CompressedNestedWriter extends PlainWriter {
        private CompressedNestedWriter(final Tracer tracer) {
            super(tracer);
        }

        @Override
        public byte[] asCeleryTask(CeleryBody task) {
            try {
                byte[] result = super.asCeleryTask(task);
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

    public abstract byte[] asCeleryTask(CeleryBody task);
}
