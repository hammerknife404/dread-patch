package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegaliaBonusesTest {
    @Test
    void eachPieceTakesFivePercent() {
        assertEquals(100, RegaliaBonuses.manaCost(100, 0));
        assertEquals(95, RegaliaBonuses.manaCost(100, 1));
        assertEquals(80, RegaliaBonuses.manaCost(100, 4));
        assertEquals(80, RegaliaBonuses.manaCost(100, 9), "never past the four pieces");
        // Rimefrost damage: more dealt, less taken.
        assertEquals(24.0F, RegaliaBonuses.rimefrostDamageDealt(20.0F, 4), 1e-6F);
        assertEquals(21.0F, RegaliaBonuses.rimefrostDamageDealt(20.0F, 1), 1e-6F);
        assertEquals(20.0F, RegaliaBonuses.rimefrostDamageDealt(20.0F, 0), 1e-6F);
        assertEquals(16.0F, RegaliaBonuses.rimefrostDamageTaken(20.0F, 4), 1e-6F);
        assertEquals(19.0F, RegaliaBonuses.rimefrostDamageTaken(20.0F, 1), 1e-6F);
    }

    @Test
    void theSetIsAllFour() {
        assertTrue(RegaliaBonuses.fullSet(4));
        assertFalse(RegaliaBonuses.fullSet(3));
    }
}
