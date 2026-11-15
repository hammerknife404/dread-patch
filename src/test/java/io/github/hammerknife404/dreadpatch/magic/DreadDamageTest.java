package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DreadDamageTest {
    private static final float EPSILON = 1.0E-4F;

    @Test
    void takesTheShareOfMaxHealth() {
        assertEquals(1.6F, DreadDamage.maxHealthShare(20.0F, 0.08F, false), EPSILON);     // a player, her staff's bolt
        assertEquals(48.0F, DreadDamage.maxHealthShare(320.0F, 0.15F, false), EPSILON);   // a 320-health golem, her own bolt
        assertEquals(150.0F, DreadDamage.maxHealthShare(1000.0F, 0.15F, false), EPSILON); // a 1,000-health dragon: not a boss, so uncapped
    }

    @Test
    void capsTheShareAgainstBosses() {
        assertEquals(DreadDamage.BOSS_CAP, DreadDamage.maxHealthShare(2000.0F, 0.08F, true), EPSILON);
        assertEquals(8.0F, DreadDamage.maxHealthShare(100.0F, 0.08F, true), EPSILON);    // under the cap, a boss takes the plain share
    }

    @Test
    void neverGoesNegative() {
        assertEquals(0.0F, DreadDamage.maxHealthShare(-5.0F, 0.15F, false), EPSILON);
        assertEquals(0.0F, DreadDamage.maxHealthShare(100.0F, -0.1F, false), EPSILON);
    }
}
