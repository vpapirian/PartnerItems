package com.partneritems.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Reads and writes string tags on an item's NBT through reflection.
 *
 * 1.8 has no PersistentDataContainer, and matching items by display name means anyone with an
 * anvil can make a fake one, so the item id goes in the NBT instead. Reflection keeps the plugin
 * from depending on one exact craftbukkit version at compile time.
 */
public final class NbtTag {

    private static Method asNmsCopy;
    private static Method asBukkitCopy;
    private static Method getTag;
    private static Method setTag;
    private static Constructor<?> newCompound;
    private static Method setString;
    private static Method getString;
    private static Method hasKey;

    private NbtTag() {
    }

    public static void init() throws ReflectiveOperationException {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);

        Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
        Class<?> nmsItemStack = Class.forName("net.minecraft.server." + version + ".ItemStack");
        Class<?> compound = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");

        asNmsCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
        asBukkitCopy = craftItemStack.getMethod("asBukkitCopy", nmsItemStack);
        getTag = nmsItemStack.getMethod("getTag");
        setTag = nmsItemStack.getMethod("setTag", compound);
        newCompound = compound.getConstructor();
        setString = compound.getMethod("setString", String.class, String.class);
        getString = compound.getMethod("getString", String.class);
        hasKey = compound.getMethod("hasKey", String.class);
    }

    /** Returns a copy of the item with the tag set. */
    public static ItemStack setString(ItemStack item, String key, String value) {
        try {
            Object nms = asNmsCopy.invoke(null, item);
            Object tag = getTag.invoke(nms);
            if (tag == null) {
                tag = newCompound.newInstance();
            }
            setString.invoke(tag, key, value);
            setTag.invoke(nms, tag);
            return (ItemStack) asBukkitCopy.invoke(null, nms);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't write NBT tag " + key, e);
        }
    }

    /** Returns the tag value, or null if the item doesn't have it. */
    public static String getString(ItemStack item, String key) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        try {
            Object nms = asNmsCopy.invoke(null, item);
            if (nms == null) {
                return null;
            }

            Object tag = getTag.invoke(nms);
            if (tag == null || !(Boolean) hasKey.invoke(tag, key)) {
                return null;
            }
            return (String) getString.invoke(tag, key);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't read NBT tag " + key, e);
        }
    }
}
