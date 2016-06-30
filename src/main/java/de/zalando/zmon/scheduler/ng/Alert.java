package de.zalando.zmon.scheduler.ng;

import de.zalando.zmon.scheduler.ng.alerts.AlertDefinition;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.entities.Entity;

/**
 * Created by jmussler on 30.06.16.
 */
public class Alert {

    private final int id;
    private final AlertRepository alertRepo;

    public Alert(int id, AlertRepository alertRepo) {
        this.alertRepo = alertRepo;
        this.id = id;
    }

    public AlertDefinition getAlertDefinition() {
        return alertRepo.get(id);
    }

    public boolean matchEntity(Entity entity) {
        return AlertOverlapGenerator.matchAlertFilter(getAlertDefinition(), entity);
    }

    public int getId() {
        return id;
    }
}
