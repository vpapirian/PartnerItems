package com.partneritems.item.impl;

import com.partneritems.PartnerItemsPlugin;
import com.partneritems.item.HitItem;
import com.partneritems.restriction.Restriction;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Needs a few hits in a row on the same player before it goes off. */
public final class AntiBuildItem extends HitItem {

    private final Map<UUID, Streak> streaks = new HashMap<>();

    private int hitsRequired;
    private long hitWindowMillis;
    private int durationSeconds;

    public AntiBuildItem(PartnerItemsPlugin plugin) {
        super(plugin, "anti-build");
    }

    @Override
    protected void loadSettings(ConfigurationSection section) {
        hitsRequired = Math.max(1, section.getInt("hits-required", 3));
        hitWindowMillis = section.getLong("hit-window", 5) * 1000L;
        durationSeconds = section.getInt("duration", 15);
    }

    @Override
    public boolean onHit(Player attacker, Player victim) {
        long now = System.currentTimeMillis();
        Streak streak = streaks.get(attacker.getUniqueId());

        if (streak == null || !streak.victim.equals(victim.getUniqueId()) || now - streak.lastHit > hitWindowMillis) {
            streak = new Streak(victim.getUniqueId());
            streaks.put(attacker.getUniqueId(), streak);
        }

        streak.hits++;
        streak.lastHit = now;

        if (streak.hits < hitsRequired) {
            plugin.getMessages().send(attacker, "anti-build-progress",
                    "player", victim.getName(), "hits", streak.hits, "required", hitsRequired);
            return false;
        }

        streaks.remove(attacker.getUniqueId());
        plugin.getItemUseService().restrict(victim, Restriction.ANTI_BUILD, durationSeconds * 1000L);

        victim.playSound(victim.getLocation(), Sound.ANVIL_LAND, 1f, 1f);
        plugin.getMessages().send(attacker, "anti-build-hit", "player", victim.getName(), "seconds", durationSeconds);
        plugin.getMessages().send(victim, "anti-build-victim", "seconds", durationSeconds);
        return true;
    }

    @Override
    public void onQuit(UUID player) {
        streaks.remove(player);
    }

    private static final class Streak {

        private final UUID victim;
        private int hits;
        private long lastHit;

        private Streak(UUID victim) {
            this.victim = victim;
        }
    }
}
