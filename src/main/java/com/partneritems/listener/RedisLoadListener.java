package com.partneritems.listener;

import com.partneritems.PartnerItemsPlugin;
import com.partneritems.cooldown.RedisCooldownSync;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class RedisLoadListener implements Listener {

    private final PartnerItemsPlugin plugin;

    public RedisLoadListener(PartnerItemsPlugin plugin) {
        this.plugin = plugin;
    }

    // Already off the main thread, so it's fine to block on redis here.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        RedisCooldownSync redis = plugin.getRedis();
        if (redis == null || event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        UUID id = event.getUniqueId();
        try {
            for (Map.Entry<String, Long> entry : redis.load(id).entrySet()) {
                plugin.getCooldowns().restore(id, entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            // don't kick anyone just because redis is down
            plugin.getLogger().log(Level.WARNING, "Couldn't load cooldowns for " + event.getName(), e);
        }
    }
}
