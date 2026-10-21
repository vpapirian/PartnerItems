package com.partneritems.cooldown;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mirrors cooldowns into Redis so they survive restarts and server switches.
 *
 * The in-memory CooldownManager is still what gets checked during gameplay, Redis is only
 * written to in the background and read once when a player logs in. That way a slow or dead
 * Redis never blocks the main thread.
 *
 * Each player gets one hash: key-prefix + uuid -> { cooldownKey: expiresAtEpochMillis }.
 * This assumes the servers' clocks are roughly in sync, which they should be anyway.
 */
public final class RedisCooldownSync implements CooldownListener {

    private static final int HASH_TTL_SECONDS = 60 * 60 * 24;

    private final JedisPool pool;
    private final String prefix;
    private final Logger logger;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "PartnerItems-Redis");
        thread.setDaemon(true);
        return thread;
    });

    public RedisCooldownSync(String host, int port, String password, String prefix, Logger logger) {
        this.pool = new JedisPool(host, port, null, password == null || password.isEmpty() ? null : password);
        this.prefix = prefix;
        this.logger = logger;
    }

    public void ping() {
        try (Jedis jedis = pool.getResource()) {
            jedis.ping();
        }
    }

    @Override
    public void onSet(UUID player, String key, long expiresAt) {
        write(jedis -> {
            String hash = hash(player);
            jedis.hset(hash, key, Long.toString(expiresAt));
            jedis.expire(hash, HASH_TTL_SECONDS);
        });
    }

    @Override
    public void onClear(UUID player, String key) {
        write(jedis -> jedis.hdel(hash(player), key));
    }

    @Override
    public void onClearAll(UUID player) {
        write(jedis -> jedis.del(hash(player)));
    }

    /** Blocking, only call this off the main thread. */
    public Map<String, Long> load(UUID player) {
        Map<String, Long> result = new HashMap<>();
        try (Jedis jedis = pool.getResource()) {
            for (Map.Entry<String, String> entry : jedis.hgetAll(hash(player)).entrySet()) {
                try {
                    result.put(entry.getKey(), Long.parseLong(entry.getValue()));
                } catch (NumberFormatException ignored) {
                    // someone put garbage in the hash, skip it
                }
            }
        }
        return result;
    }

    public void close() {
        writer.shutdown();
        try {
            writer.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pool.close();
    }

    private String hash(UUID player) {
        return prefix + player;
    }

    private void write(RedisTask task) {
        writer.execute(() -> {
            try (Jedis jedis = pool.getResource()) {
                task.run(jedis);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to write cooldown to redis", e);
            }
        });
    }

    private interface RedisTask {
        void run(Jedis jedis);
    }
}
