package com.partneritems.util;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public final class Messages {

    private final Map<String, String> messages = new HashMap<>();
    private String prefix = "";

    public void load(ConfigurationSection section) {
        messages.clear();
        if (section == null) {
            return;
        }

        prefix = Text.color(section.getString("prefix", ""));
        for (String key : section.getKeys(false)) {
            if (section.isList(key)) {
                messages.put(key, Text.color(String.join("\n", section.getStringList(key))));
            } else {
                messages.put(key, Text.color(section.getString(key)));
            }
        }
    }

    /** Placeholders are passed as pairs: get("given", "player", name, "amount", 5) */
    public String get(String key, Object... placeholders) {
        String message = messages.get(key);
        if (message == null) {
            return "Missing message: " + key;
        }

        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            message = message.replace("{" + placeholders[i] + "}", String.valueOf(placeholders[i + 1]));
        }
        return message.replace("{prefix}", prefix);
    }

    /** Empty messages in the config count as disabled. */
    public void send(CommandSender to, String key, Object... placeholders) {
        String message = get(key, placeholders);
        if (!message.isEmpty()) {
            to.sendMessage(message);
        }
    }
}
