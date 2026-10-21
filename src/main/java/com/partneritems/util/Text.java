package com.partneritems.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public final class Text {

    private Text() {
    }

    public static String color(String input) {
        return input == null ? "" : ChatColor.translateAlternateColorCodes('&', input);
    }

    public static List<String> color(List<String> input) {
        List<String> result = new ArrayList<>(input.size());
        for (String line : input) {
            result.add(color(line));
        }
        return result;
    }
}
