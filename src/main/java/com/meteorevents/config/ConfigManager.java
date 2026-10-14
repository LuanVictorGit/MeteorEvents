package com.meteorevents.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public final class ConfigManager {
    private final Plugin plugin;

    private boolean enabled = true;
    private int checkIntervalSeconds = 60;
    private double chancePercentage = 25.0;
    private int minCooldownSeconds = 600;
    private int maxConcurrentEvents = 2;
    private int minPlayersOnline = 1;
    private List<String> allowedWorlds = Collections.singletonList("world");

    private int minDistanceFromSpawn = 150;
    private int maxDistanceFromSpawn = 2500;
    private boolean targetNearOnlinePlayers = true;
    private int playerOffsetMin = 80;
    private int playerOffsetMax = 300;
    private boolean avoidWater = true;

    private int viewDistance = 128;
    private int updateRateTicks = 1;
    private int maxParticlesPerTick = 40;

    private int omenDurationSeconds = 10;
    private int coreLockSeconds = 30;
    private int lootDurationSeconds = 120;
    private int postLootVanishSeconds = 30;
    private int defaultExplorationSeconds = 180;

    private boolean participationRewardsEnabled = true;
    private int participationRadius = 50;
    private int participationXp = 50;

    private final Map<String, MeteorType> meteorTypes = new LinkedHashMap<>();
    private final Map<String, RewardTable> rewardTables = new HashMap<>();
    private final Map<String, String> messages = new HashMap<>();
    private ProtectionSettings protections;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        saveDefaults();
        loadConfig();
        loadMeteors();
        loadRewards();
        loadMessages();
        loadProtections();
    }

    private void saveDefaults() {
        saveResourceIfNotExists("config.yml");
        saveResourceIfNotExists("meteors.yml");
        saveResourceIfNotExists("rewards.yml");
        saveResourceIfNotExists("messages.yml");
        saveResourceIfNotExists("protections.yml");
    }

    private void saveResourceIfNotExists(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        enabled = cfg.getBoolean("enabled", true);
        checkIntervalSeconds = Math.max(10, cfg.getInt("scheduling.check-interval-seconds", 60));
        chancePercentage = Math.max(0.0, Math.min(100.0, cfg.getDouble("scheduling.chance-percentage", 25.0)));
        minCooldownSeconds = Math.max(0, cfg.getInt("scheduling.min-cooldown-seconds", 600));
        maxConcurrentEvents = Math.max(1, cfg.getInt("scheduling.max-concurrent-events", 2));
        minPlayersOnline = Math.max(0, cfg.getInt("scheduling.min-players-online", 1));
        allowedWorlds = cfg.getStringList("scheduling.allowed-worlds");
        if (allowedWorlds.isEmpty()) {
            allowedWorlds = Collections.singletonList("world");
        }

        minDistanceFromSpawn = cfg.getInt("location.min-distance-from-spawn", 150);
        maxDistanceFromSpawn = Math.max(minDistanceFromSpawn + 100, cfg.getInt("location.max-distance-from-spawn", 2500));
        targetNearOnlinePlayers = cfg.getBoolean("location.target-near-online-players", true);
        playerOffsetMin = cfg.getInt("location.player-offset-min", 80);
        playerOffsetMax = Math.max(playerOffsetMin + 20, cfg.getInt("location.player-offset-max", 300));
        avoidWater = cfg.getBoolean("location.avoid-water", true);

        viewDistance = Math.max(64, Math.min(512, cfg.getInt("performance.view-distance", 256)));
        updateRateTicks = Math.max(1, cfg.getInt("performance.update-rate-ticks", 1));
        maxParticlesPerTick = Math.max(10, cfg.getInt("performance.max-particles-per-tick", 80));

        omenDurationSeconds = Math.max(3, cfg.getInt("stages.omen-duration-seconds", 10));
        coreLockSeconds = Math.max(0, cfg.getInt("stages.core-lock-seconds", 30));
        lootDurationSeconds = Math.max(10, cfg.getInt("stages.loot-duration-seconds", 120));
        postLootVanishSeconds = Math.max(5, cfg.getInt("stages.post-loot-vanish-seconds", 30));
        defaultExplorationSeconds = Math.max(30, cfg.getInt("stages.default-exploration-seconds", 600));
    }

    private void loadMeteors() {
        meteorTypes.clear();
        File file = new File(plugin.getDataFolder(), "meteors.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = cfg.getConfigurationSection("meteors");
        if (section == null) {
            plugin.getLogger().log(Level.WARNING, "No 'meteors' section found in meteors.yml");
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection ms = section.getConfigurationSection(key);
            if (ms == null) continue;

            String displayName = ChatColor.translateAlternateColorCodes('&', ms.getString("display-name", key));
            String rarity = ms.getString("rarity", "COMMON");
            int weight = Math.max(1, ms.getInt("weight", 10));
            double speed = Math.max(0.1, ms.getDouble("speed", 0.85));
            int startHeight = Math.max(30, ms.getInt("start-height", 110));
            int horizontalOffset = ms.getInt("horizontal-offset", 80);
            String visualEntity = ms.getString("visual-entity", "FIREBALL");
            List<String> trailParticles = ms.getStringList("trail-particles");
            String crossingSound = ms.getString("crossing-sound", "ENTITY_FIREWORK_ROCKET_BLAST");
            String impactSound = ms.getString("impact-sound", "ENTITY_GENERIC_EXPLODE");
            float impactSoundVolume = (float) ms.getDouble("impact-sound-volume", 4.0);
            float impactSoundPitch = (float) ms.getDouble("impact-sound-pitch", 0.8);
            int impactParticlesCount = ms.getInt("impact-particles-count", 100);
            int craterRadius = Math.max(0, ms.getInt("crater-radius", 4));
            int craterDepth = Math.max(0, ms.getInt("crater-depth", 3));
            List<String> craterMaterials = ms.getStringList("crater-materials");
            boolean createFire = ms.getBoolean("create-fire", true);
            String rewardTable = ms.getString("reward-table", "ember_loot");
            String coreType = ms.getString("core-type", "CHEST");
            String coreBlock = ms.getString("core-block", "MAGMA_BLOCK");
            int fragmentCount = Math.max(0, ms.getInt("fragment-count", 4));
            String fragmentBlock = ms.getString("fragment-block", "MAGMA_BLOCK");
            int explorationDuration = Math.max(20, ms.getInt("exploration-duration-seconds", defaultExplorationSeconds));
            String unlockType = ms.getString("unlock-type", "DIRECT");
            int lockDurationSeconds = Math.max(0, ms.getInt("lock-duration-seconds", 0));
            int meteorLootDuration = Math.max(10, ms.getInt("loot-duration-seconds", lootDurationSeconds));
            String requiredKey = ms.getString("required-key", "AIR");
            boolean unstableCore = ms.getBoolean("unstable-core", false);
            float visualEntityScale = (float) ms.getDouble("visual-entity-scale", key.equalsIgnoreCase("worldbreaker") ? 5.5 : 3.8);
            boolean smokePlumesEnabled = ms.getBoolean("smoke-plumes-enabled", true);
            int smokePlumesCount = Math.max(0, ms.getInt("smoke-plumes-count", 12));

            MeteorType type = new MeteorType(key.toLowerCase(Locale.ROOT), displayName, rarity, weight, speed,
                    startHeight, horizontalOffset, visualEntity, trailParticles, crossingSound,
                    impactSound, impactSoundVolume, impactSoundPitch, impactParticlesCount,
                    craterRadius, craterDepth, craterMaterials, createFire, rewardTable,
                    coreType, coreBlock, fragmentCount, fragmentBlock, explorationDuration,
                    unlockType, lockDurationSeconds, meteorLootDuration, requiredKey, unstableCore,
                    visualEntityScale, smokePlumesEnabled, smokePlumesCount);
            meteorTypes.put(key.toLowerCase(Locale.ROOT), type);
        }
    }

    private void loadRewards() {
        rewardTables.clear();
        File file = new File(plugin.getDataFolder(), "rewards.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        participationRewardsEnabled = cfg.getBoolean("mechanics.participation-rewards-enabled", true);
        participationRadius = Math.max(10, cfg.getInt("mechanics.participation-radius", 50));
        participationXp = Math.max(0, cfg.getInt("mechanics.participation-xp", 50));

        ConfigurationSection tables = cfg.getConfigurationSection("tables");
        if (tables == null) return;

        for (String key : tables.getKeys(false)) {
            ConfigurationSection ts = tables.getConfigurationSection(key);
            if (ts == null) continue;

            int minRolls = Math.max(1, ts.getInt("min-rolls", 2));
            int maxRolls = Math.max(minRolls, ts.getInt("max-rolls", 5));

            List<RewardItem> items = new ArrayList<>();
            List<Map<?, ?>> rawItems = ts.getMapList("items");
            for (Map<?, ?> itemMap : rawItems) {
                String mat = String.valueOf(itemMap.get("material"));
                int minAmt = itemMap.containsKey("min-amount") ? ((Number) itemMap.get("min-amount")).intValue() : 1;
                int maxAmt = itemMap.containsKey("max-amount") ? ((Number) itemMap.get("max-amount")).intValue() : minAmt;
                double ch = itemMap.containsKey("chance") ? ((Number) itemMap.get("chance")).doubleValue() : 100.0;
                String name = itemMap.containsKey("name") ? ChatColor.translateAlternateColorCodes('&', String.valueOf(itemMap.get("name"))) : null;
                List<String> lore = new ArrayList<>();
                if (itemMap.containsKey("lore") && itemMap.get("lore") instanceof List) {
                    for (Object l : (List<?>) itemMap.get("lore")) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', String.valueOf(l)));
                    }
                }
                List<String> enchants = new ArrayList<>();
                if (itemMap.containsKey("enchantments") && itemMap.get("enchantments") instanceof List) {
                    for (Object e : (List<?>) itemMap.get("enchantments")) {
                        enchants.add(String.valueOf(e));
                    }
                }
                items.add(new RewardItem(mat, minAmt, maxAmt, ch, name, lore, enchants));
            }

            List<RewardTable.CommandReward> commands = new ArrayList<>();
            List<Map<?, ?>> rawCommands = ts.getMapList("commands");
            for (Map<?, ?> cmdMap : rawCommands) {
                String cmd = String.valueOf(cmdMap.get("command"));
                double ch = cmdMap.containsKey("chance") ? ((Number) cmdMap.get("chance")).doubleValue() : 100.0;
                commands.add(new RewardTable.CommandReward(cmd, ch));
            }

            RewardTable.FragmentReward frag = null;
            ConfigurationSection fs = ts.getConfigurationSection("fragment");
            if (fs != null) {
                String fmat = fs.getString("material", "IRON_NUGGET");
                String fname = ChatColor.translateAlternateColorCodes('&', fs.getString("name", "&6Meteor Fragment"));
                List<String> flore = new ArrayList<>();
                for (String l : fs.getStringList("lore")) {
                    flore.add(ChatColor.translateAlternateColorCodes('&', l));
                }
                int famt = Math.max(1, fs.getInt("amount", 1));
                frag = new RewardTable.FragmentReward(fmat, fname, flore, famt);
            }

            rewardTables.put(key, new RewardTable(key, minRolls, maxRolls, items, commands, frag));
        }
    }

    private void loadMessages() {
        messages.clear();
        File file = new File(plugin.getDataFolder(), "messages.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        String prefix = ChatColor.translateAlternateColorCodes('&', cfg.getString("prefix", "&8[&6MeteorEvents&8]&r "));
        messages.put("prefix", prefix);

        loadStringSubKeys(cfg.getConfigurationSection("announcements"), "announcements.");
        loadStringSubKeys(cfg.getConfigurationSection("rewards"), "rewards.");
        loadStringSubKeys(cfg.getConfigurationSection("admin"), "admin.");
    }

    private void loadStringSubKeys(ConfigurationSection section, String prefixKey) {
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String val = section.getString(key);
            if (val != null) {
                messages.put(prefixKey + key, ChatColor.translateAlternateColorCodes('&', val));
            }
        }
    }

    private void loadProtections() {
        File file = new File(plugin.getDataFolder(), "protections.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        boolean modifyBlocks = cfg.getBoolean("terrain.modify-blocks", true);
        boolean enableRollback = cfg.getBoolean("terrain.enable-rollback", true);
        int rollbackDelaySeconds = Math.max(10, cfg.getInt("terrain.rollback-delay-seconds", 120));
        boolean preservePlayerModifications = cfg.getBoolean("terrain.preserve-player-modifications", true);
        boolean affectTrees = cfg.getBoolean("terrain.affect-trees-and-foliage", true);
        boolean affectFluids = cfg.getBoolean("terrain.affect-fluids", false);
        List<String> blacklisted = cfg.getStringList("terrain.blacklisted-blocks");

        boolean damageEntities = cfg.getBoolean("damage.damage-entities", false);
        double damageAmount = Math.max(0.0, cfg.getDouble("damage.entity-damage-amount", 8.0));
        double knockback = Math.max(0.0, cfg.getDouble("damage.knockback-strength", 1.2));

        boolean wgEnabled = cfg.getBoolean("integrations.worldguard-enabled", true);
        boolean wgRespect = cfg.getBoolean("integrations.respect-worldguard-regions", true);
        List<String> prohibited = cfg.getStringList("prohibited-worlds");

        protections = new ProtectionSettings(modifyBlocks, enableRollback, rollbackDelaySeconds,
                preservePlayerModifications, affectTrees, affectFluids, blacklisted,
                damageEntities, damageAmount, knockback, wgEnabled, wgRespect, prohibited);
    }

    public String getMessage(String path) {
        String msg = messages.get(path);
        if (msg == null) return "";
        return msg;
    }

    public String getFormatted(String path, Map<String, String> placeholders) {
        String msg = getMessage(path);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return msg;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public double getChancePercentage() {
        return chancePercentage;
    }

    public int getMinCooldownSeconds() {
        return minCooldownSeconds;
    }

    public int getMaxConcurrentEvents() {
        return maxConcurrentEvents;
    }

    public int getMinPlayersOnline() {
        return minPlayersOnline;
    }

    public List<String> getAllowedWorlds() {
        return allowedWorlds;
    }

    public int getMinDistanceFromSpawn() {
        return minDistanceFromSpawn;
    }

    public int getMaxDistanceFromSpawn() {
        return maxDistanceFromSpawn;
    }

    public boolean isTargetNearOnlinePlayers() {
        return targetNearOnlinePlayers;
    }

    public int getPlayerOffsetMin() {
        return playerOffsetMin;
    }

    public int getPlayerOffsetMax() {
        return playerOffsetMax;
    }

    public boolean isAvoidWater() {
        return avoidWater;
    }

    public int getViewDistance() {
        return viewDistance;
    }

    public int getUpdateRateTicks() {
        return updateRateTicks;
    }

    public int getMaxParticlesPerTick() {
        return maxParticlesPerTick;
    }

    public int getOmenDurationSeconds() {
        return omenDurationSeconds;
    }

    public int getCoreLockSeconds() {
        return coreLockSeconds;
    }

    public int getLootDurationSeconds() {
        return lootDurationSeconds;
    }

    public int getPostLootVanishSeconds() {
        return postLootVanishSeconds;
    }

    public int getDefaultExplorationSeconds() {
        return defaultExplorationSeconds;
    }

    public boolean isParticipationRewardsEnabled() {
        return participationRewardsEnabled;
    }

    public int getParticipationRadius() {
        return participationRadius;
    }

    public int getParticipationXp() {
        return participationXp;
    }

    public Map<String, MeteorType> getMeteorTypes() {
        return Collections.unmodifiableMap(meteorTypes);
    }

    public MeteorType getMeteorType(String id) {
        if (id == null) return null;
        return meteorTypes.get(id.toLowerCase(Locale.ROOT));
    }

    public RewardTable getRewardTable(String id) {
        if (id == null) return null;
        return rewardTables.get(id);
    }

    public ProtectionSettings getProtections() {
        return protections;
    }
}
