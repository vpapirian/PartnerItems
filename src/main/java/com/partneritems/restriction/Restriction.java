package com.partneritems.restriction;

/**
 * Temporary debuffs put on players by partner items. They're stored in the same CooldownManager
 * as item cooldowns, so they also persist through relogs and sync over redis.
 */
public enum Restriction {

    ANTI_PEARL,
    ANTI_BUILD,
    FALL_IMMUNITY;

    private final String key = "restriction:" + name().toLowerCase();

    public String key() {
        return key;
    }
}
