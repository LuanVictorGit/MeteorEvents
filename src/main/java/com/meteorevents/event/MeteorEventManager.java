package com.meteorevents.event;

import com.meteorevents.compat.Schedulers;
import com.meteorevents.compat.WorldGuardHook;
import com.meteorevents.config.ConfigManager;
import com.meteorevents.config.MeteorType;
import com.meteorevents.crater.CraterEngine;
import com.meteorevents.loot.LootManager;
import com.meteorevents.packet.PacketVisualManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class MeteorEventManager {
    private final Plugin plugin;
    private final ConfigManager configManager;
    private final PacketVisualManager visualManager;
    private final CraterEngine craterEngine;
    private final LootManager lootManager;

    private final Map<UUID, ActiveMeteorEvent> activeEvents = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private Schedulers.TaskHandle tickTask;
    private int checkSecondsCounter;
    private long lastEventTimeMillis = 0;

    public MeteorEventManager(Plugin plugin, ConfigManager configManager, PacketVisualManager visualManager,
                              CraterEngine craterEngine, LootManager lootManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.visualManager = visualManager;
        this.craterEngine = craterEngine;
        this.lootManager = lootManager;
    }

    public void start() {
        checkSecondsCounter = configManager.getCheckIntervalSeconds();
        tickTask = Schedulers.runTimerGlobal(plugin, 1L, 1L, this::tick);
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        stopAllEvents();
    }

    private int tickCounter = 0;

    private void tick() {
        for (ActiveMeteorEvent event : activeEvents.values()) {
            if (event.getStage() == ActiveMeteorEvent.Stage.CROSSING) {
                event.tickFlight();
            }
        }

        if (++tickCounter % 20 == 0) {
            tickSecond();
        }
    }

    private void tickSecond() {
        Iterator<Map.Entry<UUID, ActiveMeteorEvent>> it = activeEvents.entrySet().iterator();
        while (it.hasNext()) {
            ActiveMeteorEvent event = it.next().getValue();
            event.tickSecond();
            if (event.getStage() == ActiveMeteorEvent.Stage.COMPLETED) {
                it.remove();
            }
        }

        if (!configManager.isEnabled()) return;

        checkSecondsCounter--;
        if (checkSecondsCounter <= 0) {
            checkSecondsCounter = configManager.getCheckIntervalSeconds();
            attemptRandomEvent();
        }
    }

    private void attemptRandomEvent() {
        if (Bukkit.getOnlinePlayers().size() < configManager.getMinPlayersOnline()) {
            return;
        }

        if (activeEvents.size() >= configManager.getMaxConcurrentEvents()) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownMillis = configManager.getMinCooldownSeconds() * 1000L;
        if (now - lastEventTimeMillis < cooldownMillis) {
            return;
        }

        if (random.nextDouble() * 100.0 > configManager.getChancePercentage()) {
            return;
        }

        MeteorType type = pickRandomType();
        if (type == null) return;

        World world = pickAllowedWorld();
        if (world == null) return;

        findImpactLocationAsync(world, target -> {
            if (target != null && activeEvents.size() < configManager.getMaxConcurrentEvents()) {
                startEvent(type, target, false);
                lastEventTimeMillis = System.currentTimeMillis();
            }
        });
    }

    public ActiveMeteorEvent startEvent(MeteorType type, Location target, boolean safeTestMode) {
        if (type == null || target == null || target.getWorld() == null) {
            return null;
        }

        ActiveMeteorEvent event = new ActiveMeteorEvent(plugin, configManager, visualManager,
                craterEngine, lootManager, type, target, safeTestMode);
        activeEvents.put(event.getId(), event);
        event.start();
        return event;
    }

    public void stopAllEvents() {
        for (ActiveMeteorEvent event : activeEvents.values()) {
            event.finishEvent();
        }
        activeEvents.clear();
    }

    public MeteorType pickRandomType() {
        Map<String, MeteorType> types = configManager.getMeteorTypes();
        if (types.isEmpty()) return null;

        int totalWeight = 0;
        for (MeteorType t : types.values()) {
            totalWeight += Math.max(1, t.getWeight());
        }

        int roll = random.nextInt(totalWeight);
        int current = 0;
        for (MeteorType t : types.values()) {
            current += Math.max(1, t.getWeight());
            if (roll < current) {
                return t;
            }
        }
        return types.values().iterator().next();
    }

    public void findImpactLocationAsync(World world, Consumer<Location> callback) {
        if (world == null) {
            callback.accept(null);
            return;
        }
        findImpactLocationStep(world, 0, callback);
    }

    private void findImpactLocationStep(World world, int attempt, Consumer<Location> callback) {
        if (attempt >= 15) {
            Location spawn = world.getSpawnLocation();
            Location fallbackProbe = new Location(world, spawn.getX() + 100, 64, spawn.getZ() + 100);
            Schedulers.runAt(plugin, fallbackProbe, () -> {
                int y;
                try {
                    y = world.getHighestBlockYAt(spawn.getBlockX() + 100, spawn.getBlockZ() + 100);
                } catch (Throwable t) {
                    y = spawn.getBlockY();
                }
                callback.accept(new Location(world, spawn.getX() + 100.5, y, spawn.getZ() + 100.5));
            });
            return;
        }

        int x;
        int z;
        if (configManager.isTargetNearOnlinePlayers() && !world.getPlayers().isEmpty()) {
            List<Player> players = world.getPlayers();
            Player targetPlayer = players.get(random.nextInt(players.size()));
            Location pLoc = targetPlayer.getLocation();
            double angle = random.nextDouble() * (2 * Math.PI);
            int dist = configManager.getPlayerOffsetMin() + random.nextInt(Math.max(1, configManager.getPlayerOffsetMax() - configManager.getPlayerOffsetMin()));
            x = (int) (pLoc.getX() + Math.cos(angle) * dist);
            z = (int) (pLoc.getZ() + Math.sin(angle) * dist);
        } else {
            Location spawn = world.getSpawnLocation();
            double angle = random.nextDouble() * (2 * Math.PI);
            int dist = configManager.getMinDistanceFromSpawn() + random.nextInt(Math.max(1, configManager.getMaxDistanceFromSpawn() - configManager.getMinDistanceFromSpawn()));
            x = (int) (spawn.getX() + Math.cos(angle) * dist);
            z = (int) (spawn.getZ() + Math.sin(angle) * dist);
        }

        Location probe = new Location(world, x, 64, z);
        Schedulers.runAt(plugin, probe, () -> {
            int y;
            try {
                y = world.getHighestBlockYAt(x, z);
            } catch (Throwable t) {
                findImpactLocationStep(world, attempt + 1, callback);
                return;
            }
            Location candidate = new Location(world, x + 0.5, y, z + 0.5);

            if (configManager.getProtections().isWorldguardEnabled() && WorldGuardHook.isRegionProtected(candidate)) {
                findImpactLocationStep(world, attempt + 1, callback);
                return;
            }

            Block block = candidate.getBlock();
            if (configManager.isAvoidWater() && (block.getType() == Material.WATER || block.getType() == Material.LAVA)) {
                findImpactLocationStep(world, attempt + 1, callback);
                return;
            }

            callback.accept(candidate);
        });
    }

    public Location findImpactLocation(World world) {
        if (world == null) return null;

        for (int attempts = 0; attempts < 15; attempts++) {
            int x;
            int z;

            if (configManager.isTargetNearOnlinePlayers() && !world.getPlayers().isEmpty()) {
                List<Player> players = world.getPlayers();
                Player targetPlayer = players.get(random.nextInt(players.size()));
                Location pLoc = targetPlayer.getLocation();

                double angle = random.nextDouble() * (2 * Math.PI);
                int dist = configManager.getPlayerOffsetMin() + random.nextInt(Math.max(1, configManager.getPlayerOffsetMax() - configManager.getPlayerOffsetMin()));
                x = (int) (pLoc.getX() + Math.cos(angle) * dist);
                z = (int) (pLoc.getZ() + Math.sin(angle) * dist);
            } else {
                Location spawn = world.getSpawnLocation();
                double angle = random.nextDouble() * (2 * Math.PI);
                int dist = configManager.getMinDistanceFromSpawn() + random.nextInt(Math.max(1, configManager.getMaxDistanceFromSpawn() - configManager.getMinDistanceFromSpawn()));
                x = (int) (spawn.getX() + Math.cos(angle) * dist);
                z = (int) (spawn.getZ() + Math.sin(angle) * dist);
            }

            int y;
            try {
                y = world.getHighestBlockYAt(x, z);
            } catch (Throwable t) {
                continue;
            }
            Location candidate = new Location(world, x + 0.5, y, z + 0.5);

            if (configManager.getProtections().isWorldguardEnabled() && WorldGuardHook.isRegionProtected(candidate)) {
                continue;
            }

            Block block = candidate.getBlock();
            if (configManager.isAvoidWater() && (block.getType() == Material.WATER || block.getType() == Material.LAVA)) {
                continue;
            }

            return candidate;
        }

        Location spawn = world.getSpawnLocation();
        int y;
        try {
            y = world.getHighestBlockYAt(spawn.getBlockX() + 100, spawn.getBlockZ() + 100);
        } catch (Throwable t) {
            y = spawn.getBlockY();
        }
        return new Location(world, spawn.getX() + 100.5, y, spawn.getZ() + 100.5);
    }

    public World pickAllowedWorld() {
        for (String wName : configManager.getAllowedWorlds()) {
            World w = Bukkit.getWorld(wName);
            if (w != null && !configManager.getProtections().getProhibitedWorlds().contains(w.getName())) {
                return w;
            }
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    public int getNextEventSeconds() {
        return Math.max(0, checkSecondsCounter);
    }

    public Collection<ActiveMeteorEvent> getActiveEvents() {
        return Collections.unmodifiableCollection(activeEvents.values());
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public ActiveMeteorEvent findEventNear(Location loc, double radius) {
        if (loc == null || loc.getWorld() == null) return null;
        double radiusSq = radius * radius;
        for (ActiveMeteorEvent event : activeEvents.values()) {
            Location eLoc = event.getImpactLocation();
            if (eLoc.getWorld() != null && eLoc.getWorld().equals(loc.getWorld()) && eLoc.distanceSquared(loc) <= radiusSq) {
                return event;
            }
        }
        return null;
    }

    public ActiveMeteorEvent findEventForChestOrCore(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        for (ActiveMeteorEvent event : activeEvents.values()) {
            if (event.isChestOrCore(loc)) {
                return event;
            }
        }
        return null;
    }

    public ActiveMeteorEvent findEventForEntity(int entityId) {
        for (ActiveMeteorEvent event : activeEvents.values()) {
            if (event.isCoreEntity(entityId)) {
                return event;
            }
        }
        return null;
    }

    public ActiveMeteorEvent findEventWithFragment(Location loc) {
        if (loc == null) return null;
        for (ActiveMeteorEvent event : activeEvents.values()) {
            if (event.isFragmentLocation(loc)) {
                return event;
            }
        }
        return null;
    }

    public ActiveMeteorEvent findEventInImpactRegion(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        for (ActiveMeteorEvent event : activeEvents.values()) {
            if (event.isInImpactRegion(loc)) {
                return event;
            }
        }
        return null;
    }
}
