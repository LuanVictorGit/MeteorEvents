package com.meteorevents;

import com.meteorevents.config.MeteorType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class MeteorTypeTest {

    @Test
    public void testMeteorTypeCreation() {
        MeteorType type = new MeteorType(
                "ember", "&6Ember Meteor", "COMMON", 50, 0.85,
                110, 80, "FIREBALL",
                Arrays.asList("FLAME", "LAVA"), "ENTITY_FIREWORK_ROCKET_BLAST",
                "ENTITY_GENERIC_EXPLODE", 4.0f, 0.8f, 120,
                4, 3, Arrays.asList("MAGMA_BLOCK", "BASALT"),
                true, "ember_loot", "CHEST", "MAGMA_BLOCK",
                4, "MAGMA_BLOCK", 180, "DIRECT", 0, "AIR", false
        );

        assertEquals("ember", type.getId());
        assertEquals("&6Ember Meteor", type.getDisplayName());
        assertEquals("COMMON", type.getRarity());
        assertEquals(50, type.getWeight());
        assertEquals(0.85, type.getSpeed(), 0.001);
        assertEquals(110, type.getStartHeight());
        assertEquals(80, type.getHorizontalOffset());
        assertEquals("FIREBALL", type.getVisualEntity());
        assertEquals(2, type.getTrailParticles().size());
        assertEquals(4, type.getCraterRadius());
        assertEquals(3, type.getCraterDepth());
        assertTrue(type.isCreateFire());
        assertEquals("ember_loot", type.getRewardTable());
        assertEquals("CHEST", type.getCoreType());
        assertEquals(4, type.getFragmentCount());
        assertEquals(180, type.getExplorationDurationSeconds());
        assertEquals("DIRECT", type.getUnlockType());
        assertFalse(type.isUnstableCore());
    }

    @Test
    public void testNullCollectionsHandled() {
        MeteorType type = new MeteorType(
                "test", "Test", "COMMON", 10, 1.0,
                100, 50, "FIREBALL",
                null, "SOUND", "SOUND", 1.0f, 1.0f, 50,
                2, 1, null, false, "loot", "CHEST", "STONE",
                0, "STONE", 60, "DIRECT", 0, "AIR", false
        );

        assertNotNull(type.getTrailParticles());
        assertTrue(type.getTrailParticles().isEmpty());
        assertNotNull(type.getCraterMaterials());
        assertTrue(type.getCraterMaterials().isEmpty());
    }
}
