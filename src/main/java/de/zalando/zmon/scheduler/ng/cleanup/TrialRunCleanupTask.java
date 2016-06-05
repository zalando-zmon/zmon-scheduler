package de.zalando.zmon.scheduler.ng.cleanup;

import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ScanResult;

/**
 * Created by jmussler on 05.06.16.
 */
public class TrialRunCleanupTask implements Runnable {

    public static final Logger LOG = LoggerFactory.getLogger(TrialRunCleanupTask.class);

    private final SchedulerConfig config;
    private String trialRunId;

    public TrialRunCleanupTask(String trialRunId, SchedulerConfig config) {
        this.config = config;
        this.trialRunId = trialRunId;
    }

    @Override
    public void run() {
        try {
            LOG.info("Trial run cleanup: id={}", this.trialRunId);
            Jedis jedis = new Jedis(config.getRedis_host(), config.getRedis_port());
            try {
                ScanResult<String> keys = jedis.scan("zmon:trial_run:" + trialRunId + "*");
                Pipeline p = jedis.pipelined();
                for(String k : keys.getResult()) {
                    p.del(k);
                }
                p.sync();
            }
            finally {
                jedis.close();
            }
        }
        catch(Throwable t) {
            LOG.error("Failed to cleanup trial run data: id={} msg={}", this.trialRunId, t.getMessage());
        }
    }
}
