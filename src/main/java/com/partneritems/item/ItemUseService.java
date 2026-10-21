package com.partneritems.item;

import com.partneritems.PartnerItemsPlugin;
import com.partneritems.cooldown.CooldownManager;
import com.partneritems.event.PartnerItemUseEvent;
import com.partneritems.restriction.Restriction;
import com.partneritems.util.Messages;
import com.partneritems.util.TimeFormat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ItemUseService {

    public static final String GLOBAL_KEY = "partner:global";

    private final PartnerItemsPlugin plugin;
    private long globalCooldownMillis;

    public ItemUseService(PartnerItemsPlugin plugin) {
        this.plugin = plugin;
    }

    public void setGlobalCooldown(long millis) {
        this.globalCooldownMillis = millis;
    }

    /**
     * Runs every check before an item is allowed to fire and tells the player why if it isn't.
     *
     * @param target the player being hit, or null
     */
    public boolean canUse(Player player, PartnerItem item, Player target) {
        Messages messages = plugin.getMessages();
        CooldownManager cooldowns = plugin.getCooldowns();

        if (isSafeZone(player.getLocation())) {
            messages.send(player, "safezone");
            return false;
        }
        if (target != null && isSafeZone(target.getLocation())) {
            messages.send(player, "target-safezone");
            return false;
        }

        long global = cooldowns.getRemaining(player.getUniqueId(), GLOBAL_KEY);
        if (global > 0) {
            messages.send(player, "global-cooldown", "time", TimeFormat.format(global));
            return false;
        }

        long own = cooldowns.getRemaining(player.getUniqueId(), item.getCooldownKey());
        if (own > 0) {
            messages.send(player, "item-cooldown", "item", item.getDisplayName(), "time", TimeFormat.format(own));
            return false;
        }

        PartnerItemUseEvent event = new PartnerItemUseEvent(player, item, target);
        plugin.getServer().getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    public void markUsed(Player player, PartnerItem item) {
        CooldownManager cooldowns = plugin.getCooldowns();
        cooldowns.set(player.getUniqueId(), item.getCooldownKey(), item.getCooldownMillis());
        cooldowns.set(player.getUniqueId(), GLOBAL_KEY, globalCooldownMillis);
        plugin.getMessages().send(player, "item-used", "item", item.getDisplayName());
    }

    public void consumeOne(Player player) {
        ItemStack hand = player.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR) {
            return;
        }

        if (hand.getAmount() <= 1) {
            player.setItemInHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
        player.updateInventory();
    }

    public boolean isSafeZone(Location location) {
        return plugin.getZoneProvider().isSafeZone(location);
    }

    public void restrict(Player player, Restriction restriction, long millis) {
        plugin.getCooldowns().set(player.getUniqueId(), restriction.key(), millis);
    }

    public long getRestriction(Player player, Restriction restriction) {
        return plugin.getCooldowns().getRemaining(player.getUniqueId(), restriction.key());
    }
}
