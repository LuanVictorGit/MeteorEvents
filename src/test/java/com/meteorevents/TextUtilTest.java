package com.meteorevents;

import com.meteorevents.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TextUtilTest {

    @Test
    public void testLegacyFormatting() {
        String input = "&aHello &cWorld";
        String formatted = TextUtil.format(input);
        assertNotNull(formatted);
        assertTrue(formatted.contains("§aHello") || formatted.contains("§cWorld"));
    }

    @Test
    public void testMiniMessageTags() {
        String input = "<green>Hello</green> <red>World</red>";
        String formatted = TextUtil.format(input);
        assertNotNull(formatted);
        assertTrue(formatted.contains("Hello"));
        assertTrue(formatted.contains("World"));
    }

    @Test
    public void testGradientFormatting() {
        String input = "<gradient:#ff0000:#00ff00>Meteor</gradient>";
        String formatted = TextUtil.format(input);
        assertNotNull(formatted);
        assertTrue(formatted.contains("§x"));
        assertTrue(formatted.contains("M"));
    }

    @Test
    public void testRainbowFormatting() {
        String input = "<rainbow>Meteor Strike</rainbow>";
        String formatted = TextUtil.format(input);
        assertNotNull(formatted);
        assertTrue(formatted.contains("§x"));
        assertEquals("Meteor Strike", org.bukkit.ChatColor.stripColor(formatted));
    }

    @Test
    public void testPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("meteor", "Ember");
        placeholders.put("time", "30");

        String input = "<gold>Meteor {meteor} lands in <yellow>{time}s</yellow>!";
        String formatted = TextUtil.format(input, placeholders);
        assertNotNull(formatted);
        assertTrue(formatted.contains("Ember"));
        assertTrue(formatted.contains("30s"));
    }

    @Test
    public void testComponentConversion() {
        String input = "<gradient:#ff8800:#ffff00>Awesome Meteor</gradient>";
        Component comp = TextUtil.formatToComponent(input);
        assertNotNull(comp);
    }

    @Test
    public void testMalformedFallback() {
        String input = "&cInvalid <notatag> text";
        String formatted = TextUtil.format(input);
        assertNotNull(formatted);
        assertTrue(formatted.contains("Invalid"));
    }
}
