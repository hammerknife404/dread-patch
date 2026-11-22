package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RimefrostRaisingTest {
    @Test
    void itRisesWithHalfTheCastersMissingHealthRoundedDown() {
        assertEquals(100.0F, RimefrostRaising.risingHealth(300, 100, 1000), "the queen at 100 of 300");
        assertEquals(7.0F, RimefrostRaising.risingHealth(20, 5, 1000), "a player at 5 of 20: 7.5, rounded down");
        assertEquals(37.0F, RimefrostRaising.risingHealth(300, 225, 1000), "the queen at three quarters");
    }

    @Test
    void neverBelowOneNorAboveItsOwnMax() {
        assertEquals(1.0F, RimefrostRaising.risingHealth(20, 20, 1000), "a caster unhurt");
        assertEquals(1.0F, RimefrostRaising.risingHealth(20, 19, 1000), "half a heart missing");
        assertEquals(60.0F, RimefrostRaising.risingHealth(300, 0, 60), "a hatchling-sized corpse caps it");
    }
}
