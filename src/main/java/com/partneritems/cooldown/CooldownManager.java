package com.partneritems.cooldown;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Tracks per-player cooldowns by string key. Doesn't touch the Bukkit API so it can be unit tested.
 *
 * Entries are kept when a player logs out on purpose, otherwise relogging would reset cooldowns.
 * Expired entries get cleaned up by {@link #prune()} instead.
 */
public final class CooldownManager {

    private final Map<UUID, Map<String, Long>> expiries = new ConcurrentHashMap<>();
    private final LongSupplier clock;
    private CooldownListener listener;

    public CooldownManager() {
        this(System::currentTimeMillis);
    }

    public CooldownManager(LongSupplier clock) {
        this.clock = clock;
    }

    public void setListener(CooldownListener listener) {
        this.listener = listener;
    }

    public void set(UUID player, String key, long durationMillis) {
        if (durationMillis <= 0) {
            clear(player, key);
            return;
        }

        long expiresAt = clock.getAsLong() + durationMillis;
        expiries.computeIfAbsent(player, id -> new ConcurrentHashMap<>()).put(key, expiresAt);

        if (listener != null) {
            listener.onSet(player, key, expiresAt);
        }
    }

    /**
     * Loads an expiry from storage. Only ever extends an existing cooldown and doesn't notify the listener,
     * since the value came from the listener's storage in the first place.
     */
    public void restore(UUID player, String key, long expiresAt) {
        if (expiresAt <= clock.getAsLong()) {
            return;
        }
        expiries.computeIfAbsent(player, id -> new ConcurrentHashMap<>()).merge(key, expiresAt, Math::max);
    }

    public long getRemaining(UUID player, String key) {
        Map<String, Long> map = expiries.get(player);
        if (map == null) {
            return 0L;
        }

        Long expiresAt = map.get(key);
        if (expiresAt == null) {
            return 0L;
        }

        long remaining = expiresAt - clock.getAsLong();
        if (remaining <= 0) {
            map.remove(key);
            return 0L;
        }
        return remaining;
    }

    public boolean isActive(UUID player, String key) {
        return getRemaining(player, key) > 0;
    }

    public void clear(UUID player, String key) {
        Map<String, Long> map = expiries.get(player);
        if (map != null && map.remove(key) != null && listener != null) {
            listener.onClear(player, key);
        }
    }

    public void clearAll(UUID player) {
        if (expiries.remove(player) != null && listener != null) {
            listener.onClearAll(player);
        }
    }

    /** Remaining time for every active cooldown the player has. */
    public Map<String, Long> getActive(UUID player) {
        Map<String, Long> map = expiries.get(player);
        if (map == null) {
            return Collections.emptyMap();
        }

        long now = clock.getAsLong();
        Map<String, Long> active = new HashMap<>();
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            long remaining = entry.getValue() - now;
            if (remaining > 0) {
                active.put(entry.getKey(), remaining);
            }
        }
        return active;
    }

    public void prune() {
        long now = clock.getAsLong();
        Iterator<Map<String, Long>> players = expiries.values().iterator();
        while (players.hasNext()) {
            Map<String, Long> map = players.next();
            map.values().removeIf(expiresAt -> expiresAt <= now);
            if (map.isEmpty()) {
                players.remove();
            }
        }
    }

    public int trackedPlayers() {
        return expiries.size();
    }
}
