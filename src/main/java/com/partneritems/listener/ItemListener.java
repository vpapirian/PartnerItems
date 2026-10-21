package com.partneritems.listener;

import com.partneritems.PartnerItemsPlugin;
import com.partneritems.item.ClickItem;
import com.partneritems.item.HitItem;
import com.partneritems.item.ItemRegistry;
import com.partneritems.item.ItemUseService;
import com.partneritems.item.PartnerItem;
import com.partneritems.item.ProjectileItem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ItemListener implements Listener {

    private static final String PROJECTILE_META = "partneritems:projectile";

    private final PartnerItemsPlugin plugin;
    private final ItemRegistry registry;
    private final ItemUseService useService;

    // players who just right clicked a projectile item and passed the checks, waiting for the launch event
    private final Map<UUID, ProjectileItem> pendingThrows = new HashMap<>();

    public ItemListener(PartnerItemsPlugin plugin) {
        this.plugin = plugin;
        this.registry = plugin.getRegistry();
        this.useService = plugin.getItemUseService();
    }

    // Not ignoreCancelled: Bukkit fires RIGHT_CLICK_AIR already cancelled.
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        PartnerItem item = registry.find(event.getItem());
        if (item == null) {
            return;
        }

        Player player = event.getPlayer();

        if (item instanceof ProjectileItem) {
            if (!useService.canUse(player, item, null)) {
                event.setCancelled(true);
                return;
            }

            UUID id = player.getUniqueId();
            pendingThrows.put(id, (ProjectileItem) item);
            // if the throw doesn't happen this tick for whatever reason, don't leave it hanging
            plugin.getServer().getScheduler().runTask(plugin, () -> pendingThrows.remove(id));
            return;
        }

        // stops vanilla behaviour like fireworks launching or blocks being placed
        event.setCancelled(true);

        if (!(item instanceof ClickItem) || !useService.canUse(player, item, null)) {
            return;
        }

        if (((ClickItem) item).onClick(player)) {
            useService.markUsed(player, item);
            useService.consumeOne(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }

        Player shooter = (Player) projectile.getShooter();
        ProjectileItem item = pendingThrows.remove(shooter.getUniqueId());
        if (item == null) {
            return;
        }

        projectile.setMetadata(PROJECTILE_META, new FixedMetadataValue(plugin, item.getId()));
        useService.markUsed(shooter, item);
    }

    // HIGHEST + ignoreCancelled so that if an HCF core cancels the hit (teammates, pvp timer, etc.)
    // the item doesn't go off.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Entity damager = event.getDamager();

        if (damager instanceof Projectile) {
            handleProjectileHit((Projectile) damager, victim);
            return;
        }
        if (!(damager instanceof Player)) {
            return;
        }

        Player attacker = (Player) damager;
        PartnerItem item = registry.find(attacker.getItemInHand());
        if (!(item instanceof HitItem) || !useService.canUse(attacker, item, victim)) {
            return;
        }

        if (((HitItem) item).onHit(attacker, victim)) {
            useService.markUsed(attacker, item);
            useService.consumeOne(attacker);
        }
    }

    private void handleProjectileHit(Projectile projectile, Player victim) {
        String id = null;
        for (MetadataValue value : projectile.getMetadata(PROJECTILE_META)) {
            if (value.getOwningPlugin() == plugin) {
                id = value.asString();
                break;
            }
        }
        if (id == null || !(projectile.getShooter() instanceof Player)) {
            return;
        }

        Player shooter = (Player) projectile.getShooter();
        PartnerItem item = registry.get(id);
        if (!(item instanceof ProjectileItem) || !shooter.isOnline() || shooter.equals(victim)) {
            return;
        }

        if (useService.isSafeZone(victim.getLocation())) {
            plugin.getMessages().send(shooter, "target-safezone");
            return;
        }

        ((ProjectileItem) item).onProjectileHit(shooter, victim, projectile);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        // some partner items are blocks (packed ice etc), don't let them get placed
        if (registry.identify(event.getItemInHand()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (registry.identify(ingredient) != null) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        pendingThrows.remove(id);
        plugin.getCombatTracker().forget(id);
        for (PartnerItem item : registry.all()) {
            item.onQuit(id);
        }
    }
}
