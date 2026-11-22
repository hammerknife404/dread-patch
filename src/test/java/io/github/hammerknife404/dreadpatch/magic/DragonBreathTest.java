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
    void theConeGrowsOutToJustPastItsAimInHalfASecond() {
        assertEquals(12.0, DragonBreath.coneLength(10, 10), EPSILON);
        assertEquals(12.0, DragonBreath.coneLength(10, 40), EPSILON);
        assertEquals(6.0, DragonBreath.coneLength(10, 5), EPSILON);
        assertEquals(DragonBreath.CONE_MAX_LENGTH, DragonBreath.coneLength(100, 40), EPSILON);
        assertEquals(3.6, DragonBreath.coneLength(10, 0), EPSILON);
    }

    @Test
    void particlesAreSentFastEnoughToCarryTheirDistance() {
        assertEquals(30.0 / DragonBreath.PARTICLE_TRAVEL, DragonBreath.particleSpeed(30), EPSILON);
        assertEquals(0.0, DragonBreath.particleSpeed(-1), EPSILON);
    }

    /** Vanilla's particle motion on her breath's flame, smoke and snow, for every lifetime they can have. */
    private static double[] reaches(double speed) {
        double[] out = new double[3];
        out[0] = Double.MAX_VALUE;
        for (int life = DragonBreath.FLAME_LIFE_MIN; life <= DragonBreath.FLAME_LIFE_MAX; life++) {
            double d = DragonBreath.travel(speed, DragonBreath.FLAME_FRICTION, life);
            out[0] = Math.min(out[0], d);
            out[1] = Math.max(out[1], d);
        }
        for (int life = DragonBreath.SMOKE_LIFE_MIN; life <= DragonBreath.SMOKE_LIFE_MAX; life++) {
            double d = DragonBreath.travel(speed, DragonBreath.SMOKE_FRICTION, life);
            out[0] = Math.min(out[0], d);
            out[1] = Math.max(out[1], d);
        }
        return out;
    }

    @Test
    void flameAndSmokeCarryTheDistanceTheyreSentFor() {
        for (double distance : new double[] {3, 10, 20, 30}) {
            double[] r = reaches(DragonBreath.particleSpeed(distance));
            assertTrue(r[0] >= 0.92 * distance, "shortest " + r[0] + " of " + distance);
            assertTrue(r[1] <= DragonBreath.PARTICLE_OVERSHOOT * distance + EPSILON, "longest " + r[1] + " of " + distance);
        }
    }

    @Test
    void noSnowflakePassesItsMark() {
        for (double distance : new double[] {3, 10, 30}) {
            double speed = DragonBreath.snowSpeed(distance);
            for (int life = 16; life <= DragonBreath.SNOW_LIFE_MAX; life++) {
                assertTrue(DragonBreath.travel(speed, DragonBreath.SNOW_FRICTION, life) <= distance + EPSILON, "life " + life);
            }
            assertTrue(DragonBreath.travel(speed, DragonBreath.SNOW_FRICTION, 16) >= 0.5 * distance, "even the shortest-lived goes half way");
        }
    }

    @Test
    void everyParticleEndsWhereTheBreathReachesAndTheStreamFillsIt() {
        for (int stage = 1; stage <= 5; stage++) {
            double splash = DragonBreath.breathRadius(stage);
            for (double length : new double[] {2, 6, 15, 30}) {
                double reach = length + splash;
                for (double roll = 0.0; roll <= 1.0; roll += 0.1) {
                    double sent = DragonBreath.sentDistance(length, splash, roll);
                    assertTrue(reaches(DragonBreath.particleSpeed(sent))[1] <= reach + EPSILON, "sent, stage " + stage + ", length " + length);
                    for (double along = 0.15 * length; along <= length; along += 0.05 * length) {
                        double drift = DragonBreath.driftDistance(length, along, splash, roll);
                        assertTrue(along + reaches(DragonBreath.particleSpeed(drift))[1] <= reach + EPSILON,
                                "set at " + along + ", stage " + stage + ", length " + length);
                    }
                }
                // The far end is still reached: the furthest-sent flame gets at least to the cone's end.
                double furthest = reaches(DragonBreath.particleSpeed(DragonBreath.sentDistance(length, splash, 1.0)))[1];
                assertTrue(furthest >= length, "stage " + stage + ", length " + length + ": furthest " + furthest);
            }
        }
    }
}
