package com.meteorevents.listener;

import com.meteorevents.config.ConfigManager;
import com.meteorevents.config.RewardTable;
import com.meteorevents.event.ActiveMeteorEvent;
import com.meteorevents.event.MeteorEventManager;
import com.meteorevents.loot.LootManager;
import com.meteorevents.packet.PacketVisualManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MeteorListener implements Listener {
    private final MeteorEventManager eventManager;
    private final ConfigManager configManager;
    private final LootManager lootManager;
    private final PacketVisualManager visualManager;

    public MeteorListener(MeteorEventManager eventManager, ConfigManager configManager,
                          LootManager lootManager, PacketVisualManager visualManager) {
        this.eventManager = eventManager;
        this.configManager = configManager;
        this.lootManager = lootManager;
        this.visualManager = visualManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        ActiveMeteorEvent meteorEvent = eventManager.findEventWithFragment(block.getLocation());
        if (meteorEvent != null) {
            event.setCancelled(true);
            meteorEvent.removeFragment(block.getLocation());

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
            visualManager.playLocalizedSound(dropLoc, "BLOCK_STONE_BREAK", 2.0f, 1.2f);
            return;
        }

        ActiveMeteorEvent nearEvent = eventManager.findEventNear(block.getLocation(), 4.0);
        if (nearEvent == null) return;

        if (nearEvent.getStage() == ActiveMeteorEvent.Stage.EXPLORATION) {
            boolean isChest = nearEvent.getChestLocation() != null
                    && nearEvent.getChestLocation().getBlockX() == block.getX()
                    && nearEvent.getChestLocation().getBlockY() == block.getY()
                    && nearEvent.getChestLocation().getBlockZ() == block.getZ();

            boolean isCore = nearEvent.getCoreLocation() != null
                    && block.getLocation().distanceSquared(nearEvent.getCoreLocation()) <= 5.0;

            if (isChest || isCore) {
                event.setCancelled(true);
                nearEvent.claimCore(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        ActiveMeteorEvent meteorEvent = eventManager.findEventWithFragment(block.getLocation());
        if (meteorEvent == null) return;

        event.setCancelled(true);
        meteorEvent.removeFragment(block.getLocation());

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
        visualManager.playLocalizedSound(dropLoc, "BLOCK_STONE_BREAK", 2.0f, 1.2f);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ())) {
            return;
        }

        double viewDistSq = (double) configManager.getViewDistance() * configManager.getViewDistance();

        for (ActiveMeteorEvent active : eventManager.getActiveEvents()) {
            if (active.getStage() != ActiveMeteorEvent.Stage.EXPLORATION && active.getStage() != ActiveMeteorEvent.Stage.POST_LOOT_VANISHING) {
                continue;
            }

            Location impact = active.getImpactLocation();
            if (impact.getWorld() == null || !impact.getWorld().equals(to.getWorld())) {
                continue;
            }

            double distSq = to.distanceSquared(impact);
            if (distSq <= viewDistSq && from.distanceSquared(impact) > viewDistSq) {
                visualManager.sendFakeBlocksToPlayer(event.getPlayer(), active.getFakeBlocks());

                if (active.getCoreLocation() != null) {
                    List<String> lines;
                    if (active.getStage() == ActiveMeteorEvent.Stage.POST_LOOT_VANISHING) {
                        lines = Arrays.asList(
                                "&7✦ Remnants Destabilizing ✦",
                                "&7⏳ Vanishing in: &e" + active.getVanishSecondsRemaining() + "s"
                        );
                    } else if (active.getLockSecondsRemaining() > 0) {
                        lines = Arrays.asList(
                                "&6&l✦ " + active.getType().getDisplayName() + " &6&l✦",
                                "&c⏳ Cooling down: &e" + active.getLockSecondsRemaining() + "s"
                        );
                    } else {
                        lines = Arrays.asList(
                                "&a&l✦ " + active.getType().getDisplayName() + " &a&l✦",
                                "&a&l✔ UNLOCKED! &e[Right-Click to Loot]"
                        );
                    }
                    visualManager.resendHologramToPlayer(event.getPlayer(), active.getCoreLocation().clone().add(0, 0.8, 0), lines);
                }
            }
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
