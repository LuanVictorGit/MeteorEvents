package com.meteorevents.listener;

import com.meteorevents.config.ConfigManager;
import com.meteorevents.config.RewardTable;
import com.meteorevents.event.ActiveMeteorEvent;
import com.meteorevents.event.MeteorEventManager;
import com.meteorevents.loot.LootManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class MeteorListener implements Listener {
    private final MeteorEventManager eventManager;
    private final ConfigManager configManager;
    private final LootManager lootManager;

    public MeteorListener(MeteorEventManager eventManager, ConfigManager configManager, LootManager lootManager) {
        this.eventManager = eventManager;
        this.configManager = configManager;
        this.lootManager = lootManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        ActiveMeteorEvent meteorEvent = eventManager.findEventNear(block.getLocation(), 4.0);
        if (meteorEvent == null) return;

        if (meteorEvent.getStage() != ActiveMeteorEvent.Stage.EXPLORATION) return;

        if (block.getType() == Material.CHEST || block.getLocation().equals(meteorEvent.getChestLocation())
                || (meteorEvent.getCoreLocation() != null && block.getLocation().distanceSquared(meteorEvent.getCoreLocation()) <= 4.0)) {
            Player player = event.getPlayer();
            boolean claimed = meteorEvent.claimCore(player);
            if (!claimed && event.isCancelled()) {
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        ActiveMeteorEvent meteorEvent = eventManager.findEventWithFragment(block.getLocation());
        if (meteorEvent == null) return;

        meteorEvent.removeFragment(block.getLocation());
        event.setDropItems(false);

        RewardTable table = configManager.getRewardTable(meteorEvent.getType().getRewardTable());
        ItemStack fragment = lootManager.createFragmentItem(table);

        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
        if (dropLoc.getWorld() != null) {
            dropLoc.getWorld().dropItemNaturally(dropLoc, fragment);
        }

        Player player = event.getPlayer();
        Map<String, String> ph = new HashMap<>();
        ph.put("fragment", fragment.hasItemMeta() && fragment.getItemMeta().hasDisplayName()
                ? fragment.getItemMeta().getDisplayName() : fragment.getType().name());

        String msg = configManager.getFormatted("rewards.fragment-mined", ph);
        if (!msg.isEmpty()) {
            player.sendMessage(configManager.getMessage("prefix") + msg);
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        for (ActiveMeteorEvent active : eventManager.getActiveEvents()) {
            if (active.getImpactLocation().getWorld() != null
                    && active.getImpactLocation().getWorld().equals(event.getWorld())) {
                active.finishEvent();
            }
        }
    }
}
