# MeteorEvents

MeteorEvents is a cinematic, performance-optimized meteor event plugin for Minecraft servers, supporting **Minecraft 1.16.5 through 26.x** across **Paper**, **Spigot**, and **Folia**.
Author: **lHawk_** (Contact: https://minebuilders.com.br)

The plugin introduces atmospheric meteor events featuring real-time packet-driven trajectories, custom sounds, visual shockwaves, procedural crater carving with safe terrain rollback, interactive cores, and customizable loot tables.

## Version

Current version: **1.0.1**

## Requirements

| Requirement | Supported Version | Notes |
|---|---|---|
| Minecraft Server | 1.16.5 to 26.x | Spigot, Paper, and Folia |
| Java | Java 8, 11, 17, 21, 25 | Bytecode compiled with release 8 for universal compatibility |
| PacketEvents | 2.14.0+ | Mandatory for packet visuals; graceful fallback if absent |
| WorldGuard | 7.0+ (Optional) | Soft dependency to protect claimed regions |

## Key Features

- **Multi-Stage Event Lifecycle:**
  1. *Omen:* Sky rumblings, localized titles, and atmospheric warnings alerting nearby players.
  2. *Atmospheric Crossing:* Angled flight trajectory rendered using PacketEvents virtual entities with continuous particle and sound trails.
  3. *Impact:* Cinematic particle burst, sound effects, knockback, and procedural crater carving.
  4. *Exploration:* Floating interactive core, loot chest, and fragment nodes scattered across the impact zone.
  5. *Cleanup & Rollback:* Timed entity despawning and automated terrain restoration protecting player-placed blocks.

- **Custom Meteor Types:**
  - **Ember (Common):** Fiery meteor generating magma and basalt craters with standard mining rewards.
  - **Frostfall (Rare):** Glacial meteor with soul fire, blue ice, snow particles, and winter combat equipment.
  - **Voidfall (Epic):** Cosmic purple meteor with dragon breath, crying obsidian, end stone, and rare scrap.
  - **Worldbreaker (Legendary):** Massive apocalyptic strike causing profound crater excavation and ancient debris loot.

- **PacketEvents Visual System:**
  - Flight trajectories and core entities exist exclusively as client-side packets, avoiding unnecessary server-side entity overhead.
  - Selective packet transmission restricted to configurable view distances.

- **Terrain & Protection Integration:**
  - Configurable spherical and hemispherical crater carving.
  - Automated terrain rollback with verification preventing overwrite of player modifications.
  - WorldGuard region detection preventing strikes in claimed areas.

- **Folia Schedulers:**
  - Multi-threaded region scheduler compatibility for chunk and block operations.
  - Global region scheduler for automated event timers and announcements.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/meteorevents start [type]` | `meteorevents.start` | Manually starts a meteor event at the player's position or random world spot. |
| `/meteorevents stop` | `meteorevents.stop` | Immediately cancels all active meteor events and cleans up entities. |
| `/meteorevents next` | `meteorevents.info` | Displays the estimated time until the next automated meteor strike. |
| `/meteorevents types` | `meteorevents.info` | Lists all configured meteor type identifiers. |
| `/meteorevents test [type]` | `meteorevents.test` | Spawns a safe visual meteor test without altering any terrain blocks. |
| `/meteorevents info` | `meteorevents.info` | Displays active meteor events, current stages, and remaining timers. |
| `/meteorevents reload` | `meteorevents.reload` | Reloads all 5 YAML configuration files with validation. |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `meteorevents.admin` | `op` | Grants access to all administrative commands. |
| `meteorevents.start` | `op` | Allows manually starting meteor events. |
| `meteorevents.stop` | `op` | Allows stopping active events. |
| `meteorevents.reload` | `op` | Allows reloading plugin configurations. |
| `meteorevents.test` | `op` | Allows running safe visual test strikes. |
| `meteorevents.info` | `true` | Allows viewing status and upcoming strike timers. |

## Configuration Files

The plugin organizes settings into 5 annotated English YAML files:
- `config.yml`: Core scheduling intervals, probabilities, allowed worlds, and performance view distance limits.
- `meteors.yml`: Meteor definitions, velocities, trajectory angles, particles, crater geometry, and visual models.
- `rewards.yml`: Loot tables, item chances, amounts, custom names, lore, enchantments, and console commands.
- `messages.yml`: Announcements, titles, subtitles, action bar messages, and placeholders.
- `protections.yml`: Terrain alteration toggles, rollback delays, blacklisted blocks, entity damage, and WorldGuard checks.

## Building and Testing

Build the plugin using Maven:
```powershell
mvn clean package
```

Run unit tests:
```powershell
mvn test
```

## Version History

- **1.0.1:** Added author metadata (`lHawk_`) and official contact URL (`https://minebuilders.com.br`) across `plugin.yml`, `pom.xml`, and documentation.
- **1.0.0:** Initial release.
  - Full event lifecycle (Omen, Crossing, Impact, Exploration, Rollback).
  - PacketEvents 2.14.0 integration for virtual falling entities and floating cores.
  - Procedural craters with safe rollback system protecting player-modified blocks.
  - 4 built-in meteor types: Ember, Frostfall, Voidfall, and Worldbreaker.
  - Folia multi-threaded region scheduler and Spigot/Paper compatibility.
  - WorldGuard 7+ soft integration.
