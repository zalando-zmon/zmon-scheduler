package de.zalando.zmon.scheduler.ng.checks;

import de.zalando.zmon.scheduler.ng.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jmussler on 18.06.16.
 */
public class CheckChangedListener implements CheckChangeListener {

    private final Scheduler scheduler;
    private final Logger log = LoggerFactory.getLogger(CheckChangedListener.class);

    public CheckChangedListener(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void notifyNewCheck(CheckRepository repo, int checkId) {
        log.info("New check discovered: checkId={}", checkId);
        scheduler.schedule(checkId, 0);
    }

    @Override
    public void notifyCheckIntervalChange(CheckRepository repo, int checkId) {
        log.info("Check interval changed: checkId={}", checkId);
        scheduler.executeImmediate(checkId);
    }

    @Override
    public void notifyDeleteCheck(CheckRepository repo, int checkId) {
        log.info("Check removed or inactive: checkId={}", checkId);
        scheduler.unschedule(checkId);
    }

    @Override
    public void notifyFilterChange(int checkId) {

    }
}
