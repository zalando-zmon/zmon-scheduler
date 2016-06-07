package de.zalando.zmon.scheduler.ng.checks;

/**
 * Created by jmussler on 4/17/15.
 */
public interface CheckChangeListener {
    void notifyNewCheck(CheckRepository repo, int checkId);

    void notifyCheckIntervalChange(CheckRepository repo, int checkId);

    void notifyDeleteCheck(CheckRepository repo, int checkId);

    void notifyFilterChange(int checkId);
}
