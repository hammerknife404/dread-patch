package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DreadFishRulesTest {
    @Test
    void fromAFishToASquid() {
        assertEquals(0.6, DreadFishRules.scale(0.0) * DreadFishRules.MODEL_LENGTH, 1e-9);
        assertEquals(2.5, DreadFishRules.scale(1.0) * DreadFishRules.MODEL_LENGTH, 1e-9);
        assertEquals(10.0, DreadFishRules.health(DreadFishRules.scale(0.0)));
        assertEquals(18.0, DreadFishRules.health(DreadFishRules.scale(1.0)));
        assertEquals(14.0, DreadFishRules.health(DreadFishRules.scale(0.5)));
        assertEquals(2.0, DreadFishRules.bite(DreadFishRules.scale(0.0)));
        assertEquals(4.0, DreadFishRules.bite(DreadFishRules.scale(1.0)));
        assertEquals(10.0, DreadFishRules.health(0.01), "past its range, its range's ends");
        assertEquals(24.5, DreadFishRules.health(5.0));
    }

    @Test
    void nowAndThenAnAlpha() {
        assertEquals(DreadFishRules.scale(0.3), DreadFishRules.scale(0.3, 7), "most are the usual size");
        double small = DreadFishRules.scale(0.0, 0);
        double big = DreadFishRules.scale(1.0, 0);
        assertEquals(2.5, small * DreadFishRules.MODEL_LENGTH, 1e-9);
        assertEquals(4.0, big * DreadFishRules.MODEL_LENGTH, 1e-9);
        assertTrue(DreadFishRules.alpha(DreadFishRules.scale(0.5, 0)));
        assertFalse(DreadFishRules.alpha(DreadFishRules.scale(1.0)));
        assertEquals(24.5, DreadFishRules.health(big), "on up the same line: 10 + 8 x 3.4 / 1.9");
        assertEquals(5.5, DreadFishRules.bite(big));
    }

    @Test
    void foodHealsAQuarterToOne() {
        assertEquals(0.25F, DreadFishRules.heal(0), "an ink sac");
        assertEquals(0.5F, DreadFishRules.heal(4), "rotten flesh");
        assertEquals(1.0F, DreadFishRules.heal(8), "a steak");
        assertEquals(1.0F, DreadFishRules.heal(20));
    }

    @Test
    void itsAuraEveryFewSeconds() {
        assertEquals(60, DreadFishRules.auraTicks(0.0));
        assertTrue(DreadFishRules.auraTicks(0.999) < 100);
    }

    @Test
    void itLungesOnlyOnLandInRangeOffCooldown() {
        assertTrue(DreadFishRules.lunges(false, true, 3.0, true, 0));
        assertFalse(DreadFishRules.lunges(true, true, 3.0, true, 0), "in water it swims");
        assertFalse(DreadFishRules.lunges(false, false, 3.0, true, 0), "mid-air");
        assertFalse(DreadFishRules.lunges(false, true, 3.0, true, 5), "cooling down");
        assertFalse(DreadFishRules.lunges(false, true, 1.0, true, 0), "already on it");
        assertFalse(DreadFishRules.lunges(false, true, 8.0, true, 0), "too far");
        assertFalse(DreadFishRules.lunges(false, true, 3.0, false, 0), "out of sight");
    }

    @Test
    void itsLureIsOffFarMoreOftenThanOn() {
        int lit = 0;
        int ticks = 200_000;
        boolean differs = false;
        for (int tick = 0; tick < ticks; tick++) {
            if (DreadFishRules.lureLit(1234, tick)) {
                lit++;
            }
            differs |= DreadFishRules.lureLit(1234, tick) != DreadFishRules.lureLit(99, tick);
        }
        double share = lit / (double) ticks;
        assertTrue(share > 0.15 && share < 0.35, "lit " + share);
        assertTrue(differs, "each fish flickers its own way");
        assertEquals(DreadFishRules.lureLit(7, -5), DreadFishRules.lureLit(7, -5));
    }

    @Test
    void darkMeansSixOrLess() {
        assertTrue(DreadFishRules.darkEnough(0, 15), "the Dreadlands' sun is never past 6");
        assertTrue(DreadFishRules.darkEnough(6, 0));
        assertFalse(DreadFishRules.darkEnough(7, 0), "by a torch");
        assertEquals(6, DreadFishRules.light(2, 15));
        assertEquals(3, DreadFishRules.light(3, 0));
    }

    @Test
    void aboutOneInSixHasItsLantern() {
        int with = 0;
        for (int roll = 0; roll < DreadFishRules.LANTERN_ONE_IN; roll++) {
            with += DreadFishRules.spawnsWithLantern(roll) ? 1 : 0;
        }
        double share = (double) with / DreadFishRules.LANTERN_ONE_IN;
        assertTrue(share >= 0.10 && share <= 0.25, "the owner's 10-25%: " + share);
    }

    @Test
    void itsLureReachesOutSearchingAndDrawsInStill() {
        float reach = 0.0F;
        for (int i = 0; i < 40; i++) {
            reach = DreadFishRules.reachToward(reach, true);
        }
        assertEquals(1.0F, reach);
        for (int i = 0; i < 40; i++) {
            reach = DreadFishRules.reachToward(reach, false);
        }
        assertEquals(0.0F, reach);
        // Drawn in, the lantern sits nearer the head (higher and less far forward) than reaching out.
        assertTrue(DreadFishRules.LANTERN_IN[1] > DreadFishRules.LANTERN_OUT[1]);
        assertTrue(DreadFishRules.LANTERN_IN[2] > DreadFishRules.LANTERN_OUT[2]);
    }

    @Test
    void itsBodyStraightensQuicklyAndCurlsSlowly() {
        // Swimming straight up (no ground covered, which vanilla's limb swing would have read as still): straight in 5 ticks.
        float straight = 0.0F;
        for (int i = 0; i < 5; i++) {
            straight = DreadFishRules.straightToward(straight, 0.08, false);
        }
        assertEquals(1.0F, straight, 1.0E-6F);
        // A moment's pause mid-swim barely curls it; stopping for good curls it in 25 ticks.
        float paused = straight;
        for (int i = 0; i < 3; i++) {
            paused = DreadFishRules.straightToward(paused, 0.0, false);
        }
        assertTrue(paused > 0.85F);
        for (int i = 0; i < 25; i++) {
            straight = DreadFishRules.straightToward(straight, 0.0, false);
        }
        assertEquals(0.0F, straight, 1.0E-6F);
        // In the air (a leap, a breach), it's straight however slowly it goes; a slow drift is half-curled.
        assertTrue(DreadFishRules.straightToward(0.9F, 0.0, true) > 0.9F);
        float drift = 0.5F;
        for (int i = 0; i < 40; i++) {
            drift = DreadFishRules.straightToward(drift, 0.03, false);
        }
        assertEquals(0.5F, drift, 1.0E-5F);
        // Never past its ends.
        assertEquals(1.0F, DreadFishRules.straightToward(1.0F, 1.0, true));
        assertEquals(0.0F, DreadFishRules.straightToward(0.0F, 0.0, false));
    }

    @Test
    void anAlphaHitsHardSlowlyAndPlainly() {
        double small = DreadFishRules.scale(0.5);
        double alpha = DreadFishRules.scale(0.5, 0);
        assertFalse(DreadFishRules.alpha(small));
        assertTrue(DreadFishRules.alpha(alpha));
        assertEquals(DreadFishRules.bite(small), DreadFishRules.fullBite(small), "an ordinary fish's bite unchanged");
        assertTrue(DreadFishRules.fullBite(alpha) >= 1.7 * DreadFishRules.bite(alpha), "an alpha's three-quarters harder: " + DreadFishRules.fullBite(alpha));
        assertTrue(DreadFishRules.ALPHA_BITE_TICKS > 2 * DreadFishRules.BITE_TICKS, "slow");
        assertTrue(DreadFishRules.ALPHA_WINDUP_TICKS >= 10, "a wind-up long enough to see coming");
        assertTrue(DreadFishRules.ALPHA_ARMOR >= 3 * DreadFishRules.ARMOR, "heavy armour");
    }

    @Test
    void itLeapsAFewBlocksEveryThreeToEightSeconds() {
        org.junit.jupiter.api.Assertions.assertEquals(60, DreadFishRules.leapCooldown(0.0));
        org.junit.jupiter.api.Assertions.assertEquals(160, DreadFishRules.leapCooldown(0.9999));
        double[] low = DreadFishRules.leap(4.0, 1.5);
        double[] high = DreadFishRules.leap(4.0, 10.0);
        org.junit.jupiter.api.Assertions.assertTrue(high[0] > low[0], "higher prey, a harder leap");
        // Capped: no higher than 4 blocks, by v^2 / 2g.
        org.junit.jupiter.api.Assertions.assertTrue(high[0] * high[0] / (2 * 0.08) < 4.5, "at most about 4 blocks: " + high[0]);
        org.junit.jupiter.api.Assertions.assertTrue(DreadFishRules.inLeapReach(6.0, 2.0));
        org.junit.jupiter.api.Assertions.assertFalse(DreadFishRules.inLeapReach(9.0, 2.0));
        org.junit.jupiter.api.Assertions.assertFalse(DreadFishRules.inLeapReach(3.0, 6.0));
    }

    @Test
    void itsWaveNeverJumpsAsItChangesState() {
        // An old fish (age 100,000 ticks) going from curled on land to straight in water over 20 ticks: the old way,
        // sin(age * rate), moved its phase by age * the rate's change, tens of radians a tick; the phase now moves only
        // by the rate of the moment.
        float wave = 0.0F;
        float wet = 0.0F;
        float straight = 0.0F;
        float oldWay = 0.0F;
        float worstOld = 0.0F;
        for (int tick = 0; tick < 20; tick++) {
            float before = wave;
            wet = DreadFishRules.approach(wet, 1.0F, DreadFishRules.WET_STEP);
            straight = DreadFishRules.straightToward(straight, 0.2, false);
            wave += DreadFishRules.waveRate(wet, straight);
            org.junit.jupiter.api.Assertions.assertTrue(wave - before > 0.0F && wave - before < 0.5F, "step " + (wave - before));
            float old = 100_000.0F * DreadFishRules.waveRate(wet, straight);
            worstOld = Math.max(worstOld, Math.abs(old - oldWay));
            oldWay = old;
        }
        org.junit.jupiter.api.Assertions.assertTrue(worstOld > 100.0F, "the old way's jump " + worstOld);
    }

    @Test
    void itsStatesEaseOverTicks() {
        // In water or not takes several ticks either way; its swing and rate move smoothly between land's and water's.
        float wet = 0.0F;
        int ticks = 0;
        while (wet < 1.0F) {
            wet = DreadFishRules.approach(wet, 1.0F, DreadFishRules.WET_STEP);
            ticks++;
        }
        org.junit.jupiter.api.Assertions.assertTrue(ticks >= 5, "ticks " + ticks);
        float land = DreadFishRules.waveSwing(0.0F, 0.5F);
        float half = DreadFishRules.waveSwing(0.5F, 0.5F);
        float water = DreadFishRules.waveSwing(1.0F, 0.5F);
        org.junit.jupiter.api.Assertions.assertTrue(land > half && half > water);
    }

    @Test
    void itsPitchIsTheWayItWentLevelWhenStill() {
        // Diving (down 0.3, across 0.3 a tick): nose down 45; climbing steeply: nose up, held to 45; barely moving: level.
        org.junit.jupiter.api.Assertions.assertEquals(45.0F, DreadFishRules.movementPitch(-0.3, 0.3), 1.0E-3F);
        org.junit.jupiter.api.Assertions.assertEquals(-DreadFishRules.PITCH_MAX, DreadFishRules.movementPitch(0.8, 0.1), 1.0E-3F);
        org.junit.jupiter.api.Assertions.assertEquals(0.0F, DreadFishRules.movementPitch(0.01, 0.01));
        org.junit.jupiter.api.Assertions.assertEquals(0.0F, DreadFishRules.movementPitch(0.0, 0.4), 1.0E-3F);
    }
}
