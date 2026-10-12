package com.meteorevents;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CraterEngineMathTest {

    @Test
    public void testCraterFactorCalculation() {
        int radius = 5;
        int depth = 4;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distSq = (double) (x * x + z * z);
                if (distSq <= radius * radius) {
                    double factor = 1.0 - (distSq / (radius * radius));
                    int localDepth = (int) Math.round(depth * factor);
                    assertTrue(localDepth >= 0 && localDepth <= depth);
                    if (x == 0 && z == 0) {
                        assertEquals(depth, localDepth);
                    }
                }
            }
        }
    }

    @Test
    public void testKnockbackMath() {
        double centerX = 100.0;
        double centerZ = 100.0;
        double entityX = 103.0;
        double entityZ = 104.0;

        double dx = entityX - centerX;
        double dz = entityZ - centerZ;
        double len = Math.sqrt(dx * dx + dz * dz);
        assertEquals(5.0, len, 0.001);

        double normX = dx / len;
        double normZ = dz / len;
        assertEquals(0.6, normX, 0.001);
        assertEquals(0.8, normZ, 0.801);

        double knockback = 1.2;
        double velX = normX * knockback;
        double velZ = normZ * knockback;
        assertEquals(0.72, velX, 0.001);
        assertEquals(0.96, velZ, 0.001);
    }
}
