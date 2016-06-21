package de.zalando.zmon.scheduler.ng;

import redis.clients.jedis.Response;

/**
 * Created by jmussler on 21.06.16.
 */

public final class RedisResponseHolder<K, R> {

    private final K key;
    private final Response<R> response;

    private RedisResponseHolder(final K key, final Response<R> response) {
        this.key = key;
        this.response = response;
    }

    public K getKey() {
        return key;
    }

    public Response<R> getResponse() {
        return response;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ResponseHolder [key=");
        builder.append(key);
        builder.append(", response=");
        builder.append(response);
        builder.append("]");
        return builder.toString();
    }

    public static <K, R> RedisResponseHolder<K, R> create(final K key, final Response<R> response) {
        return new RedisResponseHolder<>(key, response);
    }

}
