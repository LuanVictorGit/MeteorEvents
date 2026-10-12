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
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
        String formatted = ChatColor.translateAlternateColorCodes('&', msg);
        player.sendMessage(configManager.getMessage("prefix") + formatted);
        try {
            player.sendTitle("", formatted, 0, 30, 10);
        } catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != null && event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block != null) {
            ActiveMeteorEvent coreEvent = eventManager.findEventForChestOrCore(block.getLocation());
            if (coreEvent != null) {
                event.setCancelled(true);
                coreEvent.claimCore(player);
                return;
            }

            ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(block.getLocation());
            if (impactEvent != null) {
                if (!impactEvent.isClaimed()) {
                    event.setCancelled(true);
                    sendProtectedMessage(player);
                    return;
                }

                if (impactEvent.isFragmentLocation(block.getLocation())) {
                    event.setCancelled(true);
                    impactEvent.removeFragment(block.getLocation());

                    RewardTable table = configManager.getRewardTable(impactEvent.getType().getRewardTable());
                    ItemStack fragment = lootManager.createFragmentItem(table);

                    Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
                    if (dropLoc.getWorld() != null) {
                        dropLoc.getWorld().dropItemNaturally(dropLoc, fragment);
                    }

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

                event.setCancelled(true);
                return;
            }
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Location eyeLoc = player.getEyeLocation();
            for (ActiveMeteorEvent active : eventManager.getActiveEvents()) {
                Location coreLoc = active.getCoreLocation();
                if (coreLoc != null && coreLoc.getWorld() != null && coreLoc.getWorld().equals(eyeLoc.getWorld())) {
                    if (coreLoc.distanceSquared(eyeLoc) <= 36.0) {
                        Vector toCore = coreLoc.clone().subtract(eyeLoc).toVector().normalize();
                        Vector dir = eyeLoc.getDirection().normalize();
                        if (toCore.dot(dir) > 0.80) {
                            event.setCancelled(true);
                            active.claimCore(player);
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(block.getLocation());
        if (impactEvent != null) {
            event.setCancelled(true);
            if (!impactEvent.isClaimed()) {
                sendProtectedMessage(event.getPlayer());
                return;
            }

            if (impactEvent.isFragmentLocation(block.getLocation())) {
                impactEvent.removeFragment(block.getLocation());

                RewardTable table = configManager.getRewardTable(impactEvent.getType().getRewardTable());
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(event.getBlock().getLocation());
        if (impactEvent != null) {
            event.setCancelled(true);
            sendProtectedMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(event.getBlock().getLocation());
        if (impactEvent != null) {
            event.setCancelled(true);
            sendProtectedMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(event.getBlockClicked().getLocation());
        if (impactEvent != null) {
            event.setCancelled(true);
            sendProtectedMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(event.getBlock().getLocation());
        if (impactEvent != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(b.getLocation());
            if (impactEvent != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            ActiveMeteorEvent impactEvent = eventManager.findEventInImpactRegion(b.getLocation());
            if (impactEvent != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (ActiveMeteorEvent active : eventManager.getActiveEvents()) {
            event.blockList().removeIf(block -> active.isInImpactRegion(block.getLocation()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (ActiveMeteorEvent active : eventManager.getActiveEvents()) {
            event.blockList().removeIf(block -> active.isInImpactRegion(block.getLocation()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
