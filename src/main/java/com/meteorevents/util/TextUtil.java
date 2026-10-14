package com.meteorevents.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final Pattern HEX_AMPERSAND_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern HEX_SECTION_PATTERN = Pattern.compile("§#([0-9a-fA-F]{6})");
    private static final Pattern SPIGOT_HEX_PATTERN = Pattern.compile("[&§]x([&§][0-9a-fA-F]){6}");

    private TextUtil() {}

    public static String format(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        try {
            String converted = convertLegacyToMiniMessage(input);
            Component component = MINI_MESSAGE.deserialize(converted);
            return LEGACY_SERIALIZER.serialize(component);
        } catch (Throwable t) {
            return ChatColor.translateAlternateColorCodes('&', input);
        }
    }

    public static Component formatToComponent(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        try {
            String converted = convertLegacyToMiniMessage(input);
            return MINI_MESSAGE.deserialize(converted);
        } catch (Throwable t) {
            String legacy = ChatColor.translateAlternateColorCodes('&', input);
            return LegacyComponentSerializer.legacySection().deserialize(legacy);
        }
    }

    public static String format(String input, Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String text = input;
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    text = text.replace("{" + entry.getKey() + "}", entry.getValue());
                }
            }
        }
        return format(text);
    }

    private static String convertLegacyToMiniMessage(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String result = text;

        Matcher spigotHexMatcher = SPIGOT_HEX_PATTERN.matcher(result);
        StringBuffer sbSpigot = new StringBuffer();
        while (spigotHexMatcher.find()) {
            String match = spigotHexMatcher.group();
            String hex = match.replaceAll("[&§]x|[&§]", "");
            spigotHexMatcher.appendReplacement(sbSpigot, "<#" + hex + ">");
        }
        spigotHexMatcher.appendTail(sbSpigot);
        result = sbSpigot.toString();

        Matcher hexAmpMatcher = HEX_AMPERSAND_PATTERN.matcher(result);
        StringBuffer sbHexAmp = new StringBuffer();
        while (hexAmpMatcher.find()) {
            hexAmpMatcher.appendReplacement(sbHexAmp, "<#" + hexAmpMatcher.group(1) + ">");
        }
        hexAmpMatcher.appendTail(sbHexAmp);
        result = sbHexAmp.toString();

        Matcher hexSecMatcher = HEX_SECTION_PATTERN.matcher(result);
        StringBuffer sbHexSec = new StringBuffer();
        while (hexSecMatcher.find()) {
            hexSecMatcher.appendReplacement(sbHexSec, "<#" + hexSecMatcher.group(1) + ">");
        }
        hexSecMatcher.appendTail(sbHexSec);
        result = sbHexSec.toString();

        result = result
                .replace("&0", "<black>").replace("§0", "<black>")
                .replace("&1", "<dark_blue>").replace("§1", "<dark_blue>")
                .replace("&2", "<dark_green>").replace("§2", "<dark_green>")
                .replace("&3", "<dark_aqua>").replace("§3", "<dark_aqua>")
                .replace("&4", "<dark_red>").replace("§4", "<dark_red>")
                .replace("&5", "<dark_purple>").replace("§5", "<dark_purple>")
                .replace("&6", "<gold>").replace("§6", "<gold>")
                .replace("&7", "<gray>").replace("§7", "<gray>")
                .replace("&8", "<dark_gray>").replace("§8", "<dark_gray>")
                .replace("&9", "<blue>").replace("§9", "<blue>")
                .replace("&a", "<green>").replace("§a", "<green>")
                .replace("&b", "<aqua>").replace("§b", "<aqua>")
                .replace("&c", "<red>").replace("§c", "<red>")
                .replace("&d", "<light_purple>").replace("§d", "<light_purple>")
                .replace("&e", "<yellow>").replace("§e", "<yellow>")
                .replace("&f", "<white>").replace("§f", "<white>")
                .replace("&k", "<obfuscated>").replace("§k", "<obfuscated>")
                .replace("&l", "<bold>").replace("§l", "<bold>")
                .replace("&m", "<strikethrough>").replace("§m", "<strikethrough>")
                .replace("&n", "<underlined>").replace("§n", "<underlined>")
                .replace("&o", "<italic>").replace("§o", "<italic>")
                .replace("&r", "<reset>").replace("§r", "<reset>");

        return result;
    }
}
