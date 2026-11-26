package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RimeEelRulesTest {
    @Test
    void fromAFishToASquid() {
        assertEquals(0.6, RimeEelRules.scale(0.0) * RimeEelRules.MODEL_LENGTH, 1e-9);
        assertEquals(2.5, RimeEelRules.scale(1.0) * RimeEelRules.MODEL_LENGTH, 1e-9);
        assertEquals(10.0, RimeEelRules.health(RimeEelRules.scale(0.0)));
        assertEquals(18.0, RimeEelRules.health(RimeEelRules.scale(1.0)));
        assertEquals(14.0, RimeEelRules.health(RimeEelRules.scale(0.5)));
        assertEquals(2.0, RimeEelRules.bite(RimeEelRules.scale(0.0)));
        assertEquals(4.0, RimeEelRules.bite(RimeEelRules.scale(1.0)));
        assertEquals(10.0, RimeEelRules.health(0.01), "past its range, its range's ends");
        assertEquals(24.5, RimeEelRules.health(5.0));
    }

    @Test
    void nowAndThenAnAlpha() {
        assertEquals(RimeEelRules.scale(0.3), RimeEelRules.scale(0.3, 7), "most are the usual size");
        double small = RimeEelRules.scale(0.0, 0);
        double big = RimeEelRules.scale(1.0, 0);
        assertEquals(2.5, small * RimeEelRules.MODEL_LENGTH, 1e-9);
        assertEquals(4.0, big * RimeEelRules.MODEL_LENGTH, 1e-9);
        assertTrue(RimeEelRules.alpha(RimeEelRules.scale(0.5, 0)));
        assertFalse(RimeEelRules.alpha(RimeEelRules.scale(1.0)));
        assertEquals(24.5, RimeEelRules.health(big), "on up the same line: 10 + 8 x 3.4 / 1.9");
        assertEquals(5.5, RimeEelRules.bite(big));
    }

    @Test
    void foodHealsAQuarterToOne() {
        assertEquals(0.25F, RimeEelRules.heal(0), "an ink sac");
        assertEquals(0.5F, RimeEelRules.heal(4), "rotten flesh");
        assertEquals(1.0F, RimeEelRules.heal(8), "a steak");
        assertEquals(1.0F, RimeEelRules.heal(20));
    }

    @Test
    void itsAuraEveryFewSeconds() {
        assertEquals(60, RimeEelRules.auraTicks(0.0));
        assertTrue(RimeEelRules.auraTicks(0.999) < 100);
    }

    @Test
    void itLungesOnlyOnLandInRangeOffCooldown() {
        assertTrue(RimeEelRules.lunges(false, true, 3.0, true, 0));
        assertFalse(RimeEelRules.lunges(true, true, 3.0, true, 0), "in water it swims");
        assertFalse(RimeEelRules.lunges(false, false, 3.0, true, 0), "mid-air");
        assertFalse(RimeEelRules.lunges(false, true, 3.0, true, 5), "cooling down");
        assertFalse(RimeEelRules.lunges(false, true, 1.0, true, 0), "already on it");
        assertFalse(RimeEelRules.lunges(false, true, 8.0, true, 0), "too far");
        assertFalse(RimeEelRules.lunges(false, true, 3.0, false, 0), "out of sight");
    }

    @Test
    void itsLureIsOffFarMoreOftenThanOn() {
        int lit = 0;
        int ticks = 200_000;
        boolean differs = false;
        for (int tick = 0; tick < ticks; tick++) {
            if (RimeEelRules.lureLit(1234, tick)) {
                lit++;
            }
            differs |= RimeEelRules.lureLit(1234, tick) != RimeEelRules.lureLit(99, tick);
        }
        double share = lit / (double) ticks;
        assertTrue(share > 0.15 && share < 0.35, "lit " + share);
        assertTrue(differs, "each eel flickers its own way");
        assertEquals(RimeEelRules.lureLit(7, -5), RimeEelRules.lureLit(7, -5));
    }

    @Test
    void darkMeansSixOrLess() {
        assertTrue(RimeEelRules.darkEnough(0, 15), "the Dreadlands' sun is never past 6");
        assertTrue(RimeEelRules.darkEnough(6, 0));
        assertFalse(RimeEelRules.darkEnough(7, 0), "by a torch");
        assertEquals(6, RimeEelRules.light(2, 15));
        assertEquals(3, RimeEelRules.light(3, 0));
    }

    @Test
    void aboutOneInSixHasItsLantern() {
        int with = 0;
        for (int roll = 0; roll < RimeEelRules.LANTERN_ONE_IN; roll++) {
            with += RimeEelRules.spawnsWithLantern(roll) ? 1 : 0;
        }
        double share = (double) with / RimeEelRules.LANTERN_ONE_IN;
        assertTrue(share >= 0.10 && share <= 0.25, "the owner's 10-25%: " + share);
    }

    @Test
    void itsLureReachesOutSearchingAndDrawsInStill() {
        float reach = 0.0F;
        for (int i = 0; i < 40; i++) {
            reach = RimeEelRules.reachToward(reach, true);
        }
        assertEquals(1.0F, reach);
        for (int i = 0; i < 40; i++) {
            reach = RimeEelRules.reachToward(reach, false);
        }
        assertEquals(0.0F, reach);
        // Drawn in, the lantern sits nearer the head (higher and less far forward) than reaching out.
        assertTrue(RimeEelRules.LANTERN_IN[1] > RimeEelRules.LANTERN_OUT[1]);
        assertTrue(RimeEelRules.LANTERN_IN[2] > RimeEelRules.LANTERN_OUT[2]);
    }
}
