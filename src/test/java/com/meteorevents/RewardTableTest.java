package com.meteorevents;

import com.meteorevents.config.RewardItem;
import com.meteorevents.config.RewardTable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class RewardTableTest {

    @Test
    public void testRewardTableStructure() {
        RewardItem item1 = new RewardItem("DIAMOND", 1, 3, 50.0, "&bStar Diamond", Collections.singletonList("&7Shiny"), Collections.singletonList("UNBREAKING:3"));
        RewardTable.CommandReward cmd = new RewardTable.CommandReward("xp add {player} 100", 100.0);
        RewardTable.FragmentReward frag = new RewardTable.FragmentReward("IRON_NUGGET", "&6Ember Shard", Collections.singletonList("&7Fragment"), 1);

        RewardTable table = new RewardTable("test_table", 2, 5, Collections.singletonList(item1), Collections.singletonList(cmd), frag);

        assertEquals("test_table", table.getId());
        assertEquals(2, table.getMinRolls());
        assertEquals(5, table.getMaxRolls());
        assertEquals(1, table.getItems().size());
        assertEquals("DIAMOND", table.getItems().get(0).getMaterial());
        assertEquals(1, table.getItems().get(0).getMinAmount());
        assertEquals(3, table.getItems().get(0).getMaxAmount());
        assertEquals(50.0, table.getItems().get(0).getChance(), 0.001);
        assertEquals(1, table.getCommands().size());
        assertEquals("xp add {player} 100", table.getCommands().get(0).getCommand());
        assertEquals(100.0, table.getCommands().get(0).getChance(), 0.001);
        assertNotNull(table.getFragment());
        assertEquals("IRON_NUGGET", table.getFragment().getMaterial());
    }

    @Test
    public void testEmptyRewardTable() {
        RewardTable table = new RewardTable("empty", 1, 1, null, null, null);
        assertNotNull(table.getItems());
        assertTrue(table.getItems().isEmpty());
        assertNotNull(table.getCommands());
        assertTrue(table.getCommands().isEmpty());
        assertNull(table.getFragment());
    }
}
