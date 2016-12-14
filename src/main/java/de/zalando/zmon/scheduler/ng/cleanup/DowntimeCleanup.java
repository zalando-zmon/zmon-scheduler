package de.zalando.zmon.scheduler.ng.cleanup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by jmussler on 13.12.16.
 */
public class DowntimeCleanup implements Runnable {

    private final Logger log = LoggerFactory.getLogger(DowntimeCleanup.class);
    private final SchedulerConfig config;
    private final ObjectMapper mapper = (new ObjectMapper()).setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    public DowntimeCleanup(SchedulerConfig config) {
        this.config = config;
        executor.scheduleAtFixedRate(this, 5, 15, TimeUnit.MINUTES);
    }

    // not pipelining on purpose, this is just background task, dont want to block
    // just deleting entries not valid any more. Cleaning up upper level entries is done on next iteration
    private void doCleanup() {
        long now = System.currentTimeMillis() / 1000L;

        try(Jedis jedis = new Jedis(config.getRedisHost(), config.getRedisPort())) {
            Set<String> alertIdsInDowntime = jedis.smembers("zmon:downtimes");
            log.info("Alerts in downtime: count={}", alertIdsInDowntime.size());
            int cleanupCounter = 0;
            for(String alertId : alertIdsInDowntime) {
                Set<String> entityIds = jedis.smembers("zmon:downtimes:" + alertId);
                if (null == entityIds || entityIds.size() == 0) {
                    cleanupCounter++;
                    jedis.srem("zmon:downtimes", alertId);
                }

                for(String entityId : entityIds) {
                    Map<String, String> downtimes = jedis.hgetAll("zmon:downtimes:" + alertId + ":" + entityId);
                    if(null == downtimes || downtimes.size() == 0) {
                        cleanupCounter++;
                        jedis.srem("zmon:downtimes:" + alertId, entityId);
                    }

                    for(Map.Entry<String, String> dt  : downtimes.entrySet()) {
                        try {
                            JsonNode d = mapper.readTree(dt.getValue());
                            if(d.has("end_time") && Long.compare(now, d.get("end_time").asLong()) >= 0) {
                                jedis.hdel("zmon:downtimes:" + alertId + ":" + entityId, dt.getKey());
                                cleanupCounter++;
                            }
                        }
                        catch(IOException ex) {
                            // malformed json redis, delete downtime entry
                            log.info("Failed to read downtime: entity={} id={}", entityId, dt.getKey());
                            jedis.hdel("zmon:downtimes:" + alertId + ":" + entityId, dt.getKey());
                        }
                    }
                }
            }
            log.info("Cleanup downtimes ended: count={}", cleanupCounter);
        }
    }

    @Override
    public void run() {
        try {
            doCleanup();
        }
        catch(Throwable t) {
            log.error("Cleanup downtimes failed", t);
        }
    }
}
