package com.meteorevents.config;

import java.util.Collections;
import java.util.List;

public final class RewardTable {

    public static final class CommandReward {
        private final String command;
        private final double chance;

        public CommandReward(String command, double chance) {
            this.command = command;
            this.chance = chance;
        }

        public String getCommand() {
            return command;
        }

        public double getChance() {
            return chance;
        }
    }

    public static final class FragmentReward {
        private final String material;
        private final String name;
        private final List<String> lore;
        private final int amount;

        public FragmentReward(String material, String name, List<String> lore, int amount) {
            this.material = material;
            this.name = name;
            this.lore = lore != null ? lore : Collections.emptyList();
            this.amount = amount;
        }

        public String getMaterial() {
            return material;
        }

        public String getName() {
            return name;
        }

        public List<String> getLore() {
            return lore;
        }

        public int getAmount() {
            return amount;
        }
    }

    private final String id;
    private final int minRolls;
    private final int maxRolls;
    private final List<RewardItem> items;
    private final List<CommandReward> commands;
    private final FragmentReward fragment;

    public RewardTable(String id, int minRolls, int maxRolls, List<RewardItem> items,
                       List<CommandReward> commands, FragmentReward fragment) {
        this.id = id;
        this.minRolls = minRolls;
        this.maxRolls = maxRolls;
        this.items = items != null ? items : Collections.emptyList();
        this.commands = commands != null ? commands : Collections.emptyList();
        this.fragment = fragment;
    }

    public String getId() {
        return id;
    }

    public int getMinRolls() {
        return minRolls;
    }

    public int getMaxRolls() {
        return maxRolls;
    }

    public List<RewardItem> getItems() {
        return items;
    }

    public List<CommandReward> getCommands() {
        return commands;
    }

    public FragmentReward getFragment() {
        return fragment;
    }
}
