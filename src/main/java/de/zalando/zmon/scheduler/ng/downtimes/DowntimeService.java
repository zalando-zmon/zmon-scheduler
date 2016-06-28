package de.zalando.zmon.scheduler.ng.downtimes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import de.zalando.zmon.scheduler.ng.RedisResponseHolder;
import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jmussler on 18.06.16.
 */
@Component
public class DowntimeService {

    private final JedisPool redisPool;
    private final ObjectMapper mapper = (new ObjectMapper()).setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    private final Logger log = LoggerFactory.getLogger(DowntimeService.class);

    @Autowired
    public DowntimeService(SchedulerConfig config) {
        redisPool = new JedisPool(config.getRedis_host(), config.getRedis_port());
    }

    private DowntimeRequestResult storeInRedis(DowntimeRequest request) {
        final String groupId = request.getGroupId();

        DowntimeRequestResult result = new DowntimeRequestResult(groupId);
        try (Jedis jedis = redisPool.getResource()) {
            // create pipeline
            final Pipeline p = jedis.pipelined();
            for (final DowntimeAlertRequest downtimeEntities : request.getDowntimeEntities()) {
                p.sadd("zmon:downtimes", "" + downtimeEntities.getAlertId());

                final String entitiesPattern = "zmon:downtimes:" + downtimeEntities.getAlertId();
                for (final Map.Entry<String, String> entry : downtimeEntities.getEntityIds().entrySet()) {
                    final String entityId = entry.getKey();
                    final String uuid = entry.getValue();
                    p.sadd(entitiesPattern, entityId);

                    final DowntimeData details = new DowntimeData();

                    details.setId(uuid);
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
                }
            }
            p.sync();
        }
        return result;
    }

    private static final class DowntimeDetailsFormat {
        private final DowntimeData downtimeDetails;
        private final String json;

        private DowntimeDetailsFormat(final DowntimeData downtimeDetails, final String json) {
            this.downtimeDetails = downtimeDetails;
            this.json = json;
        }

        public DowntimeData getDowntimeDetails() {
            return downtimeDetails;
        }

        public String getJson() {
            return json;
        }
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

    }

    public static class DowntimeEntry {
        public String alertId;
        public String entity;

        public DowntimeEntry(String alertId, String entity) {
            this.entity = entity;
            this.alertId = alertId;
        }
    }

    public void deleteDowntimes(final Collection<String> downtimeIds) {
        Map<DowntimeEntry, Collection<String>> toDeleteItems = new HashMap<>();
        try(Jedis jedis = redisPool.getResource()) {
            Set<String> alertsInDowntime = jedis.smembers("zmon:downtimes");
            for(String alertId : alertsInDowntime) {
                Set<String> entities = jedis.smembers("zmon:downtimes:" + alertId);
                for(String entity : entities) {
                    toDeleteItems.put(new DowntimeEntry(alertId, entity), downtimeIds);
                }
            }
        }
        doDelete(toDeleteItems);
    }

    public void doDelete(Map<DowntimeEntry, Collection<String>> toDeleteEntries) {
        try (Jedis jedis = redisPool.getResource()) {
            for (Map.Entry<DowntimeEntry, Collection<String>> entry : toDeleteEntries.entrySet()) {
                final String key = "zmon:downtimes:" + entry.getKey().alertId + ":" + entry.getKey().entity;
                for(String id : entry.getValue()) {
                    jedis.hdel(key, id);
                }
                String type = jedis.type(key);
                if (null == type) {
                    jedis.srem("zmon:downtimes:" + entry.getKey().alertId);
                    if (jedis.smembers("zmon:downtimes:" + entry.getKey()).size() == 0) {
                        jedis.srem("zmon:downtimes", entry.getKey().alertId);
                    }
                }
            }
        }
    }
}
