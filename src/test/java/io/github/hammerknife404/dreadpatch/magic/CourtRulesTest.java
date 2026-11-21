package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

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
}
