package com.meteorevents.compat;

public final class Platform {
    private static Boolean folia;
    private static Boolean packetEvents;
    private static Boolean worldGuard;

    private Platform() {}

    public static boolean isFolia() {
        if (folia != null) {
            return folia;
        }
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        return folia;
    }

    public static boolean hasPacketEvents() {
        if (packetEvents != null) {
            return packetEvents;
        }
        try {
            Class.forName("com.github.retrooper.packetevents.PacketEvents");
            packetEvents = true;
        } catch (ClassNotFoundException e) {
            packetEvents = false;
        }
        return packetEvents;
    }

    public static boolean hasWorldGuard() {
        if (worldGuard != null) {
            return worldGuard;
        }
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            worldGuard = true;
        } catch (ClassNotFoundException e) {
            worldGuard = false;
        }
        return worldGuard;
    }

    public static void resetForTesting() {
        folia = null;
        packetEvents = null;
        worldGuard = null;
    }
}
