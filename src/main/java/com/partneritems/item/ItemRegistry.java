package com.partneritems.item;

import com.partneritems.util.NbtTag;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ItemRegistry {

    private final Map<String, PartnerItem> items = new LinkedHashMap<>();
    private final Set<Material> materials = EnumSet.noneOf(Material.class);

    public void register(PartnerItem item) {
        if (items.putIfAbsent(item.getId(), item) != null) {
            throw new IllegalArgumentException("Duplicate partner item id: " + item.getId());
        }
    }

    public void loadAll(ConfigurationSection section) {
        materials.clear();
        for (PartnerItem item : items.values()) {
            item.load(section == null ? null : section.getConfigurationSection(item.getId()));
            materials.add(item.getMaterial());
        }
    }

    /** The enabled partner item this stack represents, or null. */
    public PartnerItem find(ItemStack stack) {
        PartnerItem item = identify(stack);
        return item != null && item.isEnabled() ? item : null;
    }

    /** Like find, but also returns disabled items. Used for things like blocking crafting. */
    public PartnerItem identify(ItemStack stack) {
        // Every damage and interact event goes through here, so skip the NBT read for
        // anything that can't possibly be a partner item.
        if (stack == null || !materials.contains(stack.getType()) || !stack.hasItemMeta()) {
            return null;
        }

        String id = NbtTag.getString(stack, PartnerItem.NBT_KEY);
        return id == null ? null : items.get(id);
    }

    public PartnerItem get(String id) {
        return items.get(id.toLowerCase());
    }

    public Collection<PartnerItem> all() {
        return Collections.unmodifiableCollection(items.values());
    }
}
