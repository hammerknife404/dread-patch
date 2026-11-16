package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonBreathTest {
    private static final double PACK = 2.1; // the owner's pack's attackDamageIce
    private static final float EPSILON = 1.0E-4F;

    @Test
    void breathHitsAsHardAsAnIceDragonsOfHerStage() {
        assertEquals(2.1F, DragonBreath.breathDamage(1, PACK), EPSILON);
        assertEquals(10.5F, DragonBreath.breathDamage(5, PACK), EPSILON);
        assertEquals(12.5F, DragonBreath.breathDamage(5, 2.5), EPSILON); // Ice and Fire's default
    }

    @Test
    void reachGrowsWithHer() {
        assertEquals(1.5, DragonBreath.breathRadius(1), EPSILON);
        assertEquals(3.5, DragonBreath.breathRadius(5), EPSILON);
    }

    @Test
    void stagesOutsideOneToFiveCountAsTheNearest() {
        assertEquals(DragonBreath.breathDamage(1, PACK), DragonBreath.breathDamage(0, PACK), EPSILON);
        assertEquals(DragonBreath.breathDamage(5, PACK), DragonBreath.breathDamage(9, PACK), EPSILON);
    }

    @Test
    void shardsHitLight() {
        assertEquals(5.25F, DragonBreath.shardDamage(5, PACK), EPSILON);
        assertEquals(0.0F, DragonBreath.shardDamage(5, -1.0), EPSILON);
    }

    @Test
    void theConeWidensWithDistance() {
        // Along +x, 20 blocks long, no padding: tan(20°) ≈ 0.364.
        assertTrue(DragonBreath.inCone(10, 0, 0, 1, 0, 0, 20, 0));
        assertTrue(DragonBreath.inCone(10, 3.5, 0, 1, 0, 0, 20, 0), "3.5 off at 10 out: inside 3.64");
        assertFalse(DragonBreath.inCone(10, 3.8, 0, 1, 0, 0, 20, 0), "3.8 off at 10 out");
        assertFalse(DragonBreath.inCone(2, 1.0, 0, 1, 0, 0, 20, 0), "near the mouth it's narrow");
        assertTrue(DragonBreath.inCone(2, 1.0, 0, 1, 0, 0, 20, 0.5), "a body's width counts");
    }

    @Test
    void theConeHasAFrontAndAnEnd() {
        assertFalse(DragonBreath.inCone(-1, 0, 0, 1, 0, 0, 20, 0), "behind her");
        assertFalse(DragonBreath.inCone(21, 0, 0, 1, 0, 0, 20, 0), "past its end");
    }

    @Test
    void theConeGrowsOutToJustPastItsAim() {
        assertEquals(12.0, DragonBreath.coneLength(10, 40), EPSILON);
        assertEquals(6.0, DragonBreath.coneLength(10, 20), EPSILON);
        assertEquals(DragonBreath.CONE_MAX_LENGTH, DragonBreath.coneLength(100, 40), EPSILON);
        assertEquals(1.8, DragonBreath.coneLength(10, 0), EPSILON);
    }
}
