package com.partneritems.item;

import com.partneritems.PartnerItemsPlugin;
import com.partneritems.util.ItemBuilder;
import com.partneritems.util.NbtTag;
import com.partneritems.util.Text;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Base for every partner item. Subclasses extend {@link ClickItem}, {@link HitItem} or
 * {@link ProjectileItem} depending on how the item gets triggered.
 *
 * Shared checks (safezones, cooldowns, consuming the item) are handled by ItemUseService so
 * the items themselves only contain their actual effect.
 */
public abstract class PartnerItem {

    public static final String NBT_KEY = "PartnerItem";

    protected final PartnerItemsPlugin plugin;
    private final String id;

    private boolean enabled;
    private Material material;
    private short data;
    private String displayName;
    private List<String> lore;
    private long cooldownMillis;

    protected PartnerItem(PartnerItemsPlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }

    public final void load(ConfigurationSection section) {
        if (section == null) {
            enabled = false;
            plugin.getLogger().warning("No config section for item '" + id + "', it will be disabled");
            return;
        }

        enabled = section.getBoolean("enabled", true);
        material = Material.matchMaterial(section.getString("material", ""));
        if (material == null) {
            plugin.getLogger().warning("Invalid material for item '" + id + "', disabling it");
            enabled = false;
            material = Material.STONE;
        }
        data = (short) section.getInt("data", 0);
        displayName = Text.color(section.getString("name", id));
        lore = section.getStringList("lore");
        cooldownMillis = section.getLong("cooldown", 30) * 1000L;

        loadSettings(section);
    }

    /** Item-specific config values. */
    protected void loadSettings(ConfigurationSection section) {
    }

    /** Clear any per-player state the item keeps. */
    public void onQuit(UUID player) {
    }

    public ItemStack createItem(int amount) {
        ItemStack item = new ItemBuilder(material, amount, data)
                .name(displayName)
                .lore(lore == null ? Collections.<String>emptyList() : lore)
                .glow()
                .build();
        return NbtTag.setString(item, NBT_KEY, id);
    }

    public String getCooldownKey() {
        return "item:" + id;
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getCooldownMillis() {
        return cooldownMillis;
    }
}
