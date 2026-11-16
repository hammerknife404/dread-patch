package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DreadDamageTest {
    private static final float EPSILON = 1.0E-4F;

    @Test
    void takesTheShareOfMaxHealth() {
        assertEquals(1.6F, DreadDamage.maxHealthShare(20.0F, 0.08F), EPSILON);   // a player, her staff's bolt
        assertEquals(3.0F, DreadDamage.maxHealthShare(20.0F, 0.15F), EPSILON);   // a player, her own bolt
        assertEquals(15.0F, DreadDamage.maxHealthShare(100.0F, 0.15F), EPSILON); // an iron golem
    }

    @Test
    void capsTheShareAgainstEverything() {
        assertEquals(DreadDamage.CAP, DreadDamage.maxHealthShare(500.0F, 0.15F), EPSILON);  // a warden, not a boss
        assertEquals(DreadDamage.CAP, DreadDamage.maxHealthShare(1000.0F, 0.15F), EPSILON); // a dragon
        assertEquals(DreadDamage.CAP, DreadDamage.maxHealthShare(2000.0F, 0.08F), EPSILON); // a boss
    }

    @Test
    void neverGoesNegative() {
        assertEquals(0.0F, DreadDamage.maxHealthShare(-5.0F, 0.15F), EPSILON);
        assertEquals(0.0F, DreadDamage.maxHealthShare(100.0F, -0.1F), EPSILON);
    }
}
