package com.partneritems.item.impl;

import com.partneritems.PartnerItemsPlugin;
import com.partneritems.item.ClickItem;
import com.partneritems.util.Effects;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.Collections;
import java.util.List;

/** Gives the user a configurable set of potion effects. Used for both the strength and resistance portables. */
public final class PortableEffectItem extends ClickItem {

    private List<PotionEffect> effects = Collections.emptyList();

    public PortableEffectItem(PartnerItemsPlugin plugin, String id) {
        super(plugin, id);
    }

    @Override
    protected void loadSettings(ConfigurationSection section) {
        effects = Effects.parse(section.getStringList("effects"), plugin.getLogger());
    }

    @Override
    public boolean onClick(Player player) {
        if (effects.isEmpty()) {
            return false;
        }

        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect, true);
        }
        player.playSound(player.getLocation(), Sound.DRINK, 1f, 1f);
        return true;
    }
}
