package de.zalando.zmon.scheduler.ng.downtimes;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.zalando.zmon.scheduler.ng.Scheduler;
import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import de.zalando.zmon.scheduler.ng.alerts.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by jmussler on 18.06.16.
 */
@Component
public class DowntimeService {

    private final JedisPool redisPool;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger log = LoggerFactory.getLogger(DowntimeService.class);

    @Autowired
    public DowntimeService(SchedulerConfig config) {

        redisPool = new JedisPool(config.getRedis_host(), config.getRedis_port());
    }

    private void storeInRedis(DowntimeRequest request, String groupId) {
        try (Jedis jedis = redisPool.getResource()) {
            // create pipeline
            final Pipeline p = jedis.pipelined();
            for (final DowntimeAlertRequest downtimeEntities : request.getDowntimeEntities()) {
                p.sadd("zmon:downtimes", "" + downtimeEntities.getAlertId());

                final String entitiesPattern = "zmon:downtimes:" + downtimeEntities.getAlertId();
                for (final String entity : downtimeEntities.getEntityIds()) {
                    p.sadd(entitiesPattern, entity);

                    // generate id
                    final String id = UUID.randomUUID().toString();

                    final DowntimeData details = new DowntimeData();
                    details.setId(id);
                    details.setGroupId(groupId);
                    details.setComment(request.getComment());
                    details.setStartTime(request.getStartTime());
                    details.setEndTime(request.getEndTime());
                    details.setAlertId(downtimeEntities.getAlertId());
                    details.setEntity(entity);
                    details.setCreatedBy(request.getCreatedBy());

                    try {
                        final String json = mapper.writeValueAsString(details);
                        p.hset(entitiesPattern + ":" + entity, id, json);
                    } catch (final IOException e) {
                        log.error("", e);
                    }
                }
            }
            p.sync();
        }
    }

    public void deleteDowntimeGroup(String groupId) {

    }

    public void deleteDowntime(String id) {

    }

    public void storeDowntime(DowntimeRequest request) {
        // store in Redis
        storeInRedis(request, UUID.randomUUID().toString());
    }
}
