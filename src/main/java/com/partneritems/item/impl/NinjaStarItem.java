package com.partneritems.item.impl;

import com.partneritems.PartnerItemsPlugin;
import com.partneritems.item.ClickItem;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public final class NinjaStarItem extends ClickItem {

    private long lastHitWindowMillis;
    private int delaySeconds;

    public NinjaStarItem(PartnerItemsPlugin plugin) {
        super(plugin, "ninja-star");
    }

    @Override
    protected void loadSettings(ConfigurationSection section) {
        lastHitWindowMillis = section.getLong("last-hit-window", 15) * 1000L;
        delaySeconds = Math.max(0, section.getInt("delay", 3));
    }

    @Override
    public boolean onClick(Player player) {
        UUID attackerId = plugin.getCombatTracker().getRecentAttacker(player.getUniqueId(), lastHitWindowMillis);
        if (attackerId == null) {
            plugin.getMessages().send(player, "ninja-star-no-target", "seconds", lastHitWindowMillis / 1000);
            return false;
        }

        Player target = plugin.getServer().getPlayer(attackerId);
        if (target == null || target.isDead() || !target.getWorld().equals(player.getWorld())) {
            plugin.getMessages().send(player, "ninja-star-offline");
            return false;
        }

        plugin.getMessages().send(player, "ninja-star-teleporting", "player", target.getName(), "seconds", delaySeconds);
        plugin.getMessages().send(target, "ninja-star-incoming", "player", player.getName(), "seconds", delaySeconds);
        target.playSound(target.getLocation(), Sound.NOTE_PLING, 1f, 0.5f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    return;
                }
                if (!target.isOnline() || target.isDead() || !target.getWorld().equals(player.getWorld())
                        || plugin.getItemUseService().isSafeZone(target.getLocation())) {
                    plugin.getMessages().send(player, "ninja-star-cancelled");
                    return;
                }

                player.teleport(target.getLocation(), TeleportCause.PLUGIN);
                player.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1f, 1f);
            }
        }.runTaskLater(plugin, delaySeconds * 20L);

        return true;
    }
}
