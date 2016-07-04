package de.zalando.zmon.scheduler.ng.cleanup;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.zalando.zmon.scheduler.ng.AlertOverlapGenerator;
import de.zalando.zmon.scheduler.ng.alerts.AlertDefinition;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import de.zalando.zmon.scheduler.ng.checks.CheckDefinition;
import de.zalando.zmon.scheduler.ng.checks.CheckRepository;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.entities.Entity;
import de.zalando.zmon.scheduler.ng.entities.EntityChangeListener;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by jmussler on 30.06.16.
 */
public class SingleEntityCleanup implements EntityChangeListener{

    private final static Logger LOG = LoggerFactory.getLogger(SingleEntityCleanup.class);

    private final AlertRepository alertRepo;
    private final CheckRepository checkRepo;

    private final JedisPool jedisPool;
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    public SingleEntityCleanup(SchedulerConfig config, AlertRepository alertRepo, CheckRepository checkRepo, EntityRepository entityRepo) {
        this.alertRepo = alertRepo;
        this.checkRepo = checkRepo;

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setTestOnBorrow(true);

        jedisPool = new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort());
    }

    @Override
    public void notifyEntityChange(EntityRepository repo, Entity entityOld, Entity entityNew) {

    }

    @Override
    public void notifyEntityRemove(EntityRepository repo, Entity e) {
        executor.schedule(new EntityCleanupTask(e), 90, TimeUnit.SECONDS);
        executor.schedule(new EntityCleanupTask(e), 300, TimeUnit.SECONDS);
    }

    @Override
    public void notifyEntityAdd(EntityRepository repo, Entity e) {

    }

    private class EntityCleanupTask implements Runnable {
        private final Entity entity;

        public EntityCleanupTask(Entity entity) {
            this.entity = entity;
        }

        private void doCleanup(int checkId, Collection<Integer> alertIds, String entityId) {
            try(Jedis jedis = jedisPool.getResource()) {
                jedis.del("zmon:checks:" + checkId + ":" + entityId);
                for(Integer alertId : alertIds) {
                    jedis.srem("zmon:alerts:" + alertId, entityId);
                    jedis.del("zmon:alerts:" + alertId + ":" + entityId);
                    jedis.hdel("zmon:alerts:" + alertId + ":entities", entityId);
                }
            }
            catch(Throwable t) {
                LOG.error("Error during cleanup: entity={} checkId={}", entityId, checkId);
            }
        }

        @Override
        public void run() {
            int checksCleaned = 0;
            int alertsCleaned = 0;

            for (CheckDefinition checkDef : checkRepo.get()) {
                if (AlertOverlapGenerator.matchCheckFilter(checkDef, entity)) {
                    List<Integer> alerts = alertRepo.getByCheckId(checkDef.getId()).stream()
                                                      .filter(x->AlertOverlapGenerator.matchAlertFilter(x, entity))
                                                      .map(AlertDefinition::getId)
                                                      .collect(Collectors.toList());

                    if (!alerts.isEmpty()) {
                        doCleanup(checkDef.getId(), alerts, entity.getId());
                        checksCleaned++;
                        alertsCleaned += alerts.size();
                    }
                }
            }

            LOG.info("Cleanup entity={} checks={} alerts={}", entity.getId(), checksCleaned, alertsCleaned);
        }
    }
}
