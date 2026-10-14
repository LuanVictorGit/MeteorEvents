package com.meteorevents.crater;

import com.meteorevents.compat.Schedulers;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CraterRollback {

    public static final class BlockRecord {
        private final Location location;
        private final Material originalMaterial;
        private final BlockData originalData;
        private final Material placedMaterial;

        public BlockRecord(Location location, Material originalMaterial, BlockData originalData, Material placedMaterial) {
            this.location = location;
            this.originalMaterial = originalMaterial;
            this.originalData = originalData;
            this.placedMaterial = placedMaterial;
        }

        public Location getLocation() {
            return location;
        }

        public Material getOriginalMaterial() {
            return originalMaterial;
        }

        public BlockData getOriginalData() {
            return originalData;
        }

        public Material getPlacedMaterial() {
            return placedMaterial;
        }
    }

    private final java.util.Map<String, BlockRecord> records = new java.util.LinkedHashMap<>();

    public void addRecord(Location loc, Material originalMaterial, BlockData originalData, Material placedMaterial) {
        if (loc == null || loc.getWorld() == null) return;
        String key = loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
        BlockRecord existing = records.get(key);
        if (existing == null) {
            records.put(key, new BlockRecord(loc, originalMaterial, originalData, placedMaterial));
        } else {
            records.put(key, new BlockRecord(loc, existing.getOriginalMaterial(), existing.getOriginalData(), placedMaterial));
        }
    }

    public boolean containsLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        String key = loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
        return records.containsKey(key);
    }

    public List<BlockRecord> getRecords() {
        return new ArrayList<>(records.values());
    }

    public void executeRollback(Plugin plugin, boolean preservePlayerModifications) {
        if (records.isEmpty()) return;

        List<BlockRecord> toRollback = new ArrayList<>(records.values());
        records.clear();

        toRollback.sort(java.util.Comparator.comparingInt(r -> r.getLocation().getBlockY()));

        for (BlockRecord record : toRollback) {
            final Location loc = record.getLocation();
            if (loc == null || loc.getWorld() == null) continue;

            Schedulers.runAt(plugin, loc, () -> {
                Block block = loc.getBlock();
                if (block.getState() instanceof org.bukkit.block.Chest) {
                    try {
                        ((org.bukkit.block.Chest) block.getState()).getBlockInventory().clear();
                    } catch (Throwable ignored) {}
                }

                if (preservePlayerModifications) {
                    Material current = block.getType();
                    if (current != record.getPlacedMaterial() && current != Material.FIRE && current != Material.SOUL_FIRE && current != Material.AIR) {
                        return;
                    }
                }

                if (block.getType() == Material.FIRE || block.getType() == Material.SOUL_FIRE) {
                    block.setType(Material.AIR, false);
                }

                if (record.getOriginalData() != null) {
                    block.setBlockData(record.getOriginalData(), false);
                } else {
                    block.setType(record.getOriginalMaterial(), false);
                }
            });
        }
    }
}
