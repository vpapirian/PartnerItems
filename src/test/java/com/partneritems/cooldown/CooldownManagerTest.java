package com.partneritems.cooldown;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CooldownManagerTest {

    private final UUID player = UUID.randomUUID();
    private AtomicLong now;
    private CooldownManager cooldowns;

    @Before
    public void setUp() {
        now = new AtomicLong(1_000_000L);
        cooldowns = new CooldownManager(now::get);
    }

    @Test
    public void remainingCountsDown() {
        cooldowns.set(player, "item:switcher", 15_000L);
        assertEquals(15_000L, cooldowns.getRemaining(player, "item:switcher"));

        now.addAndGet(5_000L);
        assertEquals(10_000L, cooldowns.getRemaining(player, "item:switcher"));
    }

    @Test
    public void expiresAfterDuration() {
        cooldowns.set(player, "item:switcher", 15_000L);
        now.addAndGet(15_000L);

        assertFalse(cooldowns.isActive(player, "item:switcher"));
        assertEquals(0L, cooldowns.getRemaining(player, "item:switcher"));
    }

    @Test
    public void keysAreIndependent() {
        cooldowns.set(player, "item:switcher", 15_000L);
        assertFalse(cooldowns.isActive(player, "item:rocket"));
        assertFalse(cooldowns.isActive(UUID.randomUUID(), "item:switcher"));
    }

    @Test
    public void zeroDurationClears() {
        cooldowns.set(player, "partner:global", 10_000L);
        cooldowns.set(player, "partner:global", 0L);
        assertFalse(cooldowns.isActive(player, "partner:global"));
    }

    @Test
    public void restoreOnlyExtends() {
        cooldowns.set(player, "item:rocket", 30_000L);

        cooldowns.restore(player, "item:rocket", now.get() + 5_000L);
        assertEquals(30_000L, cooldowns.getRemaining(player, "item:rocket"));

        cooldowns.restore(player, "item:rocket", now.get() + 60_000L);
        assertEquals(60_000L, cooldowns.getRemaining(player, "item:rocket"));
    }

    @Test
    public void restoreIgnoresExpiredValues() {
        cooldowns.restore(player, "item:rocket", now.get() - 1);
        assertFalse(cooldowns.isActive(player, "item:rocket"));
        assertEquals(0, cooldowns.trackedPlayers());
    }

    @Test
    public void pruneRemovesExpiredPlayers() {
        cooldowns.set(player, "item:switcher", 1_000L);
        cooldowns.set(UUID.randomUUID(), "item:switcher", 60_000L);
        now.addAndGet(2_000L);

        cooldowns.prune();
        assertEquals(1, cooldowns.trackedPlayers());
    }

    @Test
    public void getActiveSkipsExpired() {
        cooldowns.set(player, "a", 1_000L);
        cooldowns.set(player, "b", 10_000L);
        now.addAndGet(5_000L);

        assertEquals(1, cooldowns.getActive(player).size());
        assertTrue(cooldowns.getActive(player).containsKey("b"));
    }

    @Test
    public void listenerIsNotified() {
        List<String> calls = new ArrayList<>();
        cooldowns.setListener(new CooldownListener() {
            @Override
            public void onSet(UUID id, String key, long expiresAt) {
                calls.add("set " + key + " " + expiresAt);
            }

            @Override
            public void onClear(UUID id, String key) {
                calls.add("clear " + key);
            }

            @Override
            public void onClearAll(UUID id) {
                calls.add("clearAll");
            }
        });

        cooldowns.set(player, "x", 1_000L);
        cooldowns.restore(player, "y", now.get() + 1_000L);
        cooldowns.clear(player, "x");
        cooldowns.clear(player, "missing");
        cooldowns.clearAll(player);

        assertEquals(3, calls.size());
        assertEquals("set x " + (now.get() + 1_000L), calls.get(0));
        assertEquals("clear x", calls.get(1));
        assertEquals("clearAll", calls.get(2));
    }
}
