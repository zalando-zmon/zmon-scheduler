package de.zalando.zmon.scheduler.ng.cleanup;

import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.Set;

/**
 * Created by jmussler on 05.06.16.
 */
public class AllTrialRunCleanupTask implements Runnable {

    public static final Logger LOG = LoggerFactory.getLogger(AllTrialRunCleanupTask.class);

    private final SchedulerConfig config;

    public AllTrialRunCleanupTask(SchedulerConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        try {
            LOG.info("Starting cleanup of old trial run results");
            Jedis jedis = new Jedis(config.getRedisHost(), config.getRedisPort());
            try {
                Set<String> keys = jedis.keys("zmon:trial_run:*");
                // No pipeline here, considering this startup task to be fast enough
                // Dont want this to fail on not cleaned appliance deployments
                for (String k : keys) {
                    jedis.del(k);
                }

                LOG.info("Finished trial run cleanup: count={}", keys.size());
            } finally {
                jedis.close();
            }
        } catch (Throwable t) {
            LOG.error("Failed to cleanup old trial runs: msg={}", t.getMessage());
        }
    }
}
