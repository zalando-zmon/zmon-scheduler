package de.zalando.zmon.scheduler.ng.cleanup;

import de.zalando.zmon.scheduler.ng.alerts.AlertDefinition;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckChangeListener;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;

import java.util.Collection;

/**
 * Created by jmussler on 02.06.16.
 */
public class CheckChangeCleaner implements CheckChangeListener {

    private final AlertChangeCleaner alertCleaner;

    private final AlertRepository alertRepository;

    public CheckChangeCleaner(AlertRepository alertRepo, AlertChangeCleaner alertCleaner) {
        this.alertRepository = alertRepo;
        this.alertCleaner = alertCleaner;
    }

    @Override
    public void notifyNewCheck(CheckRepository repo, int checkId) {

    }

    @Override
    public void notifyCheckIntervalChange(CheckRepository repo, int checkId) {

    }

    @Override
    public void notifyDeleteCheck(CheckRepository repo, int checkId) {

    }

    @Override
    public void notifyFilterChange(int checkId) {
        /* Just redirect to clean up all child alerts */
        Collection<AlertDefinition> alertDefs = alertRepository.getByCheckId(checkId);
        for(AlertDefinition ad : alertDefs) {
            alertCleaner.notifyAlertChange(ad);
        }
    }
}
