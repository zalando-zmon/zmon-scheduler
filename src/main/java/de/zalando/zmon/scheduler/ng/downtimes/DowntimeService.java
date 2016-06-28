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
import redis.clients.jedis.Response;

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

    private List<RedisResponseHolder<Integer, Set<String>>> fetchEntities(final Jedis jedis,
                                                                          final Iterable<Integer> alertIdsWithDowntime) {
        final List<RedisResponseHolder<Integer, Set<String>>> asyncAlertEntities = new LinkedList<>();

        final Pipeline p = jedis.pipelined();
        for (final Integer alertDefinitionId : alertIdsWithDowntime) {
            asyncAlertEntities.add(RedisResponseHolder.create(alertDefinitionId,
                    p.smembers("zmon:alerts:" + alertDefinitionId)));
        }

        p.sync();

        return asyncAlertEntities;
    }

    private Set<Integer> alertsInDowntime(final Jedis jedis) {
        return jedis.smembers("zmon:downtimes").stream().map(Integer::parseInt).collect(Collectors.toSet());
    }

    public void deleteDowntimeGroup(String groupId) {
        final Collection<Response<List<String>>> deleteResults = new LinkedList<>();
        try (Jedis jedis = redisPool.getResource()) {
            final List<RedisResponseHolder<Integer, Set<String>>> asyncAlertEntities = fetchEntities(jedis,
                    alertsInDowntime(jedis));

            // this is slow but we are not expecting so many deletes
            final Pipeline p = jedis.pipelined();
            for (final RedisResponseHolder<Integer, Set<String>> response : asyncAlertEntities) {
                for (final String entity : response.getResponse().get()) {
                    deleteResults.add(p.hvals("zmon:downtimes:" + response.getKey() + ":" + entity));
                }
            }

            p.sync();
        }

        final Map<String, DowntimeDetailsFormat> toRemoveJsonDetails = new HashMap<>();
        for (final Response<List<String>> response : deleteResults) {
            for (final String jsonDetails : response.get()) {
                try {
                    final DowntimeData details = mapper.readValue(jsonDetails, DowntimeData.class);
                    if (groupId.equals(details.getGroupId())) {
                        toRemoveJsonDetails.put(details.getId(), new DowntimeDetailsFormat(details, jsonDetails));
                    }
                } catch (final IOException e) {
                    throw new RuntimeException("Could not read JSON: " + jsonDetails, e);
                }
            }
        }
    }

    public void deleteDowntimes(final Collection<String> downtimeIds) {
        if (!downtimeIds.isEmpty()) {
            final Collection<Response<String>> deleteResults = new LinkedList<>();

            try (Jedis jedis = redisPool.getResource()) {
                final List<RedisResponseHolder<Integer, Set<String>>> asyncAlertEntities = fetchEntities(jedis,
                        alertsInDowntime(jedis));

                // this is slow but we are not expecting so many deletes
                final Pipeline p = jedis.pipelined();
                for (final RedisResponseHolder<Integer, Set<String>> response : asyncAlertEntities) {
                    for (final String entity : response.getResponse().get()) {
                        for (final String downtimeId : downtimeIds) {
                            deleteResults.add(p.hget("zmon:downtimes:" + response.getKey() + ":" + entity, downtimeId));
                        }
                    }
                }

                p.sync();
            }

            final Map<String, DowntimeDetailsFormat> toRemoveJsonDetails = new HashMap<>();
            for (final Response<String> response : deleteResults) {
                final String jsonDetails = response.get();
                if (jsonDetails != null) {
                    try {
                        final DowntimeData details = mapper.readValue(jsonDetails, DowntimeData.class);
                        toRemoveJsonDetails.put(details.getId(), new DowntimeDetailsFormat(details, jsonDetails));
                    } catch (final IOException e) {
                        throw new RuntimeException("Could not read JSON: " + jsonDetails, e);
                    }
                }
            }
        }
    }

    public DowntimeRequestResult storeDowntime(DowntimeRequest request) {
        // store in Redis
        return storeInRedis(request);
    }
}
