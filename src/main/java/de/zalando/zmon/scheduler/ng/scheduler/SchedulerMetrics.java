package de.zalando.zmon.scheduler.ng.scheduler;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

/**
 * Created by jmussler on 30.06.16.
 */
public class SchedulerMetrics {
    private final Meter totalChecks;
    private final Meter errorCount;
    private final MetricRegistry metrics;

    public SchedulerMetrics(MetricRegistry metrics) {
        totalChecks = metrics.meter("scheduler.total-checks");
        errorCount = metrics.meter("scheduler.total-errors");
        this.metrics = metrics;
    }

    public void incTotal() {
        totalChecks.mark();
    }

    public void incError() {
        errorCount.mark();
    }

    public long getTotalChecks() {
        return this.totalChecks.getCount();
    }

    public long getErrorCount() {
        return errorCount.getCount();
    }

    public MetricRegistry getMetrics() {
        return metrics;
    }
}
