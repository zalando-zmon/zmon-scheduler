package de.zalando.zmon.scheduler.ng.scheduler;

import de.zalando.zmon.scheduler.ng.SchedulerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by jmussler on 30.06.16.
 */
public class RedisMetricsUpdater implements Runnable {

    private final String name;
    private final SchedulerConfig config;
    private final SchedulerMetrics metrics;
    private final Logger logger = LoggerFactory.getLogger(RedisMetricsUpdater.class);

    public RedisMetricsUpdater(SchedulerConfig config, SchedulerMetrics metrics) {
        String n;
        try {
            n = "s-p" + config.getServerPort() + "." + InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException ex) {
            n = "s-p" + config.getServerPort() + ".unknown";
        }
        name = n;
        this.config = config;
        this.metrics = metrics;
    }

    @Override
    public void run() {
        try(Jedis jedis = new Jedis(config.getRedisHost(), config.getRedisPort())) {
            Pipeline p = jedis.pipelined();
            p.sadd("zmon:metrics", name);
            p.set("zmon:metrics:" + name + ":check.count", metrics.getTotalChecks() + "");
            p.set("zmon:metrics:" + name + ":ts", System.currentTimeMillis() / 1000 + "");
            p.sync();
        }
        catch (Throwable t) {
            logger.error("Metrics update failed: {} host={} port={}", t.getMessage(), config.getRedisHost(), config.getRedisPort());
        }
    }
}
