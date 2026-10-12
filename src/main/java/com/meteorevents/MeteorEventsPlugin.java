package com.meteorevents;

import com.meteorevents.command.MeteorCommand;
import com.meteorevents.compat.Platform;
import com.meteorevents.config.ConfigManager;
import com.meteorevents.crater.CraterEngine;
import com.meteorevents.event.MeteorEventManager;
import com.meteorevents.listener.MeteorListener;
import com.meteorevents.loot.LootManager;
import com.meteorevents.packet.PacketVisualManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MeteorEventsPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private PacketVisualManager visualManager;
    private CraterEngine craterEngine;
    private LootManager lootManager;
    private MeteorEventManager eventManager;

    @Override
    public void onEnable() {
        getLogger().info("Initializing MeteorEvents " + getDescription().getVersion() + "...");

        if (Platform.isFolia()) {
            getLogger().info("Folia environment detected. Region-based scheduling activated.");
        }
        if (Platform.hasPacketEvents()) {
            getLogger().info("PacketEvents detected. Packet-driven entities and effects enabled.");
        } else {
            getLogger().warning("PacketEvents not found. Falling back to native server effects.");
        }
        if (Platform.hasWorldGuard()) {
            getLogger().info("WorldGuard detected. Protected region check enabled.");
        }

        configManager = new ConfigManager(this);
        configManager.load();

        visualManager = new PacketVisualManager();
        craterEngine = new CraterEngine();
        lootManager = new LootManager(configManager);
        eventManager = new MeteorEventManager(this, configManager, visualManager, craterEngine, lootManager);
        eventManager.start();

        getServer().getPluginManager().registerEvents(new MeteorListener(eventManager, configManager, lootManager), this);

        MeteorCommand commandExecutor = new MeteorCommand(eventManager, configManager);
        PluginCommand cmd = getCommand("meteorevents");
        if (cmd != null) {
            cmd.setExecutor(commandExecutor);
            cmd.setTabCompleter(commandExecutor);
        }

        getLogger().info("MeteorEvents successfully enabled! Registered " + configManager.getMeteorTypes().size() + " meteor types.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down MeteorEvents...");
        if (eventManager != null) {
            eventManager.stop();
        }
        if (visualManager != null) {
            visualManager.cleanupAll();
        }
        getLogger().info("MeteorEvents cleanly disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MeteorEventManager getEventManager() {
        return eventManager;
    }

    public PacketVisualManager getVisualManager() {
        return visualManager;
    }

    public CraterEngine getCraterEngine() {
        return craterEngine;
    }

    public LootManager getLootManager() {
        return lootManager;
    }
}
