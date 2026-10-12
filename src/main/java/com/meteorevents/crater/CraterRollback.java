package com.meteorevents.crater;

import com.meteorevents.compat.Schedulers;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

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

    private final List<BlockRecord> records = new ArrayList<>();

    public void addRecord(Location loc, Material originalMaterial, BlockData originalData, Material placedMaterial) {
        records.add(new BlockRecord(loc, originalMaterial, originalData, placedMaterial));
    }

    public List<BlockRecord> getRecords() {
        return records;
    }

    public void executeRollback(Plugin plugin, boolean preservePlayerModifications) {
        if (records.isEmpty()) return;

        for (BlockRecord record : records) {
            final Location loc = record.getLocation();
            if (loc == null || loc.getWorld() == null) continue;

            Schedulers.runAt(plugin, loc, () -> {
                Block block = loc.getBlock();
                if (preservePlayerModifications) {
                    Material current = block.getType();
                    if (current != record.getPlacedMaterial() && current != Material.FIRE && current != Material.SOUL_FIRE && current != Material.AIR) {
                        return;
                    }
                }

                if (record.getOriginalData() != null) {
                    block.setBlockData(record.getOriginalData(), false);
                } else {
                    block.setType(record.getOriginalMaterial(), false);
                }
            });
        }
        records.clear();
    }
}
