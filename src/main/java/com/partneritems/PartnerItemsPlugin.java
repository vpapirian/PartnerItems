package com.partneritems;

import com.partneritems.combat.CombatTracker;
import com.partneritems.command.PartnerItemCommand;
import com.partneritems.cooldown.CooldownManager;
import com.partneritems.cooldown.RedisCooldownSync;
import com.partneritems.item.ItemRegistry;
import com.partneritems.item.ItemUseService;
import com.partneritems.item.impl.AntiBuildItem;
import com.partneritems.item.impl.AntiPearlItem;
import com.partneritems.item.impl.FreezerItem;
import com.partneritems.item.impl.NinjaStarItem;
import com.partneritems.item.impl.PortableEffectItem;
import com.partneritems.item.impl.RocketItem;
import com.partneritems.item.impl.SwitcherItem;
import com.partneritems.listener.CombatListener;
import com.partneritems.listener.ItemListener;
import com.partneritems.listener.RedisLoadListener;
import com.partneritems.listener.RestrictionListener;
import com.partneritems.util.Messages;
import com.partneritems.util.NbtTag;
import com.partneritems.zone.SpawnRadiusZoneProvider;
import com.partneritems.zone.ZoneProvider;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class PartnerItemsPlugin extends JavaPlugin {

    private final CooldownManager cooldowns = new CooldownManager();
    private final CombatTracker combatTracker = new CombatTracker();
    private final ItemRegistry registry = new ItemRegistry();
    private final Messages messages = new Messages();
    private final SpawnRadiusZoneProvider defaultZones = new SpawnRadiusZoneProvider();

    private ItemUseService itemUseService;
    private ZoneProvider zoneProvider = defaultZones;
    private RedisCooldownSync redis;

    @Override
    public void onEnable() {
        try {
            NbtTag.init();
        } catch (ReflectiveOperationException e) {
            getLogger().log(Level.SEVERE, "This server version isn't supported (couldn't hook into NBT)", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        itemUseService = new ItemUseService(this);

        registerItems();
        reloadSettings();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ItemListener(this), this);
        pm.registerEvents(new RestrictionListener(this), this);
        pm.registerEvents(new CombatListener(combatTracker), this);
        pm.registerEvents(new RedisLoadListener(this), this);

        PartnerItemCommand command = new PartnerItemCommand(this);
        PluginCommand pluginCommand = getCommand("partneritem");
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        // Registered at the lowest priority so an HCF core can register its own and win.
        // Resolved a tick later so plugins that enable after this one get a chance first.
        getServer().getServicesManager().register(ZoneProvider.class, defaultZones, this, ServicePriority.Lowest);
        getServer().getScheduler().runTask(this, this::resolveZoneProvider);

        // cooldowns stay in memory through relogs, so expired ones need clearing out every so often
        getServer().getScheduler().runTaskTimerAsynchronously(this, cooldowns::prune, 20L * 60, 20L * 60);
    }

    @Override
    public void onDisable() {
        closeRedis();
    }

    private void registerItems() {
        registry.register(new SwitcherItem(this));
        registry.register(new NinjaStarItem(this));
        registry.register(new AntiPearlItem(this));
        registry.register(new AntiBuildItem(this));
        registry.register(new PortableEffectItem(this, "strength"));
        registry.register(new PortableEffectItem(this, "resistance"));
        registry.register(new RocketItem(this));
        registry.register(new FreezerItem(this));
    }

    public void reloadSettings() {
        reloadConfig();
        FileConfiguration config = getConfig();

        messages.load(config.getConfigurationSection("messages"));
        registry.loadAll(config.getConfigurationSection("items"));
        defaultZones.load(config.getConfigurationSection("spawn-protection"));
        itemUseService.setGlobalCooldown(config.getLong("global-cooldown", 10) * 1000L);

        closeRedis();
        ConfigurationSection redisConfig = config.getConfigurationSection("redis");
        if (redisConfig != null && redisConfig.getBoolean("enabled")) {
            connectRedis(redisConfig);
        }

        resolveZoneProvider();
    }

    private void connectRedis(ConfigurationSection section) {
        RedisCooldownSync sync = new RedisCooldownSync(
                section.getString("host", "localhost"),
                section.getInt("port", 6379),
                section.getString("password", ""),
                section.getString("key-prefix", "partneritems:cd:"),
                getLogger());

        try {
            sync.ping();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Couldn't connect to redis, cooldowns will only be kept locally", e);
            sync.close();
            return;
        }

        redis = sync;
        cooldowns.setListener(sync);
        getLogger().info("Connected to redis, cooldowns will be synced");
    }

    private void closeRedis() {
        if (redis != null) {
            cooldowns.setListener(null);
            redis.close();
            redis = null;
        }
    }

    private void resolveZoneProvider() {
        ZoneProvider provider = getServer().getServicesManager().load(ZoneProvider.class);
        zoneProvider = provider == null ? defaultZones : provider;
    }

    public CooldownManager getCooldowns() {
        return cooldowns;
    }

    public CombatTracker getCombatTracker() {
        return combatTracker;
    }

    public ItemRegistry getRegistry() {
        return registry;
    }

    public Messages getMessages() {
        return messages;
    }

    public ItemUseService getItemUseService() {
        return itemUseService;
    }

    public ZoneProvider getZoneProvider() {
        return zoneProvider;
    }

    public RedisCooldownSync getRedis() {
        return redis;
    }
}
