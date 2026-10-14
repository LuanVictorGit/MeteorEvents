package com.meteorevents.listener;

import com.meteorevents.config.ConfigManager;
import com.meteorevents.config.RewardTable;
import com.meteorevents.event.ActiveMeteorEvent;
import com.meteorevents.event.MeteorEventManager;
import com.meteorevents.loot.LootManager;
import com.meteorevents.packet.PacketVisualManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MeteorListener implements Listener {
    private final MeteorEventManager eventManager;
    private final ConfigManager configManager;
    private final LootManager lootManager;
    private final PacketVisualManager visualManager;
    private final Map<UUID, Long> messageCooldowns = new HashMap<>();

    public MeteorListener(MeteorEventManager eventManager, ConfigManager configManager,
                          LootManager lootManager, PacketVisualManager visualManager) {
        this.eventManager = eventManager;
        this.configManager = configManager;
        this.lootManager = lootManager;
        this.visualManager = visualManager;
    }

    private void sendProtectedMessage(Player player) {
        if (player == null) return;
        long now = System.currentTimeMillis();
        Long last = messageCooldowns.get(player.getUniqueId());
        if (last != null && now - last < 2000L) {
            return;
        }
        messageCooldowns.put(player.getUniqueId(), now);
        String msg = configManager.getMessage("protections.crater-protected");
        if (msg == null || msg.isEmpty()) {
            msg = "&cThis impact area is protected until the meteor is looted!";
        }
        String formatted = com.meteorevents.util.TextUtil.format(msg);
        player.sendMessage(configManager.getMessage("prefix") + formatted);
        try {
            player.sendTitle("", formatted, 0, 30, 10);
        } catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != null && event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK && block != null) {
            ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(block.getLocation());
            if (impactEvent != null) {
                event.setCancelled(true);
                sendProtectedMessage(player);
                return;
            }
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null) {
            ActiveMeteorEvent coreEvent = eventManager.findEventForChestOrCore(block.getLocation());
            if (coreEvent != null) {
                event.setCancelled(true);
                coreEvent.claimCore(player);
                return;
            }

            ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(block.getLocation());
            if (impactEvent != null) {
                event.setCancelled(true);
                sendProtectedMessage(player);
                return;
            }
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            Location eyeLoc = player.getEyeLocation();
            Vector dir = eyeLoc.getDirection().normalize();
            for (ActiveMeteorEvent active : eventManager.getActiveEvents()) {
                Location coreLoc = active.getCoreLocation();
                if (coreLoc != null && coreLoc.getWorld() != null && coreLoc.getWorld().equals(eyeLoc.getWorld())) {
                    Vector toCore = coreLoc.clone().subtract(eyeLoc).toVector();
                    double dot = toCore.dot(dir);
                    if (dot > 0.0 && dot <= 5.0) {
                        Vector closest = eyeLoc.toVector().clone().add(dir.clone().multiply(dot));
                        if (closest.distanceSquared(coreLoc.toVector()) <= 0.64) {
                            event.setCancelled(true);
                            active.claimCore(player);
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockDamage(BlockDamageEvent event) {
        ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(event.getBlock().getLocation());
        if (impactEvent != null) {
            event.setCancelled(true);
            event.setInstaBreak(false);
            sendProtectedMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreakLowest(BlockBreakEvent event) {
        handleBlockBreak(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreakHighest(BlockBreakEvent event) {
        handleBlockBreak(event);
    }

    private void handleBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(block.getLocation());
        if (impactEvent != null) {
            event.setCancelled(true);
            sendProtectedMessage(event.getPlayer());
            event.getPlayer().sendBlockChange(block.getLocation(), block.getBlockData());
            com.meteorevents.compat.Schedulers.runLaterForEntity(eventManager.getPlugin(), event.getPlayer(), 1L, () -> {
                if (event.getPlayer().isOnline()) {
                    event.getPlayer().sendBlockChange(block.getLocation(), block.getBlockData());
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (eventManager.getActiveEvents().isEmpty()) {
            return;
        }

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
                if (active.getCoreLocation() != null) {
                    List<String> lines;
                    if (active.getStage() == ActiveMeteorEvent.Stage.POST_LOOT_VANISHING) {
                        lines = active.getVanishingHologramLines();
                    } else if (active.getLockSecondsRemaining() > 0) {
                        lines = active.getCooldownHologramLines();
                    } else {
                        lines = active.getUnlockedHologramLines();
                    }
                    visualManager.resendHologramToPlayer(event.getPlayer(), active.getCoreLocation().clone().add(0, 0.8, 0), lines);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        messageCooldowns.remove(event.getPlayer().getUniqueId());
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(event.getBlock().getLocation());
        if (impactEvent != null) {
            event.setCancelled(true);
            sendProtectedMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(event.getBlock().getLocation());
        if (impactEvent != null) {
            event.setCancelled(true);
            sendProtectedMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketFill(PlayerBucketFillEvent event) {
        ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(event.getBlockClicked().getLocation());
        if (impactEvent != null) {
            event.setCancelled(true);
            sendProtectedMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(event.getBlock().getLocation());
        if (impactEvent != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBurn(BlockBurnEvent event) {
        ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(event.getBlock().getLocation());
        if (impactEvent != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockSpread(BlockSpreadEvent event) {
        ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(event.getSource().getLocation());
        if (impactEvent != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockFade(BlockFadeEvent event) {
        ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(event.getBlock().getLocation());
        if (impactEvent != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLeavesDecay(LeavesDecayEvent event) {
        ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(event.getBlock().getLocation());
        if (impactEvent != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(b.getLocation());
            if (impactEvent != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(b.getLocation());
            if (impactEvent != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (ActiveMeteorEvent active : eventManager.getActiveEvents()) {
            event.blockList().removeIf(block -> active.isInImpactRegion(block.getLocation()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (ActiveMeteorEvent active : eventManager.getActiveEvents()) {
            event.blockList().removeIf(block -> active.isInImpactRegion(block.getLocation()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof Chest) {
            Chest chest = (Chest) event.getSource().getHolder();
            ActiveMeteorEvent meteorEvent = eventManager.findEventForChestOrCore(chest.getLocation());
            if (meteorEvent != null && meteorEvent.getLockSecondsRemaining() > 0) {
                event.setCancelled(true);
            }
        }
    }
}
