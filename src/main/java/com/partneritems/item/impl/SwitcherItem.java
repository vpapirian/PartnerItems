package com.partneritems.item.impl;

import com.partneritems.PartnerItemsPlugin;
import com.partneritems.item.ProjectileItem;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public final class SwitcherItem extends ProjectileItem {

    private double range;

    public SwitcherItem(PartnerItemsPlugin plugin) {
        super(plugin, "switcher");
    }

    @Override
    protected void loadSettings(ConfigurationSection section) {
        range = section.getDouble("range", 8);
    }

    @Override
    public void onProjectileHit(Player shooter, Player victim, Projectile projectile) {
        if (!shooter.getWorld().equals(victim.getWorld())
                || shooter.getLocation().distanceSquared(victim.getLocation()) > range * range) {
            plugin.getMessages().send(shooter, "switcher-out-of-range");
            return;
        }

        Location shooterLoc = shooter.getLocation();
        Location victimLoc = victim.getLocation();

        // swap positions but let both players keep looking where they were looking
        Location shooterTarget = victimLoc.clone();
        shooterTarget.setYaw(shooterLoc.getYaw());
        shooterTarget.setPitch(shooterLoc.getPitch());

        Location victimTarget = shooterLoc.clone();
        victimTarget.setYaw(victimLoc.getYaw());
        victimTarget.setPitch(victimLoc.getPitch());

        shooter.teleport(shooterTarget, TeleportCause.PLUGIN);
        victim.teleport(victimTarget, TeleportCause.PLUGIN);

        shooter.playSound(shooterTarget, Sound.ENDERMAN_TELEPORT, 1f, 1f);
        victim.playSound(victimTarget, Sound.ENDERMAN_TELEPORT, 1f, 1f);

        plugin.getMessages().send(shooter, "switcher-switched", "player", victim.getName());
        plugin.getMessages().send(victim, "switcher-victim", "player", shooter.getName());
    }
}
