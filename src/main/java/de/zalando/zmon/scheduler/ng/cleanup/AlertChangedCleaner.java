package de.zalando.zmon.scheduler.ng.cleanup;

import de.zalando.zmon.scheduler.ng.AlertOverlapGenerator;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.alerts.AlertChangeListener;
import de.zalando.zmon.scheduler.ng.alerts.AlertDefinition;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by jmussler on 02.06.16.
 */
public class AlertChangedCleaner implements AlertChangeListener {

    private final static Logger LOG = LoggerFactory.getLogger(AlertChangeListener.class);

    private final AlertRepository alertRepository;
    private final CheckRepository checkRepository;
    private final EntityRepository entityRepository;

    private final JedisPool redisPool;

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    public AlertChangedCleaner(AlertRepository alertRepo, CheckRepository checkRepository, EntityRepository entityRepo, SchedulerConfig config) {
        this.alertRepository = alertRepo;
        this.checkRepository = checkRepository;
        this.entityRepository = entityRepo;

        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setTestOnBorrow(true);

        redisPool = new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort());
    }

    @Override
    public void notifyAlertNew(AlertDefinition alert) {

    }

    @Override
    public void notifyAlertChange(AlertDefinition alert) {
        final AlertChangedCleaner c = this;
        // schedule twice, 1 fast for UI/UX the long run is only for multi scheduler setup where results may come in too late.
        executor.schedule(() -> c.doCleanup(alert.getId(), alert.getCheckDefinitionId()), 10, TimeUnit.SECONDS);
        executor.schedule(() -> c.doCleanup(alert.getId(), alert.getCheckDefinitionId()), 90, TimeUnit.SECONDS);
    }

    @Override
    public void notifyAlertDelete(AlertDefinition alert) {

    }

    private Set<String> getMatchingEntities(int alertId, int checkId) {
        CheckDefinition cd = checkRepository.get(checkId);
        AlertDefinition ad = alertRepository.get(alertId);

        return entityRepository.getUnfiltered().stream()
                .filter(e -> AlertOverlapGenerator.matchCheckFilter(cd, e))
                .filter(e -> AlertOverlapGenerator.matchAlertFilter(ad, e))
                .map(Entity::getId)
                .collect(Collectors.toSet());
    }

    public void doCleanup(int alertId, int checkId) {
        try {
            try (Jedis j = redisPool.getResource()) {

                final String alertKey = "zmon:alerts:" + alertId;
                final String alertMapKey = "zmon:alerts:" + alertId + ":entities";

                Set<String> entityIdsInAlert = j.smembers(alertKey);
                Set<String> entityIdsTotal = j.hkeys(alertMapKey);

                Set<String> matchedEntityIds = getMatchingEntities(alertId, checkId);

                entityIdsInAlert.removeAll(matchedEntityIds);
                entityIdsTotal.removeAll(matchedEntityIds);

                LOG.info("Cleaning alertId={} checkId={} srem={} hdel={}", alertId, checkId, entityIdsInAlert.size(), entityIdsTotal.size());

                Pipeline p = j.pipelined();
                for (String e : entityIdsInAlert) {
                    p.srem(alertKey, e);
                }

                for (String e : entityIdsTotal) {
                    p.hdel(alertMapKey, e);
                }
                p.sync();
            }
        } catch (Throwable t) {
            LOG.error("Uncaught/Unexpected exception: msg={} check_id={} alert_id={}", t, checkId, alertId);
        }
    }
}
