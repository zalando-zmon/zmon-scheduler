package de.zalando.zmon.scheduler.ng.cleanup;

import de.zalando.zmon.scheduler.ng.AlertOverlapGenerator;
import de.zalando.zmon.scheduler.ng.SchedulerConfig;
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by jmussler on 02.06.16.
 */
public class AlertChangeCleaner implements AlertChangeListener {

    private final static Logger LOG = LoggerFactory.getLogger(AlertChangeListener.class);

    private final AlertRepository alertRepository;
    private final CheckRepository checkRepository;
    private final EntityRepository entityRepository;

    private final JedisPool redisPool;

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    public AlertChangeCleaner(AlertRepository alertRepo, CheckRepository checkRepository, EntityRepository entityRepo, SchedulerConfig config) {
        this.alertRepository = alertRepo;
        this.checkRepository = checkRepository;
        this.entityRepository = entityRepo;

        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setTestOnBorrow(true);

        redisPool = new JedisPool(poolConfig, config.getRedis_host(), config.getRedis_port());
    }

    @Override
    public void notifyAlertNew(AlertDefinition alert) {

    }

    @Override
    public void notifyAlertChange(AlertDefinition alert) {
        final AlertChangeCleaner c = this;
        executor.schedule(()->c.doCleanup(alert.getId(), alert.getCheckDefinitionId()), 90, TimeUnit.SECONDS);
    }

    @Override
    public void notifyAlertDelete(AlertDefinition alert) {

    }

    private Set<String> getMatchingEntities(int alertId, int checkId) {
        Set<String> entityIds = new HashSet<>();

        CheckDefinition cd = checkRepository.get(checkId);
        AlertDefinition ad = alertRepository.get(alertId);

        for(Entity e: entityRepository.getUnfiltered()) {
            if(AlertOverlapGenerator.matchCheckFilter(cd, e)) {
                if(AlertOverlapGenerator.matchAlertFilter(ad, e)) {
                    entityIds.add(e.getId());
                }
            }
        }

        return entityIds;
    }

    public void doCleanup(int alertId, int checkId) {
        try {
            Jedis j = redisPool.getResource();
            try {

                final String alertKey = "zmon:alerts:" + alertId;
                final String alertMapKey = "zmon:alerts:" + alertId + ":entities";

                Set<String> entityIdsInAlert = j.smembers(alertKey);
                Set<String> entityIdsTotal = j.hkeys(alertMapKey);

                Set<String> matchedEntityIds = getMatchingEntities(alertId, checkId);

                entityIdsInAlert.removeAll(matchedEntityIds);
                entityIdsTotal.removeAll(matchedEntityIds);

                LOG.info("Cleaning alertId={} checkId={} srem={} hdel={}", alertId, checkId, entityIdsInAlert.size(), entityIdsTotal.size());

                Pipeline p = j.pipelined();
                for(String e : entityIdsInAlert) {
                    p.srem(alertKey, e);
                }

                for(String e: entityIdsTotal) {
                    p.hdel(alertMapKey, e);
                }
                p.sync();
            }
            finally {
                j.close();
            }
        }
        catch (Throwable t) {
            LOG.error("Uncaught/Unexpected exception", t);
        }
    }
}
