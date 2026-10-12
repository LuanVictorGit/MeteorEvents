package com.meteorevents.event;

import com.meteorevents.compat.Schedulers;
import com.meteorevents.config.ConfigManager;
import com.meteorevents.config.MeteorType;
import com.meteorevents.config.ProtectionSettings;
import com.meteorevents.config.RewardTable;
import com.meteorevents.crater.CraterEngine;
import com.meteorevents.crater.CraterRollback;
import com.meteorevents.loot.LootManager;
import com.meteorevents.packet.PacketVisualManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ActiveMeteorEvent {

    public enum Stage {
        OMEN,
        CROSSING,
        IMPACT,
        EXPLORATION,
        COMPLETED
    }

    private final UUID id = UUID.randomUUID();
    private final Plugin plugin;
    private final ConfigManager configManager;
    private final PacketVisualManager visualManager;
    private final CraterEngine craterEngine;
    private final LootManager lootManager;
    private final MeteorType type;
    private final Location impactLocation;
    private final boolean safeTestMode;

    private Stage stage = Stage.OMEN;
    private Location startLocation;
    private Location currentLocation;
    private Vector stepVector;
    private int flightTotalTicks;
    private int flightCurrentTick;

    private int meteorEntityId = -1;
    private int coreEntityId = -1;
    private Location chestLocation;
    private Location coreLocation;
    private final Set<Location> fragmentLocations = Collections.synchronizedSet(new HashSet<>());
    private final AtomicBoolean claimed = new AtomicBoolean(false);
    private final CraterRollback rollback = new CraterRollback();

    private int omenSecondsRemaining;
    private int explorationSecondsRemaining;
    private int lockSecondsRemaining;

    public ActiveMeteorEvent(Plugin plugin, ConfigManager configManager, PacketVisualManager visualManager,
                             CraterEngine craterEngine, LootManager lootManager, MeteorType type,
                             Location impactLocation, boolean safeTestMode) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.visualManager = visualManager;
        this.craterEngine = craterEngine;
        this.lootManager = lootManager;
        this.type = type;
        this.impactLocation = impactLocation.clone();
        this.safeTestMode = safeTestMode;

        this.omenSecondsRemaining = configManager.getOmenDurationSeconds();
        this.explorationSecondsRemaining = type.getExplorationDurationSeconds();
        this.lockSecondsRemaining = type.getLockDurationSeconds();

        initTrajectory();
    }

    private void initTrajectory() {
        double angle = new Random().nextDouble() * (2 * Math.PI);
        double offX = Math.cos(angle) * type.getHorizontalOffset();
        double offZ = Math.sin(angle) * type.getHorizontalOffset();

        startLocation = impactLocation.clone().add(offX, type.getStartHeight(), offZ);
        currentLocation = startLocation.clone();

        double dist = startLocation.distance(impactLocation);
        double speed = Math.max(0.2, type.getSpeed());
        flightTotalTicks = Math.max(5, (int) Math.round(dist / speed));

        stepVector = impactLocation.toVector().subtract(startLocation.toVector()).multiply(1.0 / flightTotalTicks);
    }

    public void start() {
        broadcastOmen();
    }

    public void tickSecond() {
        if (stage == Stage.OMEN) {
            omenSecondsRemaining--;
            if (omenSecondsRemaining <= 0) {
                beginCrossing();
            }
        } else if (stage == Stage.EXPLORATION) {
            explorationSecondsRemaining--;
            if (lockSecondsRemaining > 0) {
                lockSecondsRemaining--;
            }

            if (explorationSecondsRemaining == 30) {
                broadcastMessage("announcements.despawn-warning");
            }

            if (explorationSecondsRemaining <= 0) {
                finishEvent();
            } else if (coreLocation != null) {
                visualManager.broadcastParticles(coreLocation, type.getTrailParticles(), 3, 0.3, 0.3, 0.3, 0.02, configManager.getViewDistance());
            }
        }
    }

    public void tickFlight() {
        if (stage != Stage.CROSSING) return;

        flightCurrentTick++;
        currentLocation.add(stepVector);

        int viewDist = configManager.getViewDistance();
        visualManager.teleportVisualEntity(meteorEntityId, currentLocation, viewDist);
        visualManager.broadcastParticles(currentLocation, type.getTrailParticles(),
                configManager.getMaxParticlesPerTick(), 0.3, 0.3, 0.3, 0.05, viewDist);

        if (flightCurrentTick % 10 == 0) {
            visualManager.broadcastSound(currentLocation, type.getCrossingSound(), 2.0f, 1.0f, viewDist);
        }

        if (flightCurrentTick >= flightTotalTicks) {
            triggerImpact();
        }
    }

    private void beginCrossing() {
        stage = Stage.CROSSING;
        broadcastCrossing();

        int viewDist = configManager.getViewDistance();
        meteorEntityId = visualManager.spawnVisualEntity(id, currentLocation, type.getVisualEntity(), viewDist);
        visualManager.broadcastSound(currentLocation, type.getCrossingSound(), 3.0f, 1.0f, viewDist);
    }

    private void triggerImpact() {
        stage = Stage.IMPACT;

        int viewDist = configManager.getViewDistance();
        visualManager.destroyVisualEntity(id, meteorEntityId, currentLocation, viewDist);
        visualManager.broadcastSound(impactLocation, type.getImpactSound(), type.getImpactSoundVolume(), type.getImpactSoundPitch(), viewDist);

        spawnImpactShockwave();

        Schedulers.runAt(plugin, impactLocation, () -> {
            ProtectionSettings protections = configManager.getProtections();
            CraterEngine.CraterResult result;

            if (safeTestMode) {
                ProtectionSettings safeProtections = new ProtectionSettings(
                        false, false, 0, false, false, false,
                        Collections.emptyList(), false, 0.0, 0.0, false, false, Collections.emptyList()
                );
                result = craterEngine.generateCrater(impactLocation, type, safeProtections, rollback);
            } else {
                result = craterEngine.generateCrater(impactLocation, type, protections, rollback);
            }

            chestLocation = result.getChestLocation();
            coreLocation = result.getCoreLocation();
            fragmentLocations.addAll(result.getFragmentLocations());

            RewardTable table = configManager.getRewardTable(type.getRewardTable());
            if (chestLocation != null && chestLocation.getBlock() != null) {
                lootManager.populateChest(chestLocation.getBlock(), table);
            }

            coreEntityId = visualManager.spawnVisualEntity(id, coreLocation, "END_CRYSTAL", viewDist);

            lootManager.awardParticipation(impactLocation);
            broadcastImpact();

            stage = Stage.EXPLORATION;
        });
    }

    private void spawnImpactShockwave() {
        int viewDist = configManager.getViewDistance();
        int count = type.getImpactParticlesCount();
        visualManager.broadcastParticles(impactLocation, Collections.singletonList("EXPLOSION_HUGE"), 3, 0.5, 0.5, 0.5, 0.1, viewDist);
        visualManager.broadcastParticles(impactLocation, type.getTrailParticles(), count, 2.5, 1.5, 2.5, 0.3, viewDist);
    }

    public boolean claimCore(Player player) {
        if (stage != Stage.EXPLORATION || claimed.get()) {
            return false;
        }

        if (lockSecondsRemaining > 0) {
            Map<String, String> ph = new HashMap<>();
            ph.put("time", String.valueOf(lockSecondsRemaining));
            player.sendMessage(configManager.getFormatted("rewards.core-locked", ph));
            return false;
        }

        if ("KEY".equalsIgnoreCase(type.getUnlockType()) && !"AIR".equalsIgnoreCase(type.getRequiredKey())) {
            Material reqMat = parseMaterial(type.getRequiredKey());
            if (reqMat != null && (player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType() != reqMat)) {
                Map<String, String> ph = new HashMap<>();
                ph.put("key", type.getRequiredKey());
                player.sendMessage(configManager.getFormatted("rewards.core-key-required", ph));
                return false;
            }
        }

        if (!claimed.compareAndSet(false, true)) {
            return false;
        }

        RewardTable table = configManager.getRewardTable(type.getRewardTable());
        lootManager.executeRewardCommands(table, player);

        Map<String, String> ph = getPlaceholders();
        ph.put("player", player.getName());

        String broadcastMsg = configManager.getFormatted("rewards.core-claimed-broadcast", ph);
        if (!broadcastMsg.isEmpty()) {
            Bukkit.broadcastMessage(configManager.getMessage("prefix") + broadcastMsg);
        }

        String playerMsg = configManager.getFormatted("rewards.core-claimed-player", ph);
        if (!playerMsg.isEmpty()) {
            player.sendMessage(configManager.getMessage("prefix") + playerMsg);
        }

        int viewDist = configManager.getViewDistance();
        if (coreEntityId != -1) {
            visualManager.destroyVisualEntity(id, coreEntityId, coreLocation, viewDist);
            coreEntityId = -1;
        }
        visualManager.broadcastParticles(coreLocation, Collections.singletonList("FIREWORKS_SPARK"), 50, 0.5, 0.5, 0.5, 0.1, viewDist);
        visualManager.broadcastSound(coreLocation, "ENTITY_PLAYER_LEVELUP", 1.5f, 1.2f, viewDist);

        return true;
    }

    public boolean isFragmentLocation(Location loc) {
        if (loc == null || fragmentLocations.isEmpty()) return false;
        for (Location fLoc : fragmentLocations) {
            if (fLoc.getWorld() != null && fLoc.getWorld().equals(loc.getWorld())
                    && fLoc.getBlockX() == loc.getBlockX()
                    && fLoc.getBlockY() == loc.getBlockY()
                    && fLoc.getBlockZ() == loc.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    public void removeFragment(Location loc) {
        fragmentLocations.removeIf(fLoc -> fLoc.getWorld() != null && fLoc.getWorld().equals(loc.getWorld())
                && fLoc.getBlockX() == loc.getBlockX()
                && fLoc.getBlockY() == loc.getBlockY()
                && fLoc.getBlockZ() == loc.getBlockZ());
    }

    public void finishEvent() {
        if (stage == Stage.COMPLETED) return;
        stage = Stage.COMPLETED;

        int viewDist = configManager.getViewDistance();
        visualManager.cleanupEvent(id, impactLocation, viewDist);

        if (type.isUnstableCore() && !claimed.get() && coreLocation != null) {
            visualManager.broadcastParticles(coreLocation, Collections.singletonList("SMOKE_LARGE"), 40, 0.5, 0.5, 0.5, 0.1, viewDist);
            visualManager.broadcastSound(coreLocation, "ENTITY_GENERIC_EXTINGUISH_FIRE", 2.0f, 0.8f, viewDist);
        }

        broadcastMessage("announcements.ended-chat");

        ProtectionSettings protections = configManager.getProtections();
        if (!safeTestMode && protections.isEnableRollback() && rollback.getRecords().size() > 0) {
            Schedulers.runLaterAt(plugin, impactLocation, protections.getRollbackDelaySeconds() * 20L, () -> {
                rollback.executeRollback(plugin, protections.isPreservePlayerModifications());
            });
        }
    }

    private void broadcastOmen() {
        broadcastMessage("announcements.omen-chat");
        World world = impactLocation.getWorld();
        if (world == null) return;

        String title = configManager.getFormatted("announcements.omen-title", getPlaceholders());
        String sub = configManager.getFormatted("announcements.omen-subtitle", getPlaceholders());

        for (Player p : world.getPlayers()) {
            p.sendTitle(title, sub, 10, 50, 10);
            p.playSound(p.getLocation(), PacketVisualManager.parseSound("ENTITY_LIGHTNING_BOLT_THUNDER"), 0.8f, 0.5f);
        }
    }

    private void broadcastCrossing() {
        broadcastMessage("announcements.crossing-chat");
        World world = impactLocation.getWorld();
        if (world == null) return;

        String title = configManager.getFormatted("announcements.crossing-title", getPlaceholders());
        String sub = configManager.getFormatted("announcements.crossing-subtitle", getPlaceholders());

        for (Player p : world.getPlayers()) {
            p.sendTitle(title, sub, 5, 40, 10);
        }
    }

    private void broadcastImpact() {
        broadcastMessage("announcements.impact-chat");
        World world = impactLocation.getWorld();
        if (world == null) return;

        String title = configManager.getFormatted("announcements.impact-title", getPlaceholders());
        String sub = configManager.getFormatted("announcements.impact-subtitle", getPlaceholders());

        for (Player p : world.getPlayers()) {
            p.sendTitle(title, sub, 5, 50, 10);
        }
    }

    private void broadcastMessage(String key) {
        String msg = configManager.getFormatted(key, getPlaceholders());
        if (!msg.isEmpty()) {
            Bukkit.broadcastMessage(configManager.getMessage("prefix") + msg);
        }
    }

    public Map<String, String> getPlaceholders() {
        Map<String, String> ph = new HashMap<>();
        ph.put("meteor", type.getDisplayName());
        ph.put("type", type.getId());
        ph.put("world", impactLocation.getWorld() != null ? impactLocation.getWorld().getName() : "world");
        ph.put("x", String.valueOf(impactLocation.getBlockX()));
        ph.put("y", String.valueOf(impactLocation.getBlockY()));
        ph.put("z", String.valueOf(impactLocation.getBlockZ()));
        ph.put("stage", stage.name());
        ph.put("time", String.valueOf(Math.max(0, explorationSecondsRemaining)));
        return ph;
    }

    public UUID getId() {
        return id;
    }

    public Stage getStage() {
        return stage;
    }

    public MeteorType getType() {
        return type;
    }

    public Location getImpactLocation() {
        return impactLocation;
    }

    public Location getChestLocation() {
        return chestLocation;
    }

    public Location getCoreLocation() {
        return coreLocation;
    }

    public boolean isSafeTestMode() {
        return safeTestMode;
    }

    public int getExplorationSecondsRemaining() {
        return explorationSecondsRemaining;
    }

    private Material parseMaterial(String name) {
        if (name == null) return null;
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
