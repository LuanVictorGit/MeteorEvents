# MeteorEvents

MeteorEvents is a cinematic, performance-optimized meteor event plugin for Minecraft servers, supporting **Minecraft 1.16.5 through 26.x** across **Paper**, **Spigot**, and **Folia**.
Author: **lHawk_** (Contact: https://minebuilders.com.br)

The plugin introduces atmospheric meteor events featuring real-time packet-driven trajectories, custom sounds, visual shockwaves, procedural crater carving with safe terrain rollback, interactive cores, and customizable loot tables.

## Version

Current version: **1.1.0**

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
  2. *Atmospheric Crossing:* Angled flight trajectory from extreme heights (160-240 blocks) rendered using PacketEvents virtual entities with spinning double-helix trails, sonic smoke cones, and embers visible from 256+ blocks away.
  3. *Impact:* Cinematic particle burst, 3D localized sound effects, knockback, and 100% client-side fake block crater manipulation.
  4. *Exploration:* Floating interactive core with live countdown hologram (30s lock) preventing premature looting, fake core chest, and fragment nodes.
  5. *Vanishing Scenery & Cleanup:* 30 seconds after looting the core, fake destroyed terrain seamlessly vanishes via packet restoration, returning the landscape to normal without modifying world data.

- **Custom Meteor Types:**
  - **Ember (Common):** Fiery meteor generating magma and basalt craters with standard mining rewards.
  - **Frostfall (Rare):** Glacial meteor with soul fire, blue ice, snow particles, and winter combat equipment.
  - **Voidfall (Epic):** Cosmic purple meteor with dragon breath, crying obsidian, end stone, and rare scrap.
  - **Worldbreaker (Legendary):** Massive apocalyptic strike causing profound crater excavation and ancient debris loot.

- **PacketEvents Visual System:**
  - 100% client-side fake block craters (`sendBlockChange`): real world blocks are never modified.
  - Virtual floating cores and real-time updating ArmorStand holograms for cooldown timers.
  - 3D localized audio originating from the meteor's spatial coordinates rather than global player ear playback.
  - Selective packet transmission restricted to configurable view distances.

- **Terrain & Protection Integration:**
  - Client-side virtual crater carving leaving the server world untouched.
  - Automatic packet restoration after looting expiration.
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
- `config.yml`: Core scheduling intervals, probabilities, allowed worlds, view distance limits, core lock time, and post-loot vanish delay.
- `meteors.yml`: Meteor definitions, velocities, high-altitude trajectories (160-240 blocks), particles, crater geometry, lock durations, and visual models.
- `rewards.yml`: Loot tables, item chances, amounts, custom names, lore, enchantments, and console commands.
- `messages.yml`: Announcements, titles, subtitles, action bar messages, and placeholders.
- `protections.yml`: Terrain alteration toggles, blacklisted blocks, entity damage, and WorldGuard checks.

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

- **1.1.0:**
  - **Virtual Packet Craters:** Impact craters and destroyed terrain are now 100% client-side fake blocks rendered via packets; zero real blocks are modified in the server world.
  - **Cooldown Hologram & Core Lock:** Added configurable 30-second core looting cooldown with real-time countdown ArmorStand hologram (`&c⏳ Cooling down: &e{time}s` updating every second).
  - **30s Post-Loot Vanishing Scenery:** Once looted, fake destroyed terrain destabilizes and restores itself cleanly 30 seconds after opening the core.
  - **High-Altitude Cinematic Trajectory:** Multi-layered particle effects (dense fire core, spinning double-helix trails, sonic smoke cones, and sparks) launched from heights of 160-240 blocks, visible from 256+ blocks away.
  - **3D Spatial Audio:** Atmospheric flight and impact sounds originate realistically from the meteor's coordinates in 3D space with natural distance falloff, eliminating artificial global sound playback.
- **1.0.1:** Added author metadata (`lHawk_`) and official contact URL (`https://minebuilders.com.br`) across `plugin.yml`, `pom.xml`, and documentation.
- **1.0.0:** Initial release.
  - Full event lifecycle (Omen, Crossing, Impact, Exploration, Rollback).
  - PacketEvents 2.14.0 integration for virtual falling entities and floating cores.
  - 4 built-in meteor types: Ember, Frostfall, Voidfall, and Worldbreaker.
  - Folia multi-threaded region scheduler and Spigot/Paper compatibility.
  - WorldGuard 7+ soft integration.
