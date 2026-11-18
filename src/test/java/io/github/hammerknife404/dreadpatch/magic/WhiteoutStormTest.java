package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhiteoutStormTest {
    @Test
    void hersIsTheOwnersNumbers() {
        assertEquals(32.0F, WhiteoutStorm.QUEEN_RADIUS);
        assertEquals(45 * 20, WhiteoutStorm.QUEEN_LIFETIME);
        assertEquals(120 * 20, WhiteoutStorm.QUEEN_COOLDOWN);
    }

    @Test
    void aCastersGrowsToHersAtLevelFive() {
        assertEquals(16.0F, WhiteoutStorm.radius(1));
        assertEquals(25 * 20, WhiteoutStorm.lifetime(1));
        assertEquals(WhiteoutStorm.QUEEN_RADIUS, WhiteoutStorm.radius(5));
        assertEquals(WhiteoutStorm.QUEEN_LIFETIME, WhiteoutStorm.lifetime(5));
        for (int level = 1; level < 5; level++) {
            assertTrue(WhiteoutStorm.radius(level) < WhiteoutStorm.radius(level + 1));
            assertTrue(WhiteoutStorm.lifetime(level) < WhiteoutStorm.lifetime(level + 1));
        }
    }

    @Test
    void holdsATallCylinder() {
        assertTrue(WhiteoutStorm.contains(0, 0, 0, 32));
        assertTrue(WhiteoutStorm.contains(22.0, 0, 22.0, 32));     // 31.1 out, diagonally
        assertFalse(WhiteoutStorm.contains(23.0, 0, 23.0, 32));    // 32.5
        assertTrue(WhiteoutStorm.contains(0, -16.0, 0, 32));       // down a slope
        assertFalse(WhiteoutStorm.contains(0, -16.1, 0, 32));
        assertTrue(WhiteoutStorm.contains(10, 48.0, 0, 32));       // her dragon overhead
        assertFalse(WhiteoutStorm.contains(0, 48.1, 0, 32));
        assertFalse(WhiteoutStorm.contains(17.0, 0, 0, 16), "a level 1 storm's edge");
    }

    @Test
    void freezesEveryThreeSecondsNotAtOnce() {
        assertFalse(WhiteoutStorm.freezesAt(0));
        assertFalse(WhiteoutStorm.freezesAt(1));
        assertTrue(WhiteoutStorm.freezesAt(60));
        assertFalse(WhiteoutStorm.freezesAt(61));
        assertTrue(WhiteoutStorm.freezesAt(120));
        int hits = 0;
        for (int age = 1; age < WhiteoutStorm.QUEEN_LIFETIME; age++) {
            if (WhiteoutStorm.freezesAt(age)) {
                hits++;
            }
        }
        assertEquals(14, hits, "a point every 3 s over her 45 s (not at its last tick, when it's gone)");
    }

    @Test
    void itsSnowDriftsInOverTenSecondsAndMeltsAsItClears() {
        int life = WhiteoutStorm.QUEEN_LIFETIME;
        assertEquals(0.0F, WhiteoutStorm.cover(0, life));
        assertEquals(0.5F, WhiteoutStorm.cover(100, life), 1.0E-6F);
        assertEquals(1.0F, WhiteoutStorm.cover(200, life));
        assertEquals(1.0F, WhiteoutStorm.cover(life - 60, life));
        assertEquals(0.5F, WhiteoutStorm.cover(life - 30, life), 1.0E-6F);
        assertEquals(0.0F, WhiteoutStorm.cover(life, life));
        // Cut short before it's all down (hers, when she falls 5 s in): it melts from where it got to.
        assertEquals(0.5F, WhiteoutStorm.cover(100, 160), 1.0E-6F);
        assertEquals(0.5F, WhiteoutStorm.cover(130, 160), 1.0E-6F, "it lies until the melt reaches it");
        assertTrue(WhiteoutStorm.cover(150, 160) < 0.2F);
    }

    @Test
    void itSettlesInPatchesAndLiesWholeAtFullCover() {
        assertEquals(1.0F, WhiteoutStorm.settled(1.0F, 0.0F));
        assertEquals(1.0F, WhiteoutStorm.settled(1.0F, 0.999F), "every block, at full cover");
        assertEquals(0.0F, WhiteoutStorm.settled(0.0F, 0.0F));
        assertEquals(0.0F, WhiteoutStorm.settled(0.3F, 0.9F), "a late patch hasn't begun");
        assertTrue(WhiteoutStorm.settled(0.3F, 0.1F) > 0.5F, "an early one is well down");
    }

    @Test
    void whatDoesntRiseAsIceAndFireRaisesItRisesByItsShape() {
        assertTrue(WhiteoutStorm.risesAsThrall(0.6F, 1.95F), "a villager or an illager");
        assertTrue(WhiteoutStorm.risesAsThrall(0.6F, 1.7F), "a creeper");
        assertTrue(WhiteoutStorm.risesAsThrall(0.6F, 2.9F), "an enderman");
        assertTrue(WhiteoutStorm.risesAsThrall(0.9F, 2.9F), "a warden");
        assertFalse(WhiteoutStorm.risesAsThrall(2.04F, 2.04F), "a big slime");
        assertFalse(WhiteoutStorm.risesAsThrall(1.95F, 2.2F), "a ravager");
        assertFalse(WhiteoutStorm.risesAsThrall(0.5F, 0.9F), "a bat");
        assertFalse(WhiteoutStorm.risesAsThrall(0.4F, 0.8F), "a vex: upright, but under a block");
        assertEquals(1.0F, WhiteoutStorm.beastSize(1.2F), 1.0E-6F);
        assertEquals(WhiteoutStorm.BEAST_SIZE_MIN, WhiteoutStorm.beastSize(0.3F), "a fish isn't a speck");
        assertEquals(WhiteoutStorm.BEAST_SIZE_MAX, WhiteoutStorm.beastSize(4.0F), "a ghast isn't a giant");
    }

    @Test
    void buildsUpAndClears() {
        int life = WhiteoutStorm.QUEEN_LIFETIME;
        assertEquals(0.0F, WhiteoutStorm.strength(0, life));
        assertEquals(0.5F, WhiteoutStorm.strength(30, life), 1.0E-6F);
        assertEquals(1.0F, WhiteoutStorm.strength(60, life));
        assertEquals(1.0F, WhiteoutStorm.strength(450, life));
        assertEquals(0.5F, WhiteoutStorm.strength(life - 30, life), 1.0E-6F);
        assertEquals(0.0F, WhiteoutStorm.strength(life, life));
        assertEquals(0.0F, WhiteoutStorm.strength(life + 5, life), "never below nothing");
        // Hers, cut short when she falls at 15 s: its lifetime becomes now + 60, and it clears from full.
        assertEquals(1.0F, WhiteoutStorm.strength(300, 360));
        assertEquals(0.5F, WhiteoutStorm.strength(330, 360), 1.0E-6F);
    }
}
