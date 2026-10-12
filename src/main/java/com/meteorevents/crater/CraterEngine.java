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
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int centerY = world.getHighestBlockYAt(cx, cz);
        Location baseCenter = new Location(world, cx, centerY, cz);

        Location chestLoc = baseCenter.clone();
        Location coreLoc = chestLoc.clone().add(0.5, 1.2, 0.5);

        if (modifyBlocks) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double distSq = (double) (x * x + z * z);
                    if (distSq > radius * radius) continue;

                    double factor = 1.0 - (distSq / (radius * radius));
                    int localDepth = (int) Math.round(depth * factor);

                    int colX = cx + x;
                    int colZ = cz + z;
                    int colY = world.getHighestBlockYAt(colX, colZ);
                    if (Math.abs(colY - centerY) > 6) {
                        colY = centerY;
                    }

                    int bottomY = Math.max(world.getMinHeight() + 2, colY - localDepth);

                    for (int y = colY + 1; y > bottomY; y--) {
                        Block block = world.getBlockAt(colX, y, colZ);
                        Material current = block.getType();
                        if (blacklist.contains(current)) continue;
                        if (!protections.isAffectFluids() && (current == Material.WATER || current == Material.LAVA)) continue;
                        if (!protections.isAffectTreesAndFoliage() && isFoliage(current)) continue;

                        if (current != Material.AIR) {
                            rollback.addRecord(block.getLocation(), current, block.getBlockData(), Material.AIR);
                            block.setType(Material.AIR, false);
                        }
                    }

                    Block bottomBlock = world.getBlockAt(colX, bottomY, colZ);
                    Material currentBottom = bottomBlock.getType();
                    if (!blacklist.contains(currentBottom)) {
                        Material chosen = craterMaterials.get(random.nextInt(craterMaterials.size()));
                        if (currentBottom != chosen) {
                            rollback.addRecord(bottomBlock.getLocation(), currentBottom, bottomBlock.getBlockData(), chosen);
                            bottomBlock.setType(chosen, false);
                        }
                    }

                    if (type.isCreateFire() && distSq >= (radius - 2) * (radius - 2) && random.nextDouble() < 0.25) {
                        Block fireBlock = world.getBlockAt(colX, bottomY + 1, colZ);
                        Block support = world.getBlockAt(colX, bottomY, colZ);
                        if (fireBlock.getType() == Material.AIR && support.getType().isSolid()
                                && support.getType() != Material.WATER && support.getType() != Material.ICE
                                && support.getType() != Material.BLUE_ICE && support.getType() != Material.PACKED_ICE) {
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
                double dist = (radius * 0.7) + (random.nextDouble() * (radius * 0.25));
                int fx = (int) Math.round(cx + Math.cos(angle) * dist);
                int fz = (int) Math.round(cz + Math.sin(angle) * dist);
                int fy = world.getHighestBlockYAt(fx, fz);

                Block fBlock = world.getBlockAt(fx, fy, fz);
                if (!blacklist.contains(fBlock.getType())) {
                    rollback.addRecord(fBlock.getLocation(), fBlock.getType(), fBlock.getBlockData(), fragMat);
                    fBlock.setType(fragMat, false);
                    fragmentLocs.add(fBlock.getLocation());
                }
            }

            Material coreBlockMat = parseSingleMaterial(type.getCoreBlock(), Material.MAGMA_BLOCK);
            int baseBottomY = Math.max(world.getMinHeight() + 2, centerY - depth);
            Block coreBase = world.getBlockAt(cx, baseBottomY, cz);
            rollback.addRecord(coreBase.getLocation(), coreBase.getType(), coreBase.getBlockData(), coreBlockMat);
            coreBase.setType(coreBlockMat, false);

            Block chestBlock = world.getBlockAt(cx, baseBottomY + 1, cz);
            rollback.addRecord(chestBlock.getLocation(), chestBlock.getType(), chestBlock.getBlockData(), Material.CHEST);
            chestBlock.setType(Material.CHEST, false);

            chestLoc = chestBlock.getLocation();
            coreLoc = chestLoc.clone().add(0.5, 1.2, 0.5);
        }

        applyEntitiesImpact(baseCenter, radius, protections);

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
