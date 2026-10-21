package com.partneritems.cooldown;

import java.util.UUID;

public interface CooldownListener {

    void onSet(UUID player, String key, long expiresAt);

    void onClear(UUID player, String key);

    void onClearAll(UUID player);
}
