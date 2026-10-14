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
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
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
    private Inventory lootInventory;
    private final List<Location> smokePlumeLocations = new ArrayList<>();

    private int omenSecondsRemaining;
    private int lockSecondsRemaining;
    private int lootSecondsRemaining;
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
        this.lockSecondsRemaining = type.getLockDurationSeconds() > 0 ? type.getLockDurationSeconds() : configManager.getCoreLockSeconds();
        this.lootSecondsRemaining = type.getLootDurationSeconds() > 0 ? type.getLootDurationSeconds() : configManager.getLootDurationSeconds();
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
                lootSecondsRemaining--;
                updateUnlockedHologram();

                if (coreLocation != null) {
                    visualManager.broadcastParticles(coreLocation, Collections.singletonList("FIREWORKS_SPARK"), 3, 0.3, 0.3, 0.3, 0.02, viewDist);
                }

                if (lootSecondsRemaining == 30 && !claimed.get()) {
                    broadcastMessage("announcements.despawn-warning");
                }

                if (lootSecondsRemaining <= 0 && !claimed.get()) {
                    broadcastMessage("announcements.unlooted-despawn");
                    finishEvent();
                    return;
                }
            }

            visualManager.renderSmokePlumes(smokePlumeLocations, viewDist);
            if (!smokePlumeLocations.isEmpty() && (lockSecondsRemaining + lootSecondsRemaining) % 3 == 0) {
                Location randomPlume = smokePlumeLocations.get(new Random().nextInt(smokePlumeLocations.size()));
                visualManager.playLocalizedSound(randomPlume, "BLOCK_CAMPFIRE_CRACKLE", 1.8f, 0.9f);
                visualManager.playLocalizedSound(randomPlume, "BLOCK_LAVA_EXTINGUISH", 1.0f, 0.8f);
            }
        } else if (stage == Stage.POST_LOOT_VANISHING) {
            vanishSecondsRemaining--;
            updateVanishingHologram();

            visualManager.renderSmokePlumes(smokePlumeLocations, viewDist);

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
        float spinYaw = (flightCurrentTick * 28.0f) % 360.0f;
        float spinPitch = (flightCurrentTick * 18.0f) % 360.0f;
        visualManager.teleportVisualEntity(meteorEntityId, currentLocation, spinYaw, spinPitch, viewDist);

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
        meteorEntityId = visualManager.spawnVisualEntity(id, currentLocation, type.getVisualEntity(), type.getVisualEntityScale(), viewDist);
        visualManager.playLocalizedSound(currentLocation, type.getCrossingSound(), 16.0f, 0.6f);
    }

    private void triggerImpact() {
        stage = Stage.IMPACT;

        int viewDist = configManager.getViewDistance();
        visualManager.destroyVisualEntity(id, meteorEntityId, currentLocation, viewDist);
        visualManager.stopFlightSounds(impactLocation, viewDist);
        visualManager.playLocalizedSound(impactLocation, type.getImpactSound(), 16.0f, type.getImpactSoundPitch());

        spawnImpactShockwave();

        Schedulers.runAt(plugin, impactLocation, () -> {
            ProtectionSettings protections = configManager.getProtections();
            CraterEngine.CraterResult result = craterEngine.generateCrater(impactLocation, type, protections, rollback);

            chestLocation = result.getChestLocation();
            coreLocation = result.getCoreLocation();
            fragmentLocations.addAll(result.getFragmentLocations());

            RewardTable table = configManager.getRewardTable(type.getRewardTable());
            String titleTemplate = configManager.getMessage("rewards.chest-title");
            if (titleTemplate == null || titleTemplate.isEmpty()) {
                titleTemplate = "&6&lMeteor Loot - {meteor}";
            }
            String menuTitle = ChatColor.translateAlternateColorCodes('&',
                    titleTemplate.replace("{meteor}", type.getDisplayName()).replace("{type}", type.getId()));

            if (chestLocation != null && chestLocation.getBlock().getState() instanceof Chest) {
                lootManager.populateChest(chestLocation.getBlock(), table);
                Chest chestState = (Chest) chestLocation.getBlock().getState();
                try {
                    chestState.setCustomName(menuTitle);
                    chestState.update(true);
                } catch (Throwable ignored) {}
                lootInventory = chestState.getBlockInventory();
            } else {
                lootInventory = lootManager.createLootInventory(menuTitle, table);
            }

            int r = Math.max(3, type.getCraterRadius());
            World w = impactLocation.getWorld();
            if (w != null) {
                int plumeCount = Math.max(8, r * 2);
                double angleStep = (2.0 * Math.PI) / plumeCount;
                Random rnd = new Random();
                for (int i = 0; i < plumeCount; i++) {
                    double angle = (i * angleStep) + ((rnd.nextDouble() - 0.5) * 0.4);
                    double dist = (r * 0.75) + (rnd.nextDouble() * (r * 0.65));
                    int px = impactLocation.getBlockX() + (int) Math.round(Math.cos(angle) * dist);
                    int pz = impactLocation.getBlockZ() + (int) Math.round(Math.sin(angle) * dist);
                    int py;
                    try {
                        py = w.getHighestBlockYAt(px, pz);
                    } catch (Throwable t) {
                        py = impactLocation.getBlockY();
                    }
                    smokePlumeLocations.add(new Location(w, px + 0.5, py + 0.8, pz + 0.5));
                }
            }

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
        visualManager.broadcastParticles(impactLocation, Collections.singletonList("EXPLOSION_HUGE"), 8, 1.2, 1.2, 1.2, 0.1, viewDist);
        visualManager.broadcastParticles(impactLocation, Collections.singletonList("CAMPFIRE_SIGNAL_SMOKE"), 30, 2.0, 3.0, 2.0, 0.15, viewDist);
        visualManager.broadcastParticles(impactLocation, type.getTrailParticles(), count, 4.5, 3.0, 4.5, 0.4, viewDist);
        visualManager.broadcastParticles(impactLocation, Collections.singletonList("LAVA"), 25, 3.0, 2.0, 3.0, 0.2, viewDist);

        visualManager.playLocalizedSound(impactLocation, "ENTITY_GENERIC_EXPLODE", 16.0f, 0.55f);
        visualManager.playLocalizedSound(impactLocation, "ENTITY_LIGHTNING_BOLT_THUNDER", 16.0f, 0.50f);
        visualManager.playLocalizedSound(impactLocation, "ENTITY_DRAGON_FIREBALL_EXPLODE", 16.0f, 0.70f);
        visualManager.playLocalizedSound(impactLocation, "ENTITY_WITHER_SPAWN", 16.0f, 0.60f);
        visualManager.playLocalizedSound(impactLocation, "BLOCK_ANVIL_LAND", 16.0f, 0.50f);
    }

    public List<String> getCooldownHologramLines() {
        String title = configManager.getFormatted("holograms.cooling-title", getPlaceholders());
        if (title.isEmpty()) title = "&6&l✦ " + type.getDisplayName() + " &6&l✦";
        String sub = configManager.getFormatted("holograms.cooling-subtitle", getPlaceholders());
        if (sub.isEmpty()) sub = "&c⏳ Cooling down: &e" + lockSecondsRemaining + "s";
        return Arrays.asList(title, sub);
    }

    public List<String> getUnlockedHologramLines() {
        String title = configManager.getFormatted("holograms.unlocked-title", getPlaceholders());
        if (title.isEmpty()) title = "&a&l✦ " + type.getDisplayName() + " &a&l✦";
        String sub = configManager.getFormatted("holograms.unlocked-subtitle", getPlaceholders());
        if (sub.isEmpty()) sub = "&a&l✔ UNLOCKED! &e[Right-Click to Loot]";
        String timer = configManager.getFormatted("holograms.unlocked-timer", getPlaceholders());
        if (timer.isEmpty()) timer = "&e⏳ Regenerating in: &c" + lootSecondsRemaining + "s";
        return Arrays.asList(title, sub, timer);
    }

    public List<String> getVanishingHologramLines() {
        String title = configManager.getFormatted("holograms.vanishing-title", getPlaceholders());
        if (title.isEmpty()) title = "&7✦ Remnants Destabilizing ✦";
        String sub = configManager.getFormatted("holograms.vanishing-subtitle", getPlaceholders());
        if (sub.isEmpty()) sub = "&7⏳ Vanishing in: &e" + vanishSecondsRemaining + "s";
        return Arrays.asList(title, sub);
    }

    private void spawnInitialHologram() {
        if (coreLocation == null) return;
        visualManager.spawnHologram(id, coreLocation.clone().add(0, 0.8, 0), getCooldownHologramLines(), configManager.getViewDistance());
    }

    private void updateCooldownHologram() {
        if (coreLocation == null) return;
        visualManager.updateHologram(id, coreLocation.clone().add(0, 0.8, 0), getCooldownHologramLines(), configManager.getViewDistance());
    }

    private void updateUnlockedHologram() {
        if (coreLocation == null) return;
        visualManager.updateHologram(id, coreLocation.clone().add(0, 0.8, 0), getUnlockedHologramLines(), configManager.getViewDistance());
    }

    private void onCooldownFinished() {
        if (coreLocation == null) return;
        visualManager.updateHologram(id, coreLocation.clone().add(0, 0.8, 0), getUnlockedHologramLines(), configManager.getViewDistance());

        visualManager.broadcastParticles(coreLocation, Collections.singletonList("FIREWORKS_SPARK"), 35, 0.6, 0.6, 0.6, 0.08, configManager.getViewDistance());
        visualManager.playLocalizedSound(coreLocation, "BLOCK_BEACON_ACTIVATE", 6.0f, 1.2f);
        visualManager.playLocalizedSound(coreLocation, "ENTITY_PLAYER_LEVELUP", 6.0f, 1.4f);
    }

    private void updateVanishingHologram() {
        if (coreLocation == null) return;
        visualManager.updateHologram(id, coreLocation.clone().add(0, 0.8, 0), getVanishingHologramLines(), configManager.getViewDistance());
    }

    public boolean isCoreEntity(int entityId) {
        return coreEntityId != -1 && coreEntityId == entityId;
    }

    public boolean isChestOrCore(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (chestLocation != null && chestLocation.getWorld() != null && chestLocation.getWorld().equals(loc.getWorld())) {
            if (chestLocation.getBlockX() == loc.getBlockX() && chestLocation.getBlockZ() == loc.getBlockZ()) {
                if (Math.abs(chestLocation.getBlockY() - loc.getBlockY()) <= 1) {
                    return true;
                }
            }
        }
        if (coreLocation != null && coreLocation.getWorld() != null && coreLocation.getWorld().equals(loc.getWorld())) {
            if (coreLocation.distanceSquared(loc) <= 5.0) {
                return true;
            }
        }
        return false;
    }

    public boolean isInImpactRegion(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (stage != Stage.EXPLORATION && stage != Stage.POST_LOOT_VANISHING) return false;
        if (!loc.getWorld().equals(impactLocation.getWorld())) return false;

        if (rollback.containsLocation(loc)) {
            return true;
        }

        int radius = type.getCraterRadius() + 3;
        double dx = loc.getX() - impactLocation.getX();
        double dz = loc.getZ() - impactLocation.getZ();
        if (dx * dx + dz * dz <= radius * radius) {
            int depth = type.getCraterDepth() + 5;
            return loc.getY() >= impactLocation.getY() - depth && loc.getY() <= impactLocation.getY() + 10;
        }

        return false;
    }

    public boolean isClaimed() {
        return claimed.get();
    }

    public boolean claimCore(Player player) {
        if (stage == Stage.POST_LOOT_VANISHING) {
            if (lootInventory != null) {
                Schedulers.runLaterForEntity(plugin, player, 1L, () -> {
                    if (player.isOnline()) {
                        player.openInventory(lootInventory);
                    }
                });
                visualManager.playLocalizedSound(coreLocation != null ? coreLocation : impactLocation, "BLOCK_CHEST_OPEN", 1.5f, 1.0f);
            }
            return true;
        }

        if (stage != Stage.EXPLORATION) {
            return false;
        }

        if (lockSecondsRemaining > 0) {
            Map<String, String> ph = new HashMap<>();
            ph.put("time", String.valueOf(lockSecondsRemaining));
            String msg = configManager.getFormatted("rewards.core-locked", ph);
            if (!msg.isEmpty()) {
                player.sendMessage(configManager.getMessage("prefix") + msg);
            }
            try {
                player.sendTitle("", "§c⏳ Resfriando: §e" + lockSecondsRemaining + "s", 0, 20, 5);
            } catch (Throwable ignored) {}
            visualManager.playLocalizedSound(coreLocation != null ? coreLocation : impactLocation, "BLOCK_LAVA_EXTINGUISH", 2.0f, 1.0f);
            if (coreLocation != null) {
                visualManager.broadcastParticles(coreLocation, Collections.singletonList("SMOKE_NORMAL"), 12, 0.4, 0.4, 0.4, 0.05, configManager.getViewDistance());
            }
            return false;
        }

        if ("KEY".equalsIgnoreCase(type.getUnlockType()) && !"AIR".equalsIgnoreCase(type.getRequiredKey())) {
            Material reqMat = parseMaterial(type.getRequiredKey());
            if (reqMat != null && (player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType() != reqMat)) {
                Map<String, String> ph = new HashMap<>();
                ph.put("key", type.getRequiredKey());
                player.sendMessage(configManager.getMessage("prefix") + configManager.getFormatted("rewards.core-key-required", ph));
                return false;
            }
        }

        if (!claimed.compareAndSet(false, true)) {
            if (lootInventory != null) {
                Schedulers.runLaterForEntity(plugin, player, 1L, () -> {
                    if (player.isOnline()) {
                        player.openInventory(lootInventory);
                    }
                });
            }
            return true;
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
        if (coreLocation != null) {
            visualManager.broadcastParticles(coreLocation, Collections.singletonList("FIREWORKS_SPARK"), 60, 0.6, 0.6, 0.6, 0.12, viewDist);
            visualManager.playLocalizedSound(coreLocation, "BLOCK_CHEST_OPEN", 3.0f, 1.0f);
            visualManager.playLocalizedSound(coreLocation, "UI_TOAST_CHALLENGE_COMPLETE", 6.0f, 1.0f);
        }

        stage = Stage.POST_LOOT_VANISHING;
        updateVanishingHologram();

        if (lootInventory != null) {
            Schedulers.runLaterForEntity(plugin, player, 1L, () -> {
                if (player.isOnline()) {
                    player.openInventory(lootInventory);
                }
            });
        }

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

        if (lootInventory != null) {
            for (HumanEntity viewer : new ArrayList<>(lootInventory.getViewers())) {
                viewer.closeInventory();
            }
            lootInventory.clear();
        }

        int viewDist = configManager.getViewDistance();
        visualManager.cleanupEvent(id, impactLocation, viewDist);
        smokePlumeLocations.clear();

        rollback.executeRollback(plugin, false);

        visualManager.playLocalizedSound(impactLocation, "ENTITY_GENERIC_EXTINGUISH_FIRE", 4.0f, 0.8f);
        broadcastMessage("announcements.ended-chat");
    }

    public Inventory getLootInventory() {
        return lootInventory;
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
        ph.put("time", String.valueOf(Math.max(0, stage == Stage.POST_LOOT_VANISHING ? vanishSecondsRemaining : (lockSecondsRemaining > 0 ? lockSecondsRemaining : lootSecondsRemaining))));
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
        return lootSecondsRemaining;
    }

    public int getLootSecondsRemaining() {
        return lootSecondsRemaining;
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
