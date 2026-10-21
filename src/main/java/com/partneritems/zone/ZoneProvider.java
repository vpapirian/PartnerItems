package com.partneritems.zone;

import org.bukkit.Location;

/**
 * Decides where partner items are blocked. An HCF core can register its own implementation with
 * Bukkit's ServicesManager (priority Normal or higher) to use its real spawn/safezone claims.
 */
public interface ZoneProvider {

    boolean isSafeZone(Location location);
}
