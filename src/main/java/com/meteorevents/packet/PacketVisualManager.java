package com.meteorevents.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.meteorevents.compat.Platform;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PacketVisualManager {
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(-200000);
    private final Map<UUID, Set<Integer>> activeEventEntities = new ConcurrentHashMap<>();
    private final Map<UUID, List<Integer>> activeHologramEntities = new ConcurrentHashMap<>();

    public int getNextEntityId() {
        return ENTITY_ID_COUNTER.decrementAndGet();
    }

    public int spawnVisualEntity(UUID eventId, Location location, String entityTypeStr, float scale, int viewDistance) {
        int entityId = getNextEntityId();
        activeEventEntities.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet()).add(entityId);

        if (!Platform.hasPacketEvents() || location == null || location.getWorld() == null) {
            return entityId;
        }

        try {
            EntityType type = resolveEntityType(entityTypeStr);
            Vector3d pos = new Vector3d(location.getX(), location.getY(), location.getZ());
            WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(
                    entityId,
                    Optional.of(UUID.randomUUID()),
                    type,
                    pos,
                    location.getPitch(),
                    location.getYaw(),
                    location.getYaw(),
                    0,
                    Optional.of(new Vector3d(0, 0, 0))
            );
            sendToNearby(location, packet, viewDistance);

            if (type == EntityTypes.ITEM_DISPLAY) {
                ItemType itemType = resolveItemType(entityTypeStr);
                ItemStack displayStack = ItemStack.builder().type(itemType).amount(1).build();
                List<EntityData<?>> metadata = new ArrayList<>();
                metadata.add(new EntityData<>(8, EntityDataTypes.INT, 0));
                metadata.add(new EntityData<>(9, EntityDataTypes.INT, 1));
                metadata.add(new EntityData<>(10, EntityDataTypes.INT, 1));
                metadata.add(new EntityData<>(11, EntityDataTypes.VECTOR3F, new Vector3f(0f, 0f, 0f)));
                float entityScale = scale > 0.1f ? scale : (entityTypeStr != null && entityTypeStr.toUpperCase(Locale.ROOT).contains("WORLDBREAKER") ? 5.5f : 3.8f);
                metadata.add(new EntityData<>(12, EntityDataTypes.VECTOR3F, new Vector3f(entityScale, entityScale, entityScale)));
                metadata.add(new EntityData<>(16, EntityDataTypes.BYTE, (byte) 3));
                metadata.add(new EntityData<>(23, EntityDataTypes.ITEMSTACK, displayStack));
                metadata.add(new EntityData<>(24, EntityDataTypes.BYTE, (byte) 0));

                WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
                sendToNearby(location, metaPacket, viewDistance);
            }
        } catch (Throwable ignored) {}

        return entityId;
    }

    public int spawnVisualEntity(UUID eventId, Location location, String entityTypeStr, int viewDistance) {
        return spawnVisualEntity(eventId, location, entityTypeStr, 3.8f, viewDistance);
    }

    public void teleportVisualEntity(int entityId, Location location, float yaw, float pitch, int viewDistance) {
        if (!Platform.hasPacketEvents() || location == null || location.getWorld() == null) {
            return;
        }
        try {
            Vector3d pos = new Vector3d(location.getX(), location.getY(), location.getZ());
            WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                    entityId,
                    pos,
                    yaw,
                    pitch,
                    false
            );
            sendToNearby(location, packet, viewDistance);
        } catch (Throwable ignored) {}
    }

    public void teleportVisualEntity(int entityId, Location location, int viewDistance) {
        teleportVisualEntity(entityId, location, location != null ? location.getYaw() : 0f, location != null ? location.getPitch() : 0f, viewDistance);
    }

    public void destroyVisualEntity(UUID eventId, int entityId, Location location, int viewDistance) {
        Set<Integer> set = activeEventEntities.get(eventId);
        if (set != null) {
            set.remove(entityId);
        }

        if (!Platform.hasPacketEvents() || location == null || location.getWorld() == null) {
            return;
        }
        try {
            WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(entityId);
            sendToNearby(location, packet, viewDistance);
        } catch (Throwable ignored) {}
    }

    public void spawnHologram(UUID eventId, Location baseLocation, List<String> lines, int viewDistance) {
        if (baseLocation == null || lines == null || lines.isEmpty()) return;

        List<Integer> ids = new ArrayList<>();
        double startY = baseLocation.getY() + (lines.size() * 0.28);

        for (int i = 0; i < lines.size(); i++) {
            int entityId = getNextEntityId();
            ids.add(entityId);

            Location lineLoc = baseLocation.clone();
            lineLoc.setY(startY - (i * 0.28));

            if (Platform.hasPacketEvents()) {
                try {
                    Vector3d pos = new Vector3d(lineLoc.getX(), lineLoc.getY(), lineLoc.getZ());
                    WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                            entityId,
                            Optional.of(UUID.randomUUID()),
                            EntityTypes.ARMOR_STAND,
                            pos,
                            0f, 0f, 0f, 0,
                            Optional.of(new Vector3d(0, 0, 0))
                    );
                    sendToNearby(baseLocation, spawn, viewDistance);

                    List<EntityData<?>> metadata = createArmorStandMetadata(lines.get(i));
                    WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
                    sendToNearby(baseLocation, metaPacket, viewDistance);
                } catch (Throwable ignored) {}
            }
        }
        activeHologramEntities.put(eventId, ids);
    }

    public void updateHologram(UUID eventId, Location baseLocation, List<String> lines, int viewDistance) {
        List<Integer> ids = activeHologramEntities.get(eventId);
        if (ids == null || ids.isEmpty() || !Platform.hasPacketEvents()) return;

        for (int i = 0; i < lines.size() && i < ids.size(); i++) {
            int entityId = ids.get(i);
            try {
                List<EntityData<?>> metadata = createArmorStandMetadata(lines.get(i));
                WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
                sendToNearby(baseLocation, metaPacket, viewDistance);
            } catch (Throwable ignored) {}
        }
    }

    public void destroyHologram(UUID eventId, Location location, int viewDistance) {
        List<Integer> ids = activeHologramEntities.remove(eventId);
        if (ids == null || ids.isEmpty() || !Platform.hasPacketEvents()) return;

        int[] entityIds = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            entityIds[i] = ids.get(i);
        }

        try {
            WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(entityIds);
            sendToNearby(location, packet, viewDistance);
        } catch (Throwable ignored) {}
    }

    public void resendHologramToPlayer(Player player, Location baseLocation, List<String> lines) {
        if (!Platform.hasPacketEvents() || player == null || baseLocation == null || lines == null) return;
        double startY = baseLocation.getY() + (lines.size() * 0.28);

        for (int i = 0; i < lines.size(); i++) {
            int entityId = getNextEntityId();
            Location lineLoc = baseLocation.clone();
            lineLoc.setY(startY - (i * 0.28));

            try {
                Vector3d pos = new Vector3d(lineLoc.getX(), lineLoc.getY(), lineLoc.getZ());
                WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                        entityId,
                        Optional.of(UUID.randomUUID()),
                        EntityTypes.ARMOR_STAND,
                        pos,
                        0f, 0f, 0f, 0,
                        Optional.of(new Vector3d(0, 0, 0))
                );
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawn);

                List<EntityData<?>> metadata = createArmorStandMetadata(lines.get(i));
                WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, metaPacket);
            } catch (Throwable ignored) {}
        }
    }

    private List<EntityData<?>> createArmorStandMetadata(String text) {
        List<EntityData<?>> list = new ArrayList<>(4);
        list.add(new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x20));
        Component comp = com.meteorevents.util.TextUtil.formatToComponent(text);
        list.add(new EntityData<>(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(comp)));
        list.add(new EntityData<>(3, EntityDataTypes.BOOLEAN, true));
        list.add(new EntityData<>(14, EntityDataTypes.BYTE, (byte) (0x10 | 0x01)));
        return list;
    }

    public void broadcastFakeBlocks(Location center, Map<Location, BlockData> fakeBlocks, int viewDistance) {
        if (center == null || center.getWorld() == null || fakeBlocks == null || fakeBlocks.isEmpty()) return;
        double maxDistSq = (double) viewDistance * viewDistance;

        for (Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= maxDistSq) {
                sendFakeBlocksToPlayer(player, fakeBlocks);
            }
        }
    }

    public void sendFakeBlocksToPlayer(Player player, Map<Location, BlockData> fakeBlocks) {
        if (player == null || fakeBlocks == null) return;
        for (Map.Entry<Location, BlockData> entry : fakeBlocks.entrySet()) {
            player.sendBlockChange(entry.getKey(), entry.getValue());
        }
    }

    public void broadcastRestoreFakeBlocks(Location center, Set<Location> locations, int viewDistance) {
        if (center == null || center.getWorld() == null || locations == null || locations.isEmpty()) return;
        double maxDistSq = (double) viewDistance * viewDistance;

        for (Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= maxDistSq) {
                for (Location loc : locations) {
                    player.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }
        }
    }

    public void cleanupEvent(UUID eventId, Location location, int viewDistance) {
        destroyHologram(eventId, location, viewDistance);

        Set<Integer> set = activeEventEntities.remove(eventId);
        if (set == null || set.isEmpty() || !Platform.hasPacketEvents() || location == null || location.getWorld() == null) {
            return;
        }

        int[] ids = new int[set.size()];
        int idx = 0;
        for (int id : set) {
            ids[idx++] = id;
        }

        stopFlightSounds(location, viewDistance);

        try {
            WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(ids);
            sendToNearby(location, packet, viewDistance);
        } catch (Throwable ignored) {}
    }

    public void cleanupAll() {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            try {
                player.stopSound(Sound.ITEM_ELYTRA_FLYING);
            } catch (Throwable ignored) {}
            try {
                player.stopSound(Sound.ENTITY_PHANTOM_SWOOP);
            } catch (Throwable ignored) {}
        }

        if (Platform.hasPacketEvents()) {
            List<Integer> allIds = new ArrayList<>();
            for (Set<Integer> set : activeEventEntities.values()) {
                allIds.addAll(set);
            }
            for (List<Integer> holoIds : activeHologramEntities.values()) {
                allIds.addAll(holoIds);
            }

            if (!allIds.isEmpty()) {
                int[] ids = new int[allIds.size()];
                for (int i = 0; i < allIds.size(); i++) {
                    ids[i] = allIds.get(i);
                }
                try {
                    WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(ids);
                    for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
                    }
                } catch (Throwable ignored) {}
            }
        }
        activeEventEntities.clear();
        activeHologramEntities.clear();
    }

    public void registerPacketListener(com.meteorevents.event.MeteorEventManager eventManager) {
        if (!Platform.hasPacketEvents()) return;
        try {
            PacketEvents.getAPI().getEventManager().registerListener(new com.github.retrooper.packetevents.event.PacketListenerAbstract() {
                @Override
                public void onPacketReceive(com.github.retrooper.packetevents.event.PacketReceiveEvent event) {
                    if (event.getPacketType() == com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.INTERACT_ENTITY) {
                        com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity packet =
                                new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity(event);
                        int entityId = packet.getEntityId();
                        com.meteorevents.event.ActiveMeteorEvent meteorEvent = eventManager.findEventForEntity(entityId);
                        if (meteorEvent != null) {
                            Player player = (Player) event.getPlayer();
                            if (player != null) {
                                com.meteorevents.compat.Schedulers.runAt(eventManager.getPlugin(), player.getLocation(), () -> {
                                    meteorEvent.claimCore(player);
                                });
                            }
                        }
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    public void renderCinematicFlight(Location loc, Vector dir, List<String> trailParticles, int maxParticles, int tick, int viewDistance) {
        if (loc == null || loc.getWorld() == null) return;
        World world = loc.getWorld();
        double maxDistSq = (double) viewDistance * viewDistance;

        Vector normDir = dir.clone().normalize();
        Vector perp1 = new Vector(-normDir.getZ(), 0, normDir.getX());
        if (perp1.lengthSquared() < 0.001) {
            perp1 = new Vector(1, 0, 0);
        } else {
            perp1.normalize();
        }
        Vector perp2 = normDir.clone().crossProduct(perp1).normalize();

        Particle mainParticle = trailParticles != null && !trailParticles.isEmpty()
                ? parseParticle(trailParticles.get(0)) : Particle.FLAME;
        Particle secondaryParticle = trailParticles != null && trailParticles.size() > 1
                ? parseParticle(trailParticles.get(1)) : Particle.LAVA;

        List<Player> viewers = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= maxDistSq) {
                viewers.add(p);
            }
        }
        if (viewers.isEmpty()) return;

        Location[] spiralLocations = new Location[3];
        double angleBase = tick * 0.65;
        double spiralRadius = 2.4;
        for (int s = 0; s < 3; s++) {
            double angle = angleBase + (s * (2.0 * Math.PI / 3.0));
            Vector off = perp1.clone().multiply(Math.cos(angle) * spiralRadius).add(perp2.clone().multiply(Math.sin(angle) * spiralRadius));
            spiralLocations[s] = loc.clone().add(off);
        }

        Location[] tailLocations = new Location[5];
        for (int step = 1; step <= 5; step++) {
            tailLocations[step - 1] = loc.clone().subtract(normDir.clone().multiply(step * 2.2));
        }

        for (Player p : viewers) {
            safeSpawnParticle(p, mainParticle, loc, 24, 1.2, 1.2, 1.2, 0.08);
            safeSpawnParticle(p, secondaryParticle, loc, 8, 0.8, 0.8, 0.8, 0.04);
            safeSpawnParticle(p, Particle.CAMPFIRE_COSY_SMOKE, loc, 12, 1.0, 1.0, 1.0, 0.03);
            safeSpawnParticle(p, Particle.FIREWORKS_SPARK, loc, 6, 0.7, 0.7, 0.7, 0.05);

            for (Location sLoc : spiralLocations) {
                safeSpawnParticle(p, mainParticle, sLoc, 3, 0.1, 0.1, 0.1, 0.01);
                safeSpawnParticle(p, Particle.LAVA, sLoc, 1, 0.05, 0.05, 0.05, 0.01);
            }

            for (int step = 1; step <= 5; step++) {
                Location tail = tailLocations[step - 1];
                safeSpawnParticle(p, Particle.CAMPFIRE_SIGNAL_SMOKE, tail, 4, 0.5 * step, 0.5 * step, 0.5 * step, 0.02);
                safeSpawnParticle(p, Particle.SMOKE_LARGE, tail, 3, 0.4 * step, 0.4 * step, 0.4 * step, 0.02);
                safeSpawnParticle(p, mainParticle, tail, 2, 0.3 * step, 0.3 * step, 0.3 * step, 0.02);
            }
        }

        if (tick % 6 == 0) {
            playLocalizedSound(loc, "ENTITY_PHANTOM_SWOOP", 12.0f, 0.55f);
            playLocalizedSound(loc, "ENTITY_FIREWORK_ROCKET_LAUNCH", 14.0f, 0.40f);
        }
        if (tick % 12 == 0) {
            playLocalizedSound(loc, "ENTITY_GHAST_SHOOT", 14.0f, 0.50f);
        }
    }

    public void stopFlightSounds(Location loc, int viewDistance) {
        if (loc == null || loc.getWorld() == null) return;
        double maxDistSq = (double) viewDistance * viewDistance;
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= maxDistSq) {
                try {
                    p.stopSound(Sound.ITEM_ELYTRA_FLYING);
                } catch (Throwable ignored) {}
                try {
                    p.stopSound(Sound.ENTITY_PHANTOM_SWOOP);
                } catch (Throwable ignored) {}
                try {
                    p.stopSound(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH);
                } catch (Throwable ignored) {}
                try {
                    p.stopSound(Sound.ENTITY_FIREWORK_ROCKET_BLAST);
                } catch (Throwable ignored) {}
                try {
                    p.stopSound(Sound.ENTITY_GHAST_SHOOT);
                } catch (Throwable ignored) {}
            }
        }
    }

    public void renderSmokePlumes(List<Location> plumes, int viewDistance) {
        if (plumes == null || plumes.isEmpty()) return;
        Location first = plumes.get(0);
        if (first == null || first.getWorld() == null) return;
        World world = first.getWorld();
        double maxDistSq = (double) viewDistance * viewDistance;

        List<Player> viewers = new ArrayList<>();
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(first) <= maxDistSq) {
                viewers.add(p);
            }
        }
        if (viewers.isEmpty()) return;

        for (Location loc : plumes) {
            for (Player p : viewers) {
                safeSpawnParticle(p, Particle.CAMPFIRE_SIGNAL_SMOKE, loc, 3, 0.25, 0.4, 0.25, 0.03);
                safeSpawnParticle(p, Particle.CAMPFIRE_COSY_SMOKE, loc, 2, 0.2, 0.3, 0.2, 0.02);
                safeSpawnParticle(p, Particle.FLAME, loc, 1, 0.15, 0.1, 0.15, 0.01);
            }
        }
    }

    public void renderSmokePlume(Location loc, int viewDistance) {
        if (loc == null || loc.getWorld() == null) return;
        broadcastParticles(loc, Collections.singletonList("CAMPFIRE_SIGNAL_SMOKE"), 3, 0.25, 0.4, 0.25, 0.03, viewDistance);
        broadcastParticles(loc, Collections.singletonList("CAMPFIRE_COSY_SMOKE"), 2, 0.2, 0.3, 0.2, 0.02, viewDistance);
        broadcastParticles(loc, Collections.singletonList("FLAME"), 1, 0.15, 0.1, 0.15, 0.01, viewDistance);
    }

    public void broadcastParticles(Location loc, List<String> particleNames, int count, double offX, double offY, double offZ, double speed, int viewDistance) {
        if (loc == null || loc.getWorld() == null || particleNames == null || particleNames.isEmpty()) {
            return;
        }
        double maxDistSq = (double) viewDistance * viewDistance;
        World world = loc.getWorld();

        for (String pName : particleNames) {
            Particle particle = parseParticle(pName);
            if (particle == null) continue;

            for (Player player : world.getPlayers()) {
                if (player.getLocation().distanceSquared(loc) <= maxDistSq) {
                    safeSpawnParticle(player, particle, loc, count, offX, offY, offZ, speed);
                }
            }
        }
    }

    private void safeSpawnParticle(Player player, Particle particle, Location loc, int count, double offX, double offY, double offZ, double speed) {
        if (player == null || particle == null || loc == null) return;
        try {
            Class<?> dataType = particle.getDataType();
            if (dataType == Void.class) {
                player.spawnParticle(particle, loc, count, offX, offY, offZ, speed);
            } else if (dataType == Float.class) {
                player.spawnParticle(particle, loc, count, offX, offY, offZ, speed, 1.0f);
            } else if (dataType == Integer.class) {
                player.spawnParticle(particle, loc, count, offX, offY, offZ, speed, 0);
            }
        } catch (Throwable ignored) {}
    }

    public void playLocalizedSound(Location loc, String soundName, float volume, float pitch) {
        if (loc == null || loc.getWorld() == null || soundName == null) return;
        Sound sound = parseSound(soundName);
        if (sound == null) return;
        loc.getWorld().playSound(loc, sound, volume, pitch);
    }

    private void sendToNearby(Location location, Object packet, int viewDistance) {
        if (!Platform.hasPacketEvents() || location == null || location.getWorld() == null) return;
        double maxDistSq = (double) viewDistance * viewDistance;

        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= maxDistSq) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
            }
        }
    }

    private EntityType resolveEntityType(String name) {
        if (name == null) return EntityTypes.ITEM_DISPLAY;
        switch (name.toUpperCase(Locale.ROOT)) {
            case "SMALL_FIREBALL":
                return EntityTypes.SMALL_FIREBALL;
            case "DRAGON_FIREBALL":
                return EntityTypes.DRAGON_FIREBALL;
            case "END_CRYSTAL":
                return EntityTypes.END_CRYSTAL;
            case "ARMOR_STAND":
                return EntityTypes.ARMOR_STAND;
            case "FALLING_BLOCK":
                return EntityTypes.FALLING_BLOCK;
            case "FIREBALL":
            case "LARGE_FIREBALL":
                return EntityTypes.FIREBALL;
            case "FIRE_CHARGE":
            case "SNOWBALL":
            case "ENDER_EYE":
            case "WORLDBREAKER_FIRE_CHARGE":
            case "ITEM_DISPLAY":
            default:
                return EntityTypes.ITEM_DISPLAY;
        }
    }

    private ItemType resolveItemType(String name) {
        if (name == null) return ItemTypes.FIRE_CHARGE;
        switch (name.toUpperCase(Locale.ROOT)) {
            case "SNOWBALL":
                return ItemTypes.SNOWBALL;
            case "ENDER_EYE":
            case "ENDER_PEARL":
                return ItemTypes.ENDER_EYE;
            case "MAGMA_BLOCK":
                return ItemTypes.MAGMA_BLOCK;
            case "FIRE_CHARGE":
            case "WORLDBREAKER_FIRE_CHARGE":
            default:
                return ItemTypes.FIRE_CHARGE;
        }
    }

    public static Particle parseParticle(String name) {
        if (name == null) return Particle.FLAME;
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Particle.FLAME;
        }
    }

    public static Sound parseSound(String name) {
        if (name == null) return Sound.ENTITY_GENERIC_EXPLODE;
        try {
            return Sound.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Sound.ENTITY_GENERIC_EXPLODE;
        }
    }
}
