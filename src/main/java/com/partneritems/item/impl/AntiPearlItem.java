package com.partneritems.item.impl;

import com.partneritems.PartnerItemsPlugin;
import com.partneritems.item.HitItem;
import com.partneritems.restriction.Restriction;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class AntiPearlItem extends HitItem {

    private int durationSeconds;

    public AntiPearlItem(PartnerItemsPlugin plugin) {
        super(plugin, "anti-pearl");
    }

    @Override
    protected void loadSettings(ConfigurationSection section) {
        durationSeconds = section.getInt("duration", 15);
    }

    @Override
    public boolean onHit(Player attacker, Player victim) {
        plugin.getItemUseService().restrict(victim, Restriction.ANTI_PEARL, durationSeconds * 1000L);

        victim.playSound(victim.getLocation(), Sound.ITEM_BREAK, 1f, 1f);
        plugin.getMessages().send(attacker, "anti-pearl-hit", "player", victim.getName(), "seconds", durationSeconds);
        plugin.getMessages().send(victim, "anti-pearl-victim", "seconds", durationSeconds);
        return true;
    }
}
