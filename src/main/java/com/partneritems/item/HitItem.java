package com.partneritems.item;

import com.partneritems.PartnerItemsPlugin;
import org.bukkit.entity.Player;

/** Triggered by hitting another player while holding the item. */
public abstract class HitItem extends PartnerItem {

    protected HitItem(PartnerItemsPlugin plugin, String id) {
        super(plugin, id);
    }

    /** @return true if the item was actually used (cooldown starts and one item is taken). */
    public abstract boolean onHit(Player attacker, Player victim);
}
