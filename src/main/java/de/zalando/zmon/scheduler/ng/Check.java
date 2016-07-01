package de.zalando.zmon.scheduler.ng;

import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.entities.Entity;

/**
 * Created by jmussler on 30.06.16.
 */
public class Check {
    private final int id;
    private final CheckRepository checkRepo;

    public Check(int id, CheckRepository checkRepo) {
        this.id = id;
        this.checkRepo = checkRepo;
    }

    public CheckDefinition getCheckDefinition() {
        return checkRepo.get(id);
    }

    public boolean matchEntity(Entity entity) {
        return AlertOverlapGenerator.matchCheckFilter(getCheckDefinition(), entity);
    }

    public int getId() {
        return id;
    }
}
