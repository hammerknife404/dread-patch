package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RisenRulesTest {
    @Test
    void aBiteGrowsWithTheCreature() {
        assertEquals(1.45F, RisenRules.bite(3.0), 1e-6, "a cod: an ankle-biter");
        assertEquals(13.0F, RisenRules.bite(80.0), 1e-6, "an elder guardian: a leviathan");
        assertTrue(RisenRules.bite(500.0) > RisenRules.bite(80.0));
    }

    @Test
    void underwaterKindsByWeight() {
        int total = 0;
        int[] counts = new int[RisenRules.WaterKind.values().length];
        for (RisenRules.WaterKind kind : RisenRules.WaterKind.values()) {
            total += kind.weight;
        }
        assertEquals(RisenRules.WATER_WEIGHT, total);
        for (int roll = 0; roll < total; roll++) {
            counts[RisenRules.waterKind(roll).ordinal()]++;
        }
        for (RisenRules.WaterKind kind : RisenRules.WaterKind.values()) {
            assertEquals(kind.weight, counts[kind.ordinal()]);
        }
    }

    @Test
    void theyStirRarely() {
        assertTrue(RisenRules.CAVE_PHANTOM_ONE_IN >= DreadlandsSpawning.UNDEAD_ONE_IN, "rarer than the Dreadlands' stray undead");
        assertTrue(RisenRules.CAVE_WATER_ONE_IN >= DreadlandsSpawning.UNDEAD_ONE_IN);
    }
}
