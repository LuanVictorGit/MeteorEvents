package com.meteorevents.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.meteorevents.compat.Platform;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PacketVisualManager {
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(-200000);
    private final Map<UUID, Set<Integer>> activeEventEntities = new ConcurrentHashMap<>();

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

    public void cleanupEvent(UUID eventId, Location location, int viewDistance) {
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
        if (!Platform.hasPacketEvents()) {
            activeEventEntities.clear();
            return;
        }

        List<Integer> allIds = new ArrayList<>();
        for (Set<Integer> set : activeEventEntities.values()) {
            allIds.addAll(set);
        }
        activeEventEntities.clear();

        if (allIds.isEmpty()) return;

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
                    player.spawnParticle(particle, loc, count, offX, offY, offZ, speed);
                }
            }
        }
    }

    public void broadcastSound(Location loc, String soundName, float volume, float pitch, int viewDistance) {
        if (loc == null || loc.getWorld() == null || soundName == null) {
            return;
        }
        Sound sound = parseSound(soundName);
        if (sound == null) return;

        double maxDistSq = (double) viewDistance * viewDistance;
        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(loc) <= maxDistSq) {
                player.playSound(loc, sound, volume, pitch);
            }
        }
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
