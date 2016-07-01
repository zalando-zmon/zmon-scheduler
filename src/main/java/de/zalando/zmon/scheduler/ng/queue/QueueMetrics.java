package de.zalando.zmon.scheduler.ng.queue;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jmussler on 30.06.16.
 */
public class QueueMetrics {
    private final MetricRegistry metrics;

    private final Map<String, Meter> meters = new HashMap<>();
    private final Meter throughPutMeter;

    public QueueMetrics(MetricRegistry metrics) {
        this.metrics = metrics;
        this.throughPutMeter = metrics.meter("scheduler.byte-throughput");
    }

    public void incThroughput(int l) {
        throughPutMeter.mark(l);
    }

    public void mark(String q) {
        if(meters.containsKey(q)) {
            meters.get(q).mark();
        }
        else {
            synchronized (this) {
                if(meters.containsKey(q)) {
                    meters.get(q).mark();
                    return;
                }

                Meter m = metrics.meter("scheduler.queue." + q);
                meters.put(q, m);
                m.mark();
            }
        }
    }
}
