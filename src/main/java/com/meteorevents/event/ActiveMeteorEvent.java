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
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ActiveMeteorEvent {

    public enum Stage {
        OMEN,
        CROSSING,
        IMPACT,
        EXPLORATION,
        POST_LOOT_VANISHING,
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
    private final CraterRollback rollback = new CraterRollback();
    private final Set<Location> fragmentLocations = Collections.synchronizedSet(new HashSet<>());
    private final AtomicBoolean claimed = new AtomicBoolean(false);

    private int omenSecondsRemaining;
    private int explorationSecondsRemaining;
    private int lockSecondsRemaining;
    private int vanishSecondsRemaining;

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
        this.lockSecondsRemaining = type.getLockDurationSeconds() > 0 ? type.getLockDurationSeconds() : configManager.getCoreLockSeconds();
        this.vanishSecondsRemaining = configManager.getPostLootVanishSeconds();

        initTrajectory();
    }

    private void initTrajectory() {
        double angle = new Random().nextDouble() * (2 * Math.PI);
        double offX = Math.cos(angle) * type.getHorizontalOffset();
        double offZ = Math.sin(angle) * type.getHorizontalOffset();

        startLocation = impactLocation.clone().add(offX, type.getStartHeight(), offZ);
        currentLocation = startLocation.clone();

        double dist = startLocation.distance(impactLocation);
        double speed = Math.max(0.4, type.getSpeed());
        flightTotalTicks = Math.max(20, (int) Math.round(dist / speed));

        stepVector = impactLocation.toVector().subtract(startLocation.toVector()).multiply(1.0 / flightTotalTicks);
    }

    public void start() {
        broadcastOmen();
    }

    public void tickSecond() {
        int viewDist = configManager.getViewDistance();

        if (stage == Stage.OMEN) {
            omenSecondsRemaining--;
            if (omenSecondsRemaining <= 0) {
                beginCrossing();
            }
        } else if (stage == Stage.EXPLORATION) {
            explorationSecondsRemaining--;

            if (lockSecondsRemaining > 0) {
                lockSecondsRemaining--;
                updateCooldownHologram();

                if (coreLocation != null) {
                    visualManager.broadcastParticles(coreLocation, Collections.singletonList("CAMPFIRE_COSY_SMOKE"), 2, 0.2, 0.2, 0.2, 0.01, viewDist);
                }

                if (lockSecondsRemaining == 0) {
                    onCooldownFinished();
                }
            } else {
                if (coreLocation != null) {
                    visualManager.broadcastParticles(coreLocation, Collections.singletonList("FIREWORKS_SPARK"), 3, 0.3, 0.3, 0.3, 0.02, viewDist);
                }
            }

            if (explorationSecondsRemaining <= 0 && !claimed.get()) {
                finishEvent();
            }
        } else if (stage == Stage.POST_LOOT_VANISHING) {
            vanishSecondsRemaining--;
            updateVanishingHologram();

            if (coreLocation != null) {
                visualManager.broadcastParticles(coreLocation, Collections.singletonList("SMOKE_LARGE"), 4, 0.4, 0.4, 0.4, 0.02, viewDist);
            }

            if (vanishSecondsRemaining <= 0) {
                finishEvent();
            }
        }
    }

    public void tickFlight() {
        if (stage != Stage.CROSSING) return;

        flightCurrentTick++;
        currentLocation.add(stepVector);

        int viewDist = configManager.getViewDistance();
        visualManager.teleportVisualEntity(meteorEntityId, currentLocation, viewDist);

        visualManager.renderCinematicFlight(currentLocation, stepVector, type.getTrailParticles(),
                configManager.getMaxParticlesPerTick(), flightCurrentTick, viewDist);

        if (flightCurrentTick >= flightTotalTicks) {
            triggerImpact();
        }
    }

    private void beginCrossing() {
        stage = Stage.CROSSING;
        broadcastCrossing();

        int viewDist = configManager.getViewDistance();
        meteorEntityId = visualManager.spawnVisualEntity(id, currentLocation, type.getVisualEntity(), viewDist);
        visualManager.playLocalizedSound(currentLocation, type.getCrossingSound(), 16.0f, 0.6f);
    }

    private void triggerImpact() {
        stage = Stage.IMPACT;

        int viewDist = configManager.getViewDistance();
        visualManager.destroyVisualEntity(id, meteorEntityId, currentLocation, viewDist);
        visualManager.playLocalizedSound(impactLocation, type.getImpactSound(), 16.0f, type.getImpactSoundPitch());

        spawnImpactShockwave();

        Schedulers.runAt(plugin, impactLocation, () -> {
            ProtectionSettings protections = configManager.getProtections();
            CraterEngine.CraterResult result = craterEngine.generateCrater(impactLocation, type, protections, rollback);

            chestLocation = result.getChestLocation();
            coreLocation = result.getCoreLocation();
            fragmentLocations.addAll(result.getFragmentLocations());

            coreEntityId = visualManager.spawnVisualEntity(id, coreLocation, "END_CRYSTAL", viewDist);

            spawnInitialHologram();

            lootManager.awardParticipation(impactLocation);
            broadcastImpact();

            stage = Stage.EXPLORATION;
        });
    }

    private void spawnImpactShockwave() {
        int viewDist = configManager.getViewDistance();
        int count = type.getImpactParticlesCount();
        visualManager.broadcastParticles(impactLocation, Collections.singletonList("EXPLOSION_HUGE"), 5, 0.8, 0.8, 0.8, 0.1, viewDist);
        visualManager.broadcastParticles(impactLocation, type.getTrailParticles(), count, 3.5, 2.0, 3.5, 0.35, viewDist);
        visualManager.playLocalizedSound(impactLocation, "ENTITY_GENERIC_EXPLODE", 16.0f, 0.7f);
    }

    private void spawnInitialHologram() {
        if (coreLocation == null) return;
        List<String> lines = Arrays.asList(
                "&6&l✦ " + type.getDisplayName() + " &6&l✦",
                "&c⏳ Cooling down: &e" + lockSecondsRemaining + "s"
        );
        visualManager.spawnHologram(id, coreLocation.clone().add(0, 0.8, 0), lines, configManager.getViewDistance());
    }

    private void updateCooldownHologram() {
        if (coreLocation == null) return;
        List<String> lines = Arrays.asList(
                "&6&l✦ " + type.getDisplayName() + " &6&l✦",
                "&c⏳ Cooling down: &e" + lockSecondsRemaining + "s"
        );
        visualManager.updateHologram(id, coreLocation.clone().add(0, 0.8, 0), lines, configManager.getViewDistance());
    }

    private void onCooldownFinished() {
        if (coreLocation == null) return;
        List<String> lines = Arrays.asList(
                "&a&l✦ " + type.getDisplayName() + " &a&l✦",
                "&a&l✔ UNLOCKED! &e[Right-Click to Loot]"
        );
        visualManager.updateHologram(id, coreLocation.clone().add(0, 0.8, 0), lines, configManager.getViewDistance());

        visualManager.broadcastParticles(coreLocation, Collections.singletonList("FIREWORKS_SPARK"), 35, 0.6, 0.6, 0.6, 0.08, configManager.getViewDistance());
        visualManager.playLocalizedSound(coreLocation, "BLOCK_BEACON_ACTIVATE", 6.0f, 1.2f);
        visualManager.playLocalizedSound(coreLocation, "ENTITY_PLAYER_LEVELUP", 6.0f, 1.4f);
    }

    private void updateVanishingHologram() {
        if (coreLocation == null) return;
        List<String> lines = Arrays.asList(
                "&7✦ Remnants Destabilizing ✦",
                "&7⏳ Vanishing in: &e" + vanishSecondsRemaining + "s"
        );
        visualManager.updateHologram(id, coreLocation.clone().add(0, 0.8, 0), lines, configManager.getViewDistance());
    }

    public boolean claimCore(Player player) {
        if (stage != Stage.EXPLORATION || claimed.get()) {
            return false;
        }

        if (lockSecondsRemaining > 0) {
            Map<String, String> ph = new HashMap<>();
            ph.put("time", String.valueOf(lockSecondsRemaining));
            player.sendMessage(configManager.getFormatted("rewards.core-locked", ph));
            visualManager.playLocalizedSound(coreLocation != null ? coreLocation : impactLocation, "BLOCK_LAVA_EXTINGUISH", 2.0f, 1.0f);
            if (coreLocation != null) {
                visualManager.broadcastParticles(coreLocation, Collections.singletonList("SMOKE_NORMAL"), 8, 0.3, 0.3, 0.3, 0.05, configManager.getViewDistance());
            }
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
        List<ItemStack> rewards = lootManager.rollRewards(table);
        for (ItemStack item : rewards) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty() && player.getLocation().getWorld() != null) {
                for (ItemStack drop : leftover.values()) {
                    player.getLocation().getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }

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
        if (coreLocation != null) {
            visualManager.broadcastParticles(coreLocation, Collections.singletonList("FIREWORKS_SPARK"), 60, 0.6, 0.6, 0.6, 0.12, viewDist);
            visualManager.playLocalizedSound(coreLocation, "UI_TOAST_CHALLENGE_COMPLETE", 6.0f, 1.0f);
        }

        stage = Stage.POST_LOOT_VANISHING;
        updateVanishingHologram();

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
        if (loc.getWorld() != null) {
            loc.getBlock().setType(Material.AIR, false);
        }
    }

    public void finishEvent() {
        if (stage == Stage.COMPLETED) return;
        stage = Stage.COMPLETED;

        int viewDist = configManager.getViewDistance();
        visualManager.cleanupEvent(id, impactLocation, viewDist);

        rollback.executeRollback(plugin, true);

        visualManager.playLocalizedSound(impactLocation, "ENTITY_GENERIC_EXTINGUISH_FIRE", 4.0f, 0.8f);
        broadcastMessage("announcements.ended-chat");
    }

    private void broadcastOmen() {
        broadcastMessage("announcements.omen-chat");
        World world = impactLocation.getWorld();
        if (world == null) return;

        String title = configManager.getFormatted("announcements.omen-title", getPlaceholders());
        String sub = configManager.getFormatted("announcements.omen-subtitle", getPlaceholders());

        for (Player p : world.getPlayers()) {
            p.sendTitle(title, sub, 10, 50, 10);
        }
        visualManager.playLocalizedSound(impactLocation, "ENTITY_LIGHTNING_BOLT_THUNDER", 16.0f, 0.5f);
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
        ph.put("time", String.valueOf(Math.max(0, stage == Stage.POST_LOOT_VANISHING ? vanishSecondsRemaining : (lockSecondsRemaining > 0 ? lockSecondsRemaining : explorationSecondsRemaining))));
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

    public Map<Location, BlockData> getFakeBlocks() {
        return Collections.emptyMap();
    }

    public boolean isSafeTestMode() {
        return safeTestMode;
    }

    public int getExplorationSecondsRemaining() {
        return explorationSecondsRemaining;
    }

    public int getLockSecondsRemaining() {
        return lockSecondsRemaining;
    }

    public int getVanishSecondsRemaining() {
        return vanishSecondsRemaining;
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
