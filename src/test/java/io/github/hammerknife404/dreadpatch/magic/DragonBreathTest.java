package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
