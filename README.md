# PartnerItems

HCF partner items for Spigot 1.8.8. Every item is configurable, has its own cooldown plus a shared
partner item cooldown, and can be synced across servers with Redis.

## Items

| Item | Trigger | What it does |
|------|---------|--------------|
| Switcher | Throw | Swap places with the player you hit (within range) |
| Ninja Star | Right click | Teleport to whoever last hit you after a short warning |
| Anti-Pearl | Hit | Blocks the target's enderpearls for a while |
| Anti-Build Bone | Hit x3 | Target can't place, break, or open doors/gates/chests |
| Strength Portable | Right click | Short strength burst |
| Resistance Portable | Right click | Short resistance burst |
| Rocket | Right click | Launches you up and forward, next fall is free |
| Freezer | Hit | Slowness + weakness on the target |

## Building

Needs JDK 17 or newer to build (the output still runs on Java 8).

```
./gradlew build
```

The jar ends up in `build/libs/PartnerItems-1.0.0.jar`.

## Commands

| Command | Permission |
|---------|------------|
| `/pi give <player> <item\|all> [amount]` | `partneritems.admin` |
| `/pi list` | `partneritems.admin` |
| `/pi cooldowns [player]` | `partneritems.cooldowns` for yourself, admin for others |
| `/pi resetcd <player>` | `partneritems.admin` |
| `/pi reload` | `partneritems.admin` |

## How it works

- Items are identified by an NBT tag, not by name or lore, so renaming something in an anvil
  doesn't make a working partner item. NBT is accessed with reflection since 1.8 doesn't have
  PersistentDataContainer.
- Before reading NBT the material is checked against the set of partner item materials, so normal
  hits and clicks don't pay for the lookup.
- Cooldowns aren't cleared on logout, so relogging doesn't reset them. Expired entries are pruned
  once a minute instead.
- Hit items listen at `HIGHEST` with `ignoreCancelled`, so if your core cancels a hit
  (teammates, PvP timer, safezone) the item doesn't fire.
- Partner items can't be placed (some are blocks) or used in crafting recipes.

### Redis

With `redis.enabled: true` cooldowns and restrictions are stored in one hash per player. Gameplay
always reads from memory; Redis is written to from a single background thread and only read in
`AsyncPlayerPreLoginEvent`, so the main thread never waits on it. If Redis is down players can
still join, cooldowns just stay local.

## Hooking into an HCF core

**Safezones.** By default items are blocked within a square around each world's spawn
(`spawn-protection` in the config). To use your core's claims instead, register a `ZoneProvider`:

```java
getServer().getServicesManager().register(ZoneProvider.class,
        location -> claimManager.isSafeZone(location), this, ServicePriority.Normal);
```

**Blocking items in other situations.** Cancel `PartnerItemUseEvent`, e.g. during SOTW or inside a
KOTH cap zone:

```java
@EventHandler
public void onPartnerItem(PartnerItemUseEvent event) {
    if (sotw.isActive()) {
        event.setCancelled(true);
    }
}
```

## Adding a new item

1. Extend `ClickItem`, `HitItem` or `ProjectileItem` and implement the trigger method. Return
   `true` when the item actually did something so the cooldown starts and one item is taken.
2. Read any extra settings in `loadSettings`.
3. Register it in `PartnerItemsPlugin#registerItems` and add a section for it under `items` in
   `config.yml`.

Cooldown checks, safezone checks and consuming the item are all handled outside the item class.

## Tests

```
./gradlew test
```

Covers the cooldown manager and time formatting, which don't depend on Bukkit.
