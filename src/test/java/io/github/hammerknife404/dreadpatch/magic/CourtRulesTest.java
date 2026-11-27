package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourtRulesTest {
    @Test
    void picksEachKindByItsWeight() {
        Map<CourtRules.Kind, Integer> counts = new EnumMap<>(CourtRules.Kind.class);
        for (int roll = 0; roll < CourtRules.TOTAL_WEIGHT; roll++) {
            counts.merge(CourtRules.pick(roll), 1, Integer::sum);
        }
        for (CourtRules.Kind kind : CourtRules.Kind.values()) {
            assertEquals(kind.weight, counts.getOrDefault(kind, 0), kind.name());
        }
        assertEquals(7, CourtRules.TOTAL_WEIGHT);
    }

    @Test
    void raisesAWaveButNeverPastTheLimit() {
        assertEquals(3, CourtRules.toRaise(0, 6, 3));
        assertEquals(3, CourtRules.toRaise(3, 6, 3));
        assertEquals(1, CourtRules.toRaise(5, 6, 3));
        assertEquals(0, CourtRules.toRaise(6, 6, 3));
        assertEquals(0, CourtRules.toRaise(9, 6, 3));
    }

    @Test
    void aSummonedCourtGrowsWithItsLevel() {
        assertEquals(2, CourtRules.summonCount(1));
        assertEquals(4, CourtRules.summonCount(3));
        assertEquals(6, CourtRules.summonCount(5));
        assertEquals(70, CourtRules.summonSeconds(1));
        assertEquals(110, CourtRules.summonSeconds(5));
        assertEquals(CourtRules.summonCount(1), CourtRules.summonCount(0), "a level below 1 counts as 1");
    }

    @Test
    void byDefaultASummonedCourtFightsOnlyHostileMobs() {
        CourtRules.Terms terms = CourtRules.Terms.DEFAULT;
        for (boolean retaliating : new boolean[] {false, true}) {
            assertTrue(CourtRules.mayFight(CourtRules.Stance.HOSTILE, retaliating, terms));
            assertFalse(CourtRules.mayFight(CourtRules.Stance.FRIENDLY, retaliating, terms), "friendly mobs, even when they hit it");
            assertFalse(CourtRules.mayFight(CourtRules.Stance.PLAYER, retaliating, terms), "players, even when they hit it");
            assertFalse(CourtRules.mayFight(CourtRules.Stance.CASTERS_SIDE, retaliating, terms));
            assertFalse(CourtRules.mayFight(CourtRules.Stance.DREAD, retaliating, terms));
        }
    }

    @Test
    void theConfigLetsItFightBackOrJoinIn() {
        CourtRules.Terms retaliate = new CourtRules.Terms(false, true, false);
        assertTrue(CourtRules.mayFight(CourtRules.Stance.FRIENDLY, true, retaliate));
        assertFalse(CourtRules.mayFight(CourtRules.Stance.FRIENDLY, false, retaliate), "fighting back isn't joining in");
        assertTrue(CourtRules.mayFight(CourtRules.Stance.PLAYER, true, retaliate));
        assertFalse(CourtRules.mayFight(CourtRules.Stance.PLAYER, false, retaliate));

        CourtRules.Terms everything = new CourtRules.Terms(true, true, true);
        assertTrue(CourtRules.mayFight(CourtRules.Stance.FRIENDLY, false, everything));
        assertTrue(CourtRules.mayFight(CourtRules.Stance.PLAYER, false, everything));
        assertFalse(CourtRules.mayFight(CourtRules.Stance.CASTERS_SIDE, true, everything), "never its own side");
        assertFalse(CourtRules.mayFight(CourtRules.Stance.DREAD, true, everything), "never the Dread");
    }

    @Test
    void herCourtGrowsWithDifficulty() {
        assertEquals(4, CourtRules.courtLimit(1));
        assertEquals(6, CourtRules.courtLimit(2));
        assertEquals(8, CourtRules.courtLimit(3));
        assertEquals(4, CourtRules.courtLimit(0));
    }

    @Test
    void pursuersByHealthAndAnyOnAPlayer() {
        assertEquals(1, CourtRules.pursuerCap(4.0F, false));
        assertEquals(1, CourtRules.pursuerCap(10.0F, false));
        assertEquals(2, CourtRules.pursuerCap(20.0F, false));
        assertEquals(10, CourtRules.pursuerCap(100.0F, false));
        assertEquals(Integer.MAX_VALUE, CourtRules.pursuerCap(20.0F, true));
    }

    @Test
    void elevenOnOneChickenSpreadOverTheRest() {
        // The owner's example: 11 of her court all on one chicken (target 0), a dozen other mobs about.
        int members = 11;
        int targets = 13;
        double[][] distance = new double[members][targets];
        int[] current = new int[members];
        int[] caps = new int[targets];
        java.util.Arrays.fill(caps, 1);
        for (int m = 0; m < members; m++) {
            current[m] = 0;
            for (int t = 0; t < targets; t++) {
                distance[m][t] = 1.0 + Math.abs(m - t);
            }
        }
        int[] chosen = CourtRules.spread(distance, current, caps, -1);
        int[] load = new int[targets];
        for (int t : chosen) {
            assertTrue(t >= 0);
            load[t]++;
        }
        for (int t = 0; t < targets; t++) {
            assertTrue(load[t] <= 1, "target " + t + " has " + load[t]);
        }
        // The one nearest the chicken keeps it.
        assertEquals(0, chosen[0]);
    }

    @Test
    void herTargetFirstAndAllOnAPlayer() {
        double[][] distance = {{3, 1}, {2, 1}, {5, 1}};
        int[] current = {1, 1, 1};
        int[] chosen = CourtRules.spread(distance, current, new int[]{Integer.MAX_VALUE, 1}, 0);
        assertArrayEquals(new int[]{0, 0, 0}, chosen);
        // Her target a chicken: the nearest one takes it, the next keeps its own, the last has nowhere and joins her target.
        chosen = CourtRules.spread(distance, current, new int[]{1, 1}, 0);
        assertArrayEquals(new int[]{1, 0, 0}, chosen);
    }

    @Test
    void outOfAFightTheIdleStayIdleAndUnseenPreyIsNotTaken() {
        double inf = Double.POSITIVE_INFINITY;
        double[][] distance = {{1, inf}, {1, inf}, {2, 3}};
        int[] chosen = CourtRules.spread(distance, new int[]{0, 0, -1}, new int[]{1, 1}, -1);
        assertEquals(0, chosen[0]);
        // Crowded off the first and the second out of sight: nowhere with room, so it keeps what it had.
        assertEquals(0, chosen[1]);
        assertEquals(-1, chosen[2]);
    }
}
