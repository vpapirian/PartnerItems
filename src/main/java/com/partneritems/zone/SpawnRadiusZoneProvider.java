package com.partneritems.zone;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/** Fallback used when no HCF core provides zones: a square around each configured world's spawn. */
public final class SpawnRadiusZoneProvider implements ZoneProvider {

    private final Map<String, Integer> radii = new HashMap<>();

    public void load(ConfigurationSection section) {
        radii.clear();
        if (section == null) {
            return;
        }
        for (String world : section.getKeys(false)) {
            radii.put(world.toLowerCase(), section.getInt(world));
        }
    }

    @Override
    public boolean isSafeZone(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        Integer radius = radii.get(world.getName().toLowerCase());
        if (radius == null || radius <= 0) {
            return false;
        }

        Location spawn = world.getSpawnLocation();
        return Math.abs(location.getBlockX() - spawn.getBlockX()) <= radius
                && Math.abs(location.getBlockZ() - spawn.getBlockZ()) <= radius;
    }
}
