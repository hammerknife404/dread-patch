package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DreadDebuffsTest {
    @Test
    void scuttlersWitherRarelyAtIIAndFrostBriefly() {
        DreadDebuffs.Hit hit = DreadDebuffs.roll(DreadDebuffs.Kind.SCUTTLER, 0.05F, 0.1F);
        assertEquals(PhantomRules.WITHERING_TICKS, hit.witheringTicks());
        assertEquals(1, hit.witheringAmplifier());
        assertEquals(40, hit.rimefrostTicks());
        assertEquals(0, DreadDebuffs.roll(DreadDebuffs.Kind.SCUTTLER, 0.1F, 0.9F).witheringTicks());
        assertEquals(0, DreadDebuffs.roll(DreadDebuffs.Kind.SCUTTLER, 0.1F, 0.9F).rimefrostTicks());
    }

    @Test
    void knightsWitherRarelyAndFrostSometimes() {
        assertEquals(new DreadDebuffs.Hit(PhantomRules.WITHERING_TICKS, 0, 60), DreadDebuffs.roll(DreadDebuffs.Kind.KNIGHT, 0.0F, 0.12F));
        assertEquals(DreadDebuffs.Hit.NONE, DreadDebuffs.roll(DreadDebuffs.Kind.KNIGHT, 0.08F, 0.13F));
    }

    @Test
    void othersAtFivePercentEach() {
        DreadDebuffs.Hit hit = DreadDebuffs.roll(DreadDebuffs.Kind.OTHER, 0.04F, 0.04F);
        assertEquals(PhantomRules.WITHERING_TICKS, hit.witheringTicks());
        assertEquals(PhantomRules.RIMEFROST_TICKS, hit.rimefrostTicks());
        assertEquals(DreadDebuffs.Hit.NONE, DreadDebuffs.roll(DreadDebuffs.Kind.OTHER, 0.05F, 0.05F));
    }

    @Test
    void oneSourceAtATime() {
        assertTrue(DreadDebuffs.lands(null, "a", false));
        assertTrue(DreadDebuffs.lands("a", "a", false));
        assertFalse(DreadDebuffs.lands("a", "b", false));
        assertTrue(DreadDebuffs.lands("a", "queen", true));
        assertTrue(DreadDebuffs.lands("a", null, false));
    }

    @Test
    void siphonUncappedCappedAndSplit() {
        assertEquals(2.0F, DreadDebuffs.siphon(2.0F, 5, 6, 0, false));
        assertEquals(2.0F, DreadDebuffs.siphon(2.0F, 2, 6, 3, false));
        assertEquals(0.0F, DreadDebuffs.siphon(2.0F, 3, 6, 3, false));
        assertEquals(0.5F, DreadDebuffs.siphon(2.0F, 0, 4, 0, true));
        assertEquals(1.0F, DreadDebuffs.siphon(2.0F, 1, 6, 2, true));
        assertEquals(2.0F, DreadDebuffs.siphon(2.0F, 0, 0, 0, true));
    }
}
