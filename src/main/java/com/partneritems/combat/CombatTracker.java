package com.partneritems.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Remembers who last hit each player, for the Ninja Star. */
public final class CombatTracker {

    private final Map<UUID, LastHit> lastHits = new ConcurrentHashMap<>();

    public void record(UUID victim, UUID attacker) {
        lastHits.put(victim, new LastHit(attacker, System.currentTimeMillis()));
    }

    /** The last attacker if they hit within the window, otherwise null. */
    public UUID getRecentAttacker(UUID victim, long windowMillis) {
        LastHit hit = lastHits.get(victim);
        if (hit == null || System.currentTimeMillis() - hit.time > windowMillis) {
            return null;
        }
        return hit.attacker;
    }

    public void forget(UUID player) {
        lastHits.remove(player);
    }

    private static final class LastHit {

        private final UUID attacker;
        private final long time;

        private LastHit(UUID attacker, long time) {
            this.attacker = attacker;
            this.time = time;
        }
    }
}
