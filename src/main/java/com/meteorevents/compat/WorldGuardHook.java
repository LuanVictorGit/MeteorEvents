package com.meteorevents.compat;

import org.bukkit.Location;

import java.lang.reflect.Method;

public final class WorldGuardHook {
    private WorldGuardHook() {}

    public static boolean isRegionProtected(Location location) {
        if (!Platform.hasWorldGuard() || location == null || location.getWorld() == null) {
            return false;
        }
        try {
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object wg = wgClass.getMethod("getInstance").invoke(null);
            Object platform = wg.getClass().getMethod("getPlatform").invoke(wg);
            Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);

            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object worldEditWorld = bukkitAdapter.getMethod("adapt", org.bukkit.World.class).invoke(null, location.getWorld());

            Method getMethod = container.getClass().getMethod("get", Class.forName("com.sk89q.worldedit.world.World"));
            Object regionManager = getMethod.invoke(container, worldEditWorld);
            if (regionManager == null) {
                return false;
            }

            Class<?> blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            Object blockVector = blockVector3Class.getMethod("at", double.class, double.class, double.class)
                    .invoke(null, location.getX(), location.getY(), location.getZ());

            Method getApplicable = regionManager.getClass().getMethod("getApplicableRegions", blockVector3Class);
            Object regionSet = getApplicable.invoke(regionManager, blockVector);

            Method sizeMethod = regionSet.getClass().getMethod("size");
            int size = (int) sizeMethod.invoke(regionSet);
            return size > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
