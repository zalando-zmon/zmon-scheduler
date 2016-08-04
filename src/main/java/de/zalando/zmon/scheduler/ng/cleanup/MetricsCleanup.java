package de.zalando.zmon.scheduler.ng.cleanup;

import de.zalando.zmon.scheduler.ng.config.SchedulerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by jmussler on 11.07.16.
 */
public class MetricsCleanup {
    private final SchedulerConfig config;
    private final Logger log = LoggerFactory.getLogger(MetricsCleanup.class);
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    public MetricsCleanup(SchedulerConfig config) {
        this.config = config;
        executor.scheduleAtFixedRate(new CleanupTask(), 0, 5, TimeUnit.MINUTES);
    }

    private class CleanupTask implements Runnable {

        @Override
        public void run() {
            try (Jedis jedis = new Jedis(config.getRedisHost(), config.getRedisPort())) {
                float currentTimeSeconds = System.currentTimeMillis() / 1000;
                int count = 0;
                Set<String> members = jedis.smembers("zmon:metrics");
                for (String m : members) {
                    // NOTE: the timestamp is in seconds since epoch (Python worker writes a float value)
                    float lastUpdateSeconds = Float.parseFloat(jedis.get("zmon:metrics:" + m + ":ts"));
                    if (lastUpdateSeconds < (currentTimeSeconds - 60 * 60)) {
                        // cleanup entries that have no activity for 60 min for now
                        jedis.del("zmon:metrics:" + m + ":ts");
                        jedis.del("zmon:metrics:" + m + ":check.count");
                        jedis.srem("zmon:metrics", m);
                        count++;
                    }
                }
                log.info("removed {} entities", count);
            } catch (Throwable t) {
                log.error("Metrics cleanup failed: {}", t.getMessage());
            }
        }
    }
}
