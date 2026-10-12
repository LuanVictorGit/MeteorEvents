package com.meteorevents.config;

import java.util.Collections;
import java.util.List;

public final class ProtectionSettings {
    private final boolean modifyBlocks;
    private final boolean enableRollback;
    private final int rollbackDelaySeconds;
    private final boolean preservePlayerModifications;
    private final boolean affectTreesAndFoliage;
    private final boolean affectFluids;
    private final List<String> blacklistedBlocks;
    private final boolean damageEntities;
    private final double entityDamageAmount;
    private final double knockbackStrength;
    private final boolean worldguardEnabled;
    private final boolean respectWorldguardRegions;
    private final List<String> prohibitedWorlds;

    public ProtectionSettings(boolean modifyBlocks, boolean enableRollback, int rollbackDelaySeconds,
                              boolean preservePlayerModifications, boolean affectTreesAndFoliage,
                              boolean affectFluids, List<String> blacklistedBlocks,
                              boolean damageEntities, double entityDamageAmount,
                              double knockbackStrength, boolean worldguardEnabled,
                              boolean respectWorldguardRegions, List<String> prohibitedWorlds) {
        this.modifyBlocks = modifyBlocks;
        this.enableRollback = enableRollback;
        this.rollbackDelaySeconds = rollbackDelaySeconds;
        this.preservePlayerModifications = preservePlayerModifications;
        this.affectTreesAndFoliage = affectTreesAndFoliage;
        this.affectFluids = affectFluids;
        this.blacklistedBlocks = blacklistedBlocks != null ? blacklistedBlocks : Collections.emptyList();
        this.damageEntities = damageEntities;
        this.entityDamageAmount = entityDamageAmount;
        this.knockbackStrength = knockbackStrength;
        this.worldguardEnabled = worldguardEnabled;
        this.respectWorldguardRegions = respectWorldguardRegions;
        this.prohibitedWorlds = prohibitedWorlds != null ? prohibitedWorlds : Collections.emptyList();
    }

    public boolean isModifyBlocks() {
        return modifyBlocks;
    }

    public boolean isEnableRollback() {
        return enableRollback;
    }

    public int getRollbackDelaySeconds() {
        return rollbackDelaySeconds;
    }

    public boolean isPreservePlayerModifications() {
        return preservePlayerModifications;
    }

    public boolean isAffectTreesAndFoliage() {
        return affectTreesAndFoliage;
    }

    public boolean isAffectFluids() {
        return affectFluids;
    }

    public List<String> getBlacklistedBlocks() {
        return blacklistedBlocks;
    }

    public boolean isDamageEntities() {
        return damageEntities;
    }

    public double getEntityDamageAmount() {
        return entityDamageAmount;
    }

    public double getKnockbackStrength() {
        return knockbackStrength;
    }

    public boolean isWorldguardEnabled() {
        return worldguardEnabled;
    }

    public boolean isRespectWorldguardRegions() {
        return respectWorldguardRegions;
    }

    public List<String> getProhibitedWorlds() {
        return prohibitedWorlds;
    }
}
