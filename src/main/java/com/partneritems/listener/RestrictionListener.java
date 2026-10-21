package com.partneritems.listener;

import com.partneritems.PartnerItemsPlugin;
import com.partneritems.item.ItemUseService;
import com.partneritems.restriction.Restriction;
import com.partneritems.util.TimeFormat;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/** Enforces the anti-pearl, anti-build and rocket fall immunity effects. */
public final class RestrictionListener implements Listener {

    private final PartnerItemsPlugin plugin;
    private final ItemUseService useService;

    public RestrictionListener(PartnerItemsPlugin plugin) {
        this.plugin = plugin;
        this.useService = plugin.getItemUseService();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.ENDER_PEARL) {
                long remaining = useService.getRestriction(player, Restriction.ANTI_PEARL);
                if (remaining > 0) {
                    event.setCancelled(true);
                    player.updateInventory();
                    plugin.getMessages().send(player, "anti-pearl-blocked", "time", TimeFormat.format(remaining));
                    return;
                }
            }
        }

        if (action == Action.RIGHT_CLICK_BLOCK && isInteractable(event.getClickedBlock())) {
            denyIfAntiBuild(player, () -> event.setCancelled(true));
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        denyIfAntiBuild(event.getPlayer(), () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        denyIfAntiBuild(event.getPlayer(), () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (useService.getRestriction(player, Restriction.FALL_IMMUNITY) > 0) {
            event.setCancelled(true);
            plugin.getCooldowns().clear(player.getUniqueId(), Restriction.FALL_IMMUNITY.key());
        }
    }

    private void denyIfAntiBuild(Player player, Runnable cancel) {
        long remaining = useService.getRestriction(player, Restriction.ANTI_BUILD);
        if (remaining > 0) {
            cancel.run();
            plugin.getMessages().send(player, "anti-build-blocked", "time", TimeFormat.format(remaining));
        }
    }

    private boolean isInteractable(Block block) {
        if (block == null) {
            return false;
        }
        // name check covers every wood type of door/gate without listing them all
        String name = block.getType().name();
        return name.contains("DOOR") || name.contains("FENCE_GATE")
                || name.contains("CHEST") || name.equals("LEVER") || name.contains("BUTTON");
    }
}
