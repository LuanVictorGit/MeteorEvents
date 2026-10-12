package com.meteorevents.crater;

import com.meteorevents.config.MeteorType;
import com.meteorevents.config.ProtectionSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.*;

public final class CraterEngine {

    public static final class CraterResult {
        private final Location coreLocation;
        private final Location chestLocation;
        private final List<Location> fragmentLocations;

        public CraterResult(Location coreLocation, Location chestLocation, List<Location> fragmentLocations) {
            this.coreLocation = coreLocation;
            this.chestLocation = chestLocation;
            this.fragmentLocations = fragmentLocations != null ? fragmentLocations : Collections.emptyList();
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
    }

    private final Random random = new Random();

    public CraterResult generateCrater(Location center, MeteorType type, ProtectionSettings protections, CraterRollback rollback) {
        World world = center.getWorld();
        if (world == null) {
            return new CraterResult(center, center, Collections.emptyList());
        }

        int radius = type.getCraterRadius();
        int depth = type.getCraterDepth();
        boolean modifyBlocks = protections.isModifyBlocks() && radius > 0;

        List<Material> craterMaterials = parseMaterials(type.getCraterMaterials());
        if (craterMaterials.isEmpty()) {
            craterMaterials.add(Material.MAGMA_BLOCK);
        }

        Set<Material> blacklist = new HashSet<>();
        for (String b : protections.getBlacklistedBlocks()) {
            try {
                blacklist.add(Material.valueOf(b.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {}
        }
        blacklist.add(Material.BEDROCK);
        blacklist.add(Material.BARRIER);

        List<Location> fragmentLocs = new ArrayList<>();
        Location chestLoc = center.clone().add(0, -Math.min(depth, 1), 0);
        Location coreLoc = center.clone().add(0, 1, 0);

        if (modifyBlocks) {
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
                        Block block = world.getBlockAt(cx + x, cy + y, cz + z);
                        Material current = block.getType();

                        if (blacklist.contains(current)) continue;
                        if (!protections.isAffectFluids() && (current == Material.WATER || current == Material.LAVA)) continue;
                        if (!protections.isAffectTreesAndFoliage() && isFoliage(current)) continue;

                        if (y >= 0 && y <= 2) {
                            if (current != Material.AIR) {
                                rollback.addRecord(block.getLocation(), current, block.getBlockData(), Material.AIR);
                                block.setType(Material.AIR, false);
                            }
                        } else if (y == -localDepth) {
                            Material chosen = craterMaterials.get(random.nextInt(craterMaterials.size()));
                            if (current != chosen) {
                                rollback.addRecord(block.getLocation(), current, block.getBlockData(), chosen);
                                block.setType(chosen, false);
                            }
                        } else {
                            if (current != Material.AIR) {
                                rollback.addRecord(block.getLocation(), current, block.getBlockData(), Material.AIR);
                                block.setType(Material.AIR, false);
                            }
                        }
                    }

                    if (type.isCreateFire() && distSq >= (radius - 1) * (radius - 1) && random.nextDouble() < 0.3) {
                        Block fireBlock = world.getBlockAt(cx + x, cy + 1, cz + z);
                        if (fireBlock.getType() == Material.AIR && world.getBlockAt(cx + x, cy, cz + z).getType().isSolid()) {
                            rollback.addRecord(fireBlock.getLocation(), Material.AIR, null, Material.FIRE);
                            fireBlock.setType(Material.FIRE, false);
                        }
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

                Block fBlock = world.getBlockAt(fx, fy, fz);
                if (!blacklist.contains(fBlock.getType())) {
                    rollback.addRecord(fBlock.getLocation(), fBlock.getType(), fBlock.getBlockData(), fragMat);
                    fBlock.setType(fragMat, false);
                    fragmentLocs.add(fBlock.getLocation());
                }
            }

            Material coreBlockMat = parseSingleMaterial(type.getCoreBlock(), Material.MAGMA_BLOCK);
            Block baseBlock = world.getBlockAt(center.getBlockX(), center.getBlockY() - Math.min(depth, 1), center.getBlockZ());
            rollback.addRecord(baseBlock.getLocation(), baseBlock.getType(), baseBlock.getBlockData(), coreBlockMat);
            baseBlock.setType(coreBlockMat, false);

            chestLoc = baseBlock.getLocation().clone().add(0, 1, 0);
            coreLoc = chestLoc.clone().add(0.5, 0.5, 0.5);
        }

        applyEntitiesImpact(center, radius, protections);

        return new CraterResult(coreLoc, chestLoc, fragmentLocs);
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

    private boolean isFoliage(Material m) {
        String name = m.name();
        return name.endsWith("_LEAVES") || name.endsWith("_SAPLING") || name.endsWith("_LOG") || name.endsWith("_WOOD");
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
