package com.meteorevents.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
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

    public int spawnVisualEntity(UUID eventId, Location location, String entityTypeStr, int viewDistance) {
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
        } catch (Throwable ignored) {}

        return entityId;
    }

    public void teleportVisualEntity(int entityId, Location location, int viewDistance) {
        if (!Platform.hasPacketEvents() || location == null || location.getWorld() == null) {
            return;
        }
        try {
            Vector3d pos = new Vector3d(location.getX(), location.getY(), location.getZ());
            WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                    entityId,
                    pos,
                    location.getYaw(),
                    location.getPitch(),
                    false
            );
            sendToNearby(location, packet, viewDistance);
        } catch (Throwable ignored) {}
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
        Component comp = AdventureSerializer.fromLegacyFormat(ChatColor.translateAlternateColorCodes('&', text));
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

        try {
            WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(ids);
            sendToNearby(location, packet, viewDistance);
        } catch (Throwable ignored) {}
    }

    public void cleanupAll() {
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

        for (Player p : viewers) {
            safeSpawnParticle(p, mainParticle, loc, 14, 0.8, 0.8, 0.8, 0.05);
            safeSpawnParticle(p, secondaryParticle, loc, 4, 0.5, 0.5, 0.5, 0.02);

            double angle1 = tick * 0.45;
            double angle2 = angle1 + Math.PI;
            double radius = 1.6;

            Vector off1 = perp1.clone().multiply(Math.cos(angle1) * radius).add(perp2.clone().multiply(Math.sin(angle1) * radius));
            Vector off2 = perp1.clone().multiply(Math.cos(angle2) * radius).add(perp2.clone().multiply(Math.sin(angle2) * radius));

            Location p1 = loc.clone().add(off1);
            Location p2 = loc.clone().add(off2);
            safeSpawnParticle(p, Particle.FLAME, p1, 2, 0.05, 0.05, 0.05, 0.01);
            safeSpawnParticle(p, Particle.SOUL_FIRE_FLAME, p2, 2, 0.05, 0.05, 0.05, 0.01);

            for (int step = 1; step <= 3; step++) {
                Location tail = loc.clone().subtract(normDir.clone().multiply(step * 1.8));
                safeSpawnParticle(p, Particle.CAMPFIRE_COSY_SMOKE, tail, 3, 0.4 * step, 0.4 * step, 0.4 * step, 0.01);
            }
        }

        if (tick % 8 == 0) {
            playLocalizedSound(loc, "ENTITY_FIREWORK_ROCKET_BLAST", 16.0f, 0.6f);
        }
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
        if (name == null) return EntityTypes.FIREBALL;
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
            default:
                return EntityTypes.FIREBALL;
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
