package com.partneritems.item.impl;

import com.partneritems.PartnerItemsPlugin;
import com.partneritems.item.HitItem;
import com.partneritems.util.Effects;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.Collections;
import java.util.List;

public final class FreezerItem extends HitItem {

    private List<PotionEffect> effects = Collections.emptyList();

    public FreezerItem(PartnerItemsPlugin plugin) {
        super(plugin, "freezer");
    }

    @Override
    protected void loadSettings(ConfigurationSection section) {
        effects = Effects.parse(section.getStringList("effects"), plugin.getLogger());
    }

    @Override
    public boolean onHit(Player attacker, Player victim) {
        if (effects.isEmpty()) {
            return false;
        }

        for (PotionEffect effect : effects) {
            victim.addPotionEffect(effect, true);
        }
        victim.playSound(victim.getLocation(), Sound.GLASS, 1f, 1f);
        plugin.getMessages().send(attacker, "freezer-hit", "player", victim.getName());
        plugin.getMessages().send(victim, "freezer-victim", "player", attacker.getName());
        return true;
    }
}
