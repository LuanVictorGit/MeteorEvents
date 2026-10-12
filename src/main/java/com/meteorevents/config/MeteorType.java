package com.meteorevents.config;

import java.util.Collections;
import java.util.List;

public final class MeteorType {
    private final String id;
    private final String displayName;
    private final String rarity;
    private final int weight;
    private final double speed;
    private final int startHeight;
    private final int horizontalOffset;
    private final String visualEntity;
    private final List<String> trailParticles;
    private final String crossingSound;
    private final String impactSound;
    private final float impactSoundVolume;
    private final float impactSoundPitch;
    private final int impactParticlesCount;
    private final int craterRadius;
    private final int craterDepth;
    private final List<String> craterMaterials;
    private final boolean createFire;
    private final String rewardTable;
    private final String coreType;
    private final String coreBlock;
    private final int fragmentCount;
    private final String fragmentBlock;
    private final int explorationDurationSeconds;
    private final String unlockType;
    private final int lockDurationSeconds;
    private final String requiredKey;
    private final boolean unstableCore;
    private final float visualEntityScale;
    private final boolean smokePlumesEnabled;
    private final int smokePlumesCount;

    public MeteorType(String id, String displayName, String rarity, int weight, double speed,
                      int startHeight, int horizontalOffset, String visualEntity,
                      List<String> trailParticles, String crossingSound, String impactSound,
                      float impactSoundVolume, float impactSoundPitch, int impactParticlesCount,
                      int craterRadius, int craterDepth, List<String> craterMaterials,
                      boolean createFire, String rewardTable, String coreType, String coreBlock,
                      int fragmentCount, String fragmentBlock, int explorationDurationSeconds,
                      String unlockType, int lockDurationSeconds, String requiredKey,
                      boolean unstableCore) {
        this(id, displayName, rarity, weight, speed, startHeight, horizontalOffset, visualEntity,
                trailParticles, crossingSound, impactSound, impactSoundVolume, impactSoundPitch,
                impactParticlesCount, craterRadius, craterDepth, craterMaterials, createFire,
                rewardTable, coreType, coreBlock, fragmentCount, fragmentBlock,
                explorationDurationSeconds, unlockType, lockDurationSeconds, requiredKey,
                unstableCore, 3.8f, true, 12);
    }

    public MeteorType(String id, String displayName, String rarity, int weight, double speed,
                      int startHeight, int horizontalOffset, String visualEntity,
                      List<String> trailParticles, String crossingSound, String impactSound,
                      float impactSoundVolume, float impactSoundPitch, int impactParticlesCount,
                      int craterRadius, int craterDepth, List<String> craterMaterials,
                      boolean createFire, String rewardTable, String coreType, String coreBlock,
                      int fragmentCount, String fragmentBlock, int explorationDurationSeconds,
                      String unlockType, int lockDurationSeconds, String requiredKey,
                      boolean unstableCore, float visualEntityScale, boolean smokePlumesEnabled,
                      int smokePlumesCount) {
        this.id = id;
        this.displayName = displayName;
        this.rarity = rarity;
        this.weight = weight;
        this.speed = speed;
        this.startHeight = startHeight;
        this.horizontalOffset = horizontalOffset;
        this.visualEntity = visualEntity;
        this.trailParticles = trailParticles != null ? trailParticles : Collections.emptyList();
        this.crossingSound = crossingSound;
        this.impactSound = impactSound;
        this.impactSoundVolume = impactSoundVolume;
        this.impactSoundPitch = impactSoundPitch;
        this.impactParticlesCount = impactParticlesCount;
        this.craterRadius = craterRadius;
        this.craterDepth = craterDepth;
        this.craterMaterials = craterMaterials != null ? craterMaterials : Collections.emptyList();
        this.createFire = createFire;
        this.rewardTable = rewardTable;
        this.coreType = coreType;
        this.coreBlock = coreBlock;
        this.fragmentCount = fragmentCount;
        this.fragmentBlock = fragmentBlock;
        this.explorationDurationSeconds = explorationDurationSeconds;
        this.unlockType = unlockType;
        this.lockDurationSeconds = lockDurationSeconds;
        this.requiredKey = requiredKey;
        this.unstableCore = unstableCore;
        this.visualEntityScale = visualEntityScale > 0.1f ? visualEntityScale : 3.8f;
        this.smokePlumesEnabled = smokePlumesEnabled;
        this.smokePlumesCount = Math.max(0, smokePlumesCount);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRarity() {
        return rarity;
    }

    public int getWeight() {
        return weight;
    }

    public double getSpeed() {
        return speed;
    }

    public int getStartHeight() {
        return startHeight;
    }

    public int getHorizontalOffset() {
        return horizontalOffset;
    }

    public String getVisualEntity() {
        return visualEntity;
    }

    public List<String> getTrailParticles() {
        return trailParticles;
    }

    public String getCrossingSound() {
        return crossingSound;
    }

    public String getImpactSound() {
        return impactSound;
    }

    public float getImpactSoundVolume() {
        return impactSoundVolume;
    }

    public float getImpactSoundPitch() {
        return impactSoundPitch;
    }

    public int getImpactParticlesCount() {
        return impactParticlesCount;
    }

    public int getCraterRadius() {
        return craterRadius;
    }

    public int getCraterDepth() {
        return craterDepth;
    }

    public List<String> getCraterMaterials() {
        return craterMaterials;
    }

    public boolean isCreateFire() {
        return createFire;
    }

    public String getRewardTable() {
        return rewardTable;
    }

    public String getCoreType() {
        return coreType;
    }

    public String getCoreBlock() {
        return coreBlock;
    }

    public int getFragmentCount() {
        return fragmentCount;
    }

    public String getFragmentBlock() {
        return fragmentBlock;
    }

    public int getExplorationDurationSeconds() {
        return explorationDurationSeconds;
    }

    public String getUnlockType() {
        return unlockType;
    }

    public int getLockDurationSeconds() {
        return lockDurationSeconds;
    }

    public String getRequiredKey() {
        return requiredKey;
    }

    public boolean isUnstableCore() {
        return unstableCore;
    }

    public float getVisualEntityScale() {
        return visualEntityScale;
    }

    public boolean isSmokePlumesEnabled() {
        return smokePlumesEnabled;
    }

    public int getSmokePlumesCount() {
        return smokePlumesCount;
    }
}
