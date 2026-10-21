package com.partneritems.item;

import com.partneritems.PartnerItemsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

/**
 * Thrown like a normal snowball/egg. The cooldown starts when it's thrown (vanilla takes the item),
 * and the effect runs when the projectile hits a player.
 */
public abstract class ProjectileItem extends PartnerItem {

    protected ProjectileItem(PartnerItemsPlugin plugin, String id) {
        super(plugin, id);
    }

    public abstract void onProjectileHit(Player shooter, Player victim, Projectile projectile);
}
