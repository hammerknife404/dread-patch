package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhantomRulesTest {
    @Test
    void itsBiteRollsBothApart() {
        Random random = new Random(5);
        int rimefrost = 0;
        int withering = 0;
        int both = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            boolean slow = PhantomRules.rimefrost(random.nextFloat());
            boolean rime = PhantomRules.witheringRime(random.nextFloat());
            rimefrost += slow ? 1 : 0;
            withering += rime ? 1 : 0;
            both += slow && rime ? 1 : 0;
        }
        assertEquals(0.35, rimefrost / (double) n, 0.01);
        assertEquals(0.15, withering / (double) n, 0.01);
        // Independent: both together about 35% of 15%.
        assertEquals(0.35 * 0.15, both / (double) n, 0.005);
    }

    @Test
    void neverMoreThanFourOfASummoners() {
        assertEquals(2, PhantomRules.toCall(0, 2));
        assertEquals(1, PhantomRules.toCall(0, 1));
        assertEquals(1, PhantomRules.toCall(3, 2));
        assertEquals(0, PhantomRules.toCall(4, 2));
        assertEquals(0, PhantomRules.toCall(6, 1));
    }

    @Test
    void itDivesMoreOftenOnHarderDifficulties() {
        for (double roll = 0.0; roll < 1.0; roll += 0.05) {
            int easy = PhantomRules.swoopTicks(PhantomRules.EASY, roll);
            int normal = PhantomRules.swoopTicks(PhantomRules.NORMAL, roll);
            int hard = PhantomRules.swoopTicks(PhantomRules.HARD, roll);
            // Easy is vanilla's 8 to 11 s.
            assertTrue(easy >= 160 && easy < 220, "easy " + easy);
            assertTrue(normal >= 120 && normal < 160, "normal " + normal);
            assertTrue(hard >= 80 && hard < 120, "hard " + hard);
            assertTrue(hard < normal && normal < easy);
        }
        assertTrue(PhantomRules.firstSwoopTicks(PhantomRules.HARD) < PhantomRules.firstSwoopTicks(PhantomRules.NORMAL));
        assertTrue(PhantomRules.firstSwoopTicks(PhantomRules.NORMAL) < PhantomRules.firstSwoopTicks(PhantomRules.EASY));
        assertEquals(10, PhantomRules.firstSwoopTicks(PhantomRules.EASY));
    }
}
