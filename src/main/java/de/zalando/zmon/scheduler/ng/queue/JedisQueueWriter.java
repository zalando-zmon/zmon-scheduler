package de.zalando.zmon.scheduler.ng.queue;

import com.codahale.metrics.MetricRegistry;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by jmussler on 30.06.16.
 */
public class JedisQueueWriter extends QueueWriter {

    private final JedisPool pool;

    public JedisQueueWriter(String host, int port, MetricRegistry metrics) {
        super(metrics);
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(8);
        this.pool = new JedisPool(config, host, port);
    }

    @Override
    protected void write(String queue, byte[] command) {
        try(Jedis jedis = pool.getResource()) {
            jedis.rpush(queue.getBytes(), command);
        }
    }
}
