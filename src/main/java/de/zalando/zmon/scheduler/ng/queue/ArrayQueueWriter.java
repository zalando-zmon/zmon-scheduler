package de.zalando.zmon.scheduler.ng.queue;

import com.codahale.metrics.MetricRegistry;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by jmussler on 30.06.16.
 */
public class ArrayQueueWriter extends QueueWriter {
    private final HashMap<String, ArrayList<byte[]>> tasks = new HashMap<>();

    public ArrayQueueWriter(MetricRegistry metrics) {
        super(metrics);
    }

    @Override
    protected void write(String queue, byte[] command) {
        synchronized (this) {
            ArrayList<byte[]> l = tasks.getOrDefault(queue, null);
            if (null == l) {
                l = new ArrayList<>();
                tasks.put(queue, l);
            }
            l.add(command);
        }
    }
}
