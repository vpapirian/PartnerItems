package com.partneritems.item.impl;

import com.partneritems.PartnerItemsPlugin;
import com.partneritems.item.ClickItem;
import com.partneritems.restriction.Restriction;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class RocketItem extends ClickItem {

    private static final long FALL_IMMUNITY_MILLIS = 10_000L;

    private double verticalPower;
    private double forwardPower;

    public RocketItem(PartnerItemsPlugin plugin) {
        super(plugin, "rocket");
    }

    @Override
    protected void loadSettings(ConfigurationSection section) {
        verticalPower = section.getDouble("vertical-power", 1.6);
        forwardPower = section.getDouble("forward-power", 1.2);
    }

    @Override
    public boolean onClick(Player player) {
        Vector direction = player.getLocation().getDirection().setY(0);
        if (direction.lengthSquared() > 0) {
            direction.normalize().multiply(forwardPower);
        }

        player.setVelocity(direction.setY(verticalPower));
        player.setFallDistance(0f);
        plugin.getItemUseService().restrict(player, Restriction.FALL_IMMUNITY, FALL_IMMUNITY_MILLIS);
        player.playSound(player.getLocation(), Sound.FIREWORK_LAUNCH, 1f, 1f);
        return true;
    }
}
