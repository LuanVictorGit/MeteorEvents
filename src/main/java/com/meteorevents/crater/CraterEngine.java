package com.meteorevents.crater;

import com.meteorevents.config.MeteorType;
import com.meteorevents.config.ProtectionSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.*;

public final class CraterEngine {

    public static final class CraterResult {
        private final Location coreLocation;
        private final Location chestLocation;
        private final List<Location> fragmentLocations;
        private final Map<Location, BlockData> fakeBlocks;

        public CraterResult(Location coreLocation, Location chestLocation, List<Location> fragmentLocations, Map<Location, BlockData> fakeBlocks) {
            this.coreLocation = coreLocation;
            this.chestLocation = chestLocation;
            this.fragmentLocations = fragmentLocations != null ? fragmentLocations : Collections.emptyList();
            this.fakeBlocks = fakeBlocks != null ? fakeBlocks : Collections.emptyMap();
        }

        public Location getCoreLocation() {
            return coreLocation;
        }

        public Location getChestLocation() {
            return chestLocation;
        }

        public List<Location> getFragmentLocations() {
            return fragmentLocations;
        }

        public Map<Location, BlockData> getFakeBlocks() {
            return fakeBlocks;
        }
    }

    private final Random random = new Random();

    public CraterResult generateCrater(Location center, MeteorType type, ProtectionSettings protections) {
        World world = center.getWorld();
        if (world == null) {
            return new CraterResult(center, center, Collections.emptyList(), Collections.emptyMap());
        }

        int radius = type.getCraterRadius();
        int depth = type.getCraterDepth();

        List<Material> craterMaterials = parseMaterials(type.getCraterMaterials());
        if (craterMaterials.isEmpty()) {
            craterMaterials.add(Material.MAGMA_BLOCK);
        }

        Map<Location, BlockData> fakeBlocks = new HashMap<>();
        List<Location> fragmentLocs = new ArrayList<>();

        Location chestLoc = center.clone().add(0, -Math.min(depth, 1), 0);
        Location coreLoc = chestLoc.clone().add(0.5, 1.2, 0.5);

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distSq = (double) (x * x + z * z);
                if (distSq > radius * radius) continue;

                double factor = 1.0 - (distSq / (radius * radius));
                int localDepth = (int) Math.round(depth * factor);

                for (int y = -localDepth; y <= 2; y++) {
                    Location loc = new Location(world, cx + x, cy + y, cz + z);
                    if (y >= 0 && y <= 2) {
                        fakeBlocks.put(loc, Material.AIR.createBlockData());
                    } else if (y == -localDepth) {
                        Material chosen = craterMaterials.get(random.nextInt(craterMaterials.size()));
                        fakeBlocks.put(loc, chosen.createBlockData());
                    } else {
                        fakeBlocks.put(loc, Material.AIR.createBlockData());
                    }
                }

                if (type.isCreateFire() && distSq >= (radius - 1) * (radius - 1) && random.nextDouble() < 0.25) {
                    Location fireLoc = new Location(world, cx + x, cy + 1, cz + z);
                    fakeBlocks.put(fireLoc, Material.FIRE.createBlockData());
                }
            }
        }

        Material fragMat = parseSingleMaterial(type.getFragmentBlock(), Material.MAGMA_BLOCK);
        int fragCount = Math.min(type.getFragmentCount(), 16);
        double angleStep = (2 * Math.PI) / Math.max(1, fragCount);

        for (int i = 0; i < fragCount; i++) {
            double angle = i * angleStep + (random.nextDouble() * 0.3);
            double dist = (radius * 0.7) + (random.nextDouble() * (radius * 0.3));
            int fx = (int) Math.round(center.getX() + Math.cos(angle) * dist);
            int fz = (int) Math.round(center.getZ() + Math.sin(angle) * dist);
            int fy = world.getHighestBlockYAt(fx, fz);

            Location fLoc = new Location(world, fx, fy, fz);
            fakeBlocks.put(fLoc, fragMat.createBlockData());
            fragmentLocs.add(fLoc);
        }

        Material coreBlockMat = parseSingleMaterial(type.getCoreBlock(), Material.MAGMA_BLOCK);
        Location baseLoc = new Location(world, center.getBlockX(), center.getBlockY() - Math.min(depth, 1), center.getBlockZ());
        fakeBlocks.put(baseLoc, coreBlockMat.createBlockData());

        Location chestBlockLoc = baseLoc.clone().add(0, 1, 0);
        fakeBlocks.put(chestBlockLoc, Material.CHEST.createBlockData());
        chestLoc = chestBlockLoc;
        coreLoc = chestLoc.clone().add(0.5, 1.3, 0.5);

        applyEntitiesImpact(center, radius, protections);

        return new CraterResult(coreLoc, chestLoc, fragmentLocs, fakeBlocks);
    }

    private void applyEntitiesImpact(Location center, int radius, ProtectionSettings protections) {
        World world = center.getWorld();
        if (world == null) return;

        double radiusDouble = Math.max(3.0, (double) radius + 2.0);
        double radiusSq = radiusDouble * radiusDouble;
        double knockback = protections.getKnockbackStrength();

        for (Entity entity : world.getNearbyEntities(center, radiusDouble, radiusDouble, radiusDouble)) {
            Location eLoc = entity.getLocation();
            double distSq = eLoc.distanceSquared(center);
            if (distSq > radiusSq) continue;

            Vector dir = eLoc.toVector().subtract(center.toVector()).normalize();
            if (dir.lengthSquared() < 0.001) {
                dir = new Vector(0, 1, 0);
            }
            dir.multiply(knockback).setY(Math.max(0.4, dir.getY() * 0.6));
            entity.setVelocity(dir);

            if (protections.isDamageEntities() && entity instanceof LivingEntity) {
                ((LivingEntity) entity).damage(protections.getEntityDamageAmount());
            }
        }
    }

    private List<Material> parseMaterials(List<String> names) {
        List<Material> list = new ArrayList<>();
        if (names == null) return list;
        for (String n : names) {
            try {
                list.add(Material.valueOf(n.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {}
        }
        return list;
    }

    private Material parseSingleMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
