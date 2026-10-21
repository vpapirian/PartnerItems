package com.partneritems.util;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class Effects {

    private Effects() {
    }

    /** Parses entries like "INCREASE_DAMAGE:1:5" (type, amplifier, seconds). Bad entries are logged and skipped. */
    public static List<PotionEffect> parse(List<String> entries, Logger logger) {
        List<PotionEffect> effects = new ArrayList<>();
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length != 3) {
                logger.warning("Invalid effect '" + entry + "', expected TYPE:amplifier:seconds");
                continue;
            }

            PotionEffectType type = PotionEffectType.getByName(parts[0].trim().toUpperCase());
            if (type == null) {
                logger.warning("Unknown potion effect type '" + parts[0] + "'");
                continue;
            }

            try {
                int amplifier = Integer.parseInt(parts[1].trim());
                int seconds = Integer.parseInt(parts[2].trim());
                effects.add(new PotionEffect(type, seconds * 20, amplifier));
            } catch (NumberFormatException e) {
                logger.warning("Invalid numbers in effect '" + entry + "'");
            }
        }
        return effects;
    }
}
