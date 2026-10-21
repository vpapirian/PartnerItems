package com.partneritems.event;

import com.partneritems.item.PartnerItem;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Called after the built-in checks pass but before the item does anything. Other plugins can
 * cancel it, for example to block partner items during SOTW, inside KOTH caps, or while frozen.
 */
public class PartnerItemUseEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PartnerItem item;
    private final Player target;
    private boolean cancelled;

    public PartnerItemUseEvent(Player player, PartnerItem item, Player target) {
        super(player);
        this.item = item;
        this.target = target;
    }

    public PartnerItem getItem() {
        return item;
    }

    /** The player being hit, or null for click and thrown items. */
    public Player getTarget() {
        return target;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
