package com.meteorevents.loot;

import com.meteorevents.config.ConfigManager;
import com.meteorevents.config.RewardItem;
import com.meteorevents.config.RewardTable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class LootManager {
    private final ConfigManager configManager;
    private final Random random = new Random();

    public LootManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public List<ItemStack> rollRewards(RewardTable table) {
        List<ItemStack> rolled = new ArrayList<>();
        if (table == null || table.getItems().isEmpty()) {
            return rolled;
        }

        List<RewardItem> available = new ArrayList<>(table.getItems());
        int rolls = table.getMinRolls() + random.nextInt(Math.max(1, table.getMaxRolls() - table.getMinRolls() + 1));

        for (int i = 0; i < rolls; i++) {
            Collections.shuffle(available, random);
            for (RewardItem itemDef : available) {
                if (random.nextDouble() * 100.0 <= itemDef.getChance()) {
                    ItemStack stack = createItemStack(itemDef);
                    if (stack != null) {
                        rolled.add(stack);
                        break;
                    }
                }
            }
        }
        return rolled;
    }

    public void populateChest(Block block, RewardTable table) {
        if (block == null) return;
        block.setType(Material.CHEST, false);

        if (block.getState() instanceof Chest) {
            Chest chest = (Chest) block.getState();
            Inventory inv = chest.getBlockInventory();
            inv.clear();

            List<ItemStack> items = rollRewards(table);
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < inv.getSize(); i++) {
                slots.add(i);
            }
            Collections.shuffle(slots, random);

            for (int i = 0; i < items.size() && i < slots.size(); i++) {
                inv.setItem(slots.get(i), items.get(i));
            }
        }
    }

    public ItemStack createFragmentItem(RewardTable table) {
        if (table == null || table.getFragment() == null) {
            return new ItemStack(Material.IRON_NUGGET);
        }
        RewardTable.FragmentReward frag = table.getFragment();
        Material mat = parseMaterial(frag.getMaterial(), Material.IRON_NUGGET);
        ItemStack stack = new ItemStack(mat, Math.max(1, frag.getAmount()));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (frag.getName() != null && !frag.getName().isEmpty()) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', frag.getName()));
            }
            if (frag.getLore() != null && !frag.getLore().isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String l : frag.getLore()) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', l));
                }
                meta.setLore(coloredLore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public void executeRewardCommands(RewardTable table, Player player) {
        if (table == null || player == null || table.getCommands().isEmpty()) {
            return;
        }
        for (RewardTable.CommandReward cmd : table.getCommands()) {
            if (random.nextDouble() * 100.0 <= cmd.getChance()) {
                String parsed = cmd.getCommand().replace("{player}", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        }
    }

    public void awardParticipation(Location center) {
        if (!configManager.isParticipationRewardsEnabled() || center == null || center.getWorld() == null) {
            return;
        }
        double radius = configManager.getParticipationRadius();
        double radiusSq = radius * radius;
        int xp = configManager.getParticipationXp();

        Map<String, String> ph = new HashMap<>();
        ph.put("xp", String.valueOf(xp));
        String msg = configManager.getFormatted("rewards.participation-reward", ph);

        for (Player player : center.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= radiusSq) {
                if (xp > 0) {
                    player.giveExp(xp);
                }
                if (!msg.isEmpty()) {
                    player.sendMessage(msg);
                }
            }
        }
    }

    private ItemStack createItemStack(RewardItem itemDef) {
        Material mat = parseMaterial(itemDef.getMaterial(), null);
        if (mat == null || mat == Material.AIR) return null;

        int min = itemDef.getMinAmount();
        int max = itemDef.getMaxAmount();
        int amount = min >= max ? min : min + random.nextInt(max - min + 1);
        amount = Math.max(1, Math.min(64, amount));

        ItemStack stack = new ItemStack(mat, amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (itemDef.getName() != null && !itemDef.getName().isEmpty()) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemDef.getName()));
            }
            if (itemDef.getLore() != null && !itemDef.getLore().isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String l : itemDef.getLore()) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', l));
                }
                meta.setLore(coloredLore);
            }
            if (itemDef.getEnchantments() != null) {
                for (String encStr : itemDef.getEnchantments()) {
                    String[] parts = encStr.split(":");
                    if (parts.length == 2) {
                        try {
                            Enchantment enchantment = Enchantment.getByName(parts[0].toUpperCase(Locale.ROOT));
                            int level = Integer.parseInt(parts[1]);
                            if (enchantment != null) {
                                meta.addEnchant(enchantment, level, true);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
