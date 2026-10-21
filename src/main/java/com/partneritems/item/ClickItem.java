package com.partneritems.item;

import com.partneritems.PartnerItemsPlugin;
import org.bukkit.entity.Player;

/** Triggered by right clicking. */
public abstract class ClickItem extends PartnerItem {

    protected ClickItem(PartnerItemsPlugin plugin, String id) {
        super(plugin, id);
    }

    /** @return true if the item was actually used (cooldown starts and one item is taken). */
    public abstract boolean onClick(Player player);
}
