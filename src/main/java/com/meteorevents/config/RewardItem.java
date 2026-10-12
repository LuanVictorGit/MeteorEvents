package com.meteorevents.config;

import java.util.Collections;
import java.util.List;

public final class RewardItem {
    private final String material;
    private final int minAmount;
    private final int maxAmount;
    private final double chance;
    private final String name;
    private final List<String> lore;
    private final List<String> enchantments;

    public RewardItem(String material, int minAmount, int maxAmount, double chance,
                      String name, List<String> lore, List<String> enchantments) {
        this.material = material;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.chance = chance;
        this.name = name;
        this.lore = lore != null ? lore : Collections.emptyList();
        this.enchantments = enchantments != null ? enchantments : Collections.emptyList();
    }

    public String getMaterial() {
        return material;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public double getChance() {
        return chance;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public List<String> getEnchantments() {
        return enchantments;
    }
}
