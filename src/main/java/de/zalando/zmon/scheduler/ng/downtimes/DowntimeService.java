package de.zalando.zmon.scheduler.ng.downtimes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.entities.EntityRepository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

/**
 * Created by jmussler on 18.06.16.
 */
@Component
public class DowntimeService {

    private final ObjectMapper mapper = (new ObjectMapper()).setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    private final Logger log = LoggerFactory.getLogger(DowntimeService.class);

    private final SchedulerConfig config;
    private final EntityRepository entityRepository;
    private final boolean enableEntityFilter;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Autowired
    public DowntimeService(SchedulerConfig config, EntityRepository entityRepository) {
        this.entityRepository = entityRepository;
        this.enableEntityFilter = config.isDowntimeEntityFilter();
        this.config = config;
    }

    private DowntimeRequestResult storeInRedis(DowntimeRequest request) {
        final String groupId = request.getGroupId();
        final List<String> listOfIds = new ArrayList<>();

        DowntimeRequestResult result = new DowntimeRequestResult(groupId);
        try (Jedis jedis = new Jedis(config.getRedisHost(), config.getRedisPort())) {
            // create pipeline
            final Pipeline p = jedis.pipelined();
            for (final DowntimeAlertRequest downtimeEntities : request.getDowntimeEntities()) {
                p.sadd("zmon:downtimes", "" + downtimeEntities.getAlertId());

                final String entitiesPattern = "zmon:downtimes:" + downtimeEntities.getAlertId();
                for (final Map.Entry<String, String> entry : downtimeEntities.getEntityIds().entrySet()) {
                    final String entityId = entry.getKey();

                    if (enableEntityFilter && !entityRepository.getCurrentMap().containsKey(entityId)) {
                        continue;
                    }

                    final String uuid = entry.getValue();
                    p.sadd(entitiesPattern, entityId);

                    final DowntimeData details = new DowntimeData();

                    details.setId(uuid);
                    listOfIds.add(uuid); // store that this id is actually used in this DC
                    details.setGroupId(groupId);
                    details.setComment(request.getComment());
                    details.setStartTime(request.getStartTime());
                    details.setEndTime(request.getEndTime());
                    details.setAlertId(downtimeEntities.getAlertId());
                    details.setEntity(entityId);
                    details.setCreatedBy(request.getCreatedBy());

                    try {
                        final String json = mapper.writeValueAsString(details);
                        p.hset(entitiesPattern + ":" + entityId, uuid, json);
                        result.getIds().put(entityId, uuid);
                    } catch (final IOException e) {
                        log.error("creating entity downtime failed: entity={} groupId={}", entityId, groupId);
                    }
                    p.sadd("zmon:downtime-groups:" + groupId, listOfIds.toArray(new String[listOfIds.size()]));
                }
            }
            p.sync();
        }
        return result;
    }

    public DowntimeRequestResult storeDowntime(DowntimeRequest request) {
        // store in Redis
        return storeInRedis(request);
    }

/* Downtimes look like this in Redis:

127.0.0.1:6379> keys *downtime*
1) "zmon:downtimes:5"
2) "zmon:downtimes:5:zmon-worker"
3) "zmon:downtimes"
4) "zmon:active_downtimes"


127.0.0.1:6379> smembers zmon:downtimes
1) "5"

127.0.0.1:6379> smembers zmon:downtimes:5
1) "zmon-worker"

127.0.0.1:6379> hgetall zmon:downtimes:5:zmon-worker
1) "0c64cd04-5adc-40b0-9f2b-cee405afec7f"
2) "{\"comment\":\"Jan-M\",\"start_time\":1467128641,\"end_time\":1467130441,\"id\":\"0c64cd04-5adc-40b0-9f2b-cee405afec7f\",\"alert_id\":5,\"entity\":\"zmon-worker\",\"group_id\":\"fcd43031-a120-47d5-b4db-f78b52b10d70\",\"created_by\":\"test\"}"

For now do a very stupid delete, we just assume that the id is present and delete for now, this does not hurt, otherwise we would do one more read anyways

*/

    public void deleteDowntimeGroup(String groupId) {
        DeleteGroupTask t = new DeleteGroupTask(groupId);
        executor.execute(t);
    }

    public void deleteDowntimes(final Collection<String> downtimeIds) {
        DeleteDowntimesTask t = new DeleteDowntimesTask(downtimeIds);
        executor.execute(t);
    }

    public static class DowntimeEntry {
        public String alertId;
        public String entity;

        public DowntimeEntry(String alertId, String entity) {
            this.entity = entity;
            this.alertId = alertId;
        }
    }

    protected void doDeleteDowntimeGroup(String groupId) {
        Set<String> ids = null;
        try (Jedis jedis = new Jedis(config.getRedisHost(), config.getRedisPort())) {
            ids = jedis.smembers("zmon:downtime-groups:" + groupId);
            jedis.del("zmon:downtime-groups:" + groupId);
        }

        if (null != ids) {
            log.info("deleting downtime group: id={} count={}", groupId, ids.size());
            deleteDowntimesByIds(ids);
        }
    }

    protected void deleteDowntimesByIds(final Collection<String> downtimeIds) {
        Map<DowntimeEntry, Collection<String>> toDeleteItems = new HashMap<>();
        try (Jedis jedis = new Jedis(config.getRedisHost(), config.getRedisPort())) {
            Set<String> alertsInDowntime = jedis.smembers("zmon:downtimes");
            for (String alertId : alertsInDowntime) {
                Set<String> entities = jedis.smembers("zmon:downtimes:" + alertId);
                for (String entity : entities) {
                    toDeleteItems.put(new DowntimeEntry(alertId, entity), downtimeIds);
                }
            }
        }

        log.info("deleting individual downtime entries: count={}", toDeleteItems.size());
        doDelete(toDeleteItems);
    }

    protected void doDelete(Map<DowntimeEntry, Collection<String>> toDeleteEntries) {
        try (Jedis jedis = new Jedis(config.getRedisHost(), config.getRedisPort())) {
            for (Map.Entry<DowntimeEntry, Collection<String>> entry : toDeleteEntries.entrySet()) {
                final String key = "zmon:downtimes:" + entry.getKey().alertId + ":" + entry.getKey().entity;
                for (String id : entry.getValue()) {
                    jedis.hdel(key, id);
                }

                String type = jedis.type(key);
                if ("none".equals(type)) {
                    final String alertKey = "zmon:downtimes:" + entry.getKey().alertId;
                    jedis.srem(alertKey, entry.getKey().entity);
                    Set<String> members =jedis.smembers(alertKey);
                    if (null == members || members.size() == 0) {
                        jedis.srem("zmon:downtimes", entry.getKey().alertId);
                    }
                }
            }
        }
    }

    private class DeleteDowntimesTask implements Runnable {

        private final Collection<String> ids;

        public DeleteDowntimesTask(Collection<String> ids) {
            this.ids = ids;
        }

        public void delete() {
            deleteDowntimesByIds(ids);
        }

        @Override
        public void run() {
            try {
                long start = System.currentTimeMillis();
                delete();
                long end = System.currentTimeMillis();
                log.info("Deleted downtimes {} in {}ms", ids, end - start);
            }
            catch(Throwable t) {
                log.error("Failed delete downtimes task", t);
            }
        }
    }

    private class DeleteGroupTask implements Runnable {

        private final String groupId;

        public DeleteGroupTask(String groupId) {
            this.groupId = groupId;
        }

        public void delete() {
            doDeleteDowntimeGroup(groupId);
        }

        @Override
        public void run() {
            try {
                long start = System.currentTimeMillis();
                delete();
                long end = System.currentTimeMillis();
                log.info("Deleted downtime group {} in {}ms", groupId, end - start);
            }
            catch(Throwable t) {
                log.error("Failed group delete task", t);
            }
        }
    }
}
