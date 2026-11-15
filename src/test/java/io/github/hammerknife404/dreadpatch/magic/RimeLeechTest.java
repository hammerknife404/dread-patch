package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RimeLeechTest {
    private static final float[] SHARES = {RimeLeech.EASY, RimeLeech.NORMAL, RimeLeech.HARD};

    private static void assertTicks(float maxHealth, float easy, float normal, float hard) {
        float[] expected = {easy, normal, hard};
        for (int i = 0; i < SHARES.length; i++) {
            assertEquals(expected[i], RimeLeech.perTick(maxHealth, SHARES[i]), 1.0E-4F, maxHealth + " health, difficulty " + i);
        }
    }

    @Test
    void aPlayerWithersAtTheFloor() {
        assertTicks(20.0F, 1.0F, 1.0F, 1.0F);
    }

    @Test
    void biggerTargetsFeedHerMoreOnHarderDifficulties() {
        assertTicks(100.0F, 1.0F, 1.5F, 2.0F);
        assertTicks(320.0F, 3.2F, 4.8F, 6.4F);
    }

    @Test
    void aDragonFeedsHerNoMoreThanTheCap() {
        assertTicks(1000.0F, 10.0F, 10.0F, 10.0F);
        assertTicks(500.0F, 5.0F, 7.5F, 10.0F);
    }

    @Test
    void neverBelowTheFloor() {
        assertEquals(RimeLeech.FLOOR, RimeLeech.perTick(0.0F, RimeLeech.HARD));
    }
}
