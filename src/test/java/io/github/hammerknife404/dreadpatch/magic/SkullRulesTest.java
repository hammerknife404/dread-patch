package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkullRulesTest {
    @Test
    void oneALevelUpToFive() {
        assertEquals(1, SkullRules.skulls(1));
        assertEquals(5, SkullRules.skulls(5));
        assertEquals(5, SkullRules.skulls(9));
    }

    @Test
    void differentTargetsFirst() {
        Set<Integer> seen = new HashSet<>();
        for (int skull = 0; skull < 3; skull++) {
            seen.add(SkullRules.targetFor(skull, 3));
        }
        assertEquals(3, seen.size());
        // Five skulls, two targets: shared round.
        assertEquals(0, SkullRules.targetFor(2, 2));
        assertEquals(1, SkullRules.targetFor(3, 2));
        assertEquals(-1, SkullRules.targetFor(0, 0));
    }

    @Test
    void itTurnsHarderUpClose() {
        assertEquals(1.0, SkullRules.turn(2.0));
        assertEquals(SkullRules.TURN, SkullRules.turn(10.0));
    }

    @Test
    void theVolleyFans() {
        assertEquals(0.0F, SkullRules.fan(0, 1));
        assertEquals(-SkullRules.FAN, SkullRules.fan(0, 5));
        assertEquals(SkullRules.FAN, SkullRules.fan(4, 5));
        assertEquals(0.0F, SkullRules.fan(2, 5), 1.0E-6);
    }
}
