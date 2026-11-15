package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
