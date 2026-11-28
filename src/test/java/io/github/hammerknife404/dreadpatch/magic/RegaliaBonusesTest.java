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

    @Test
    void theFullSetIsAFifthMoreRimefrostPowerAndEachPieceHasItsOwn() {
        assertEquals(0.20, RegaliaBonuses.RIMEFROST_POWER_PER_PIECE * RegaliaBonuses.PIECES, 1.0E-9);
        for (double own : new double[]{RegaliaBonuses.CROWN_COOLDOWN_REDUCTION, RegaliaBonuses.BODICE_SPELL_POWER,
                RegaliaBonuses.ROBE_MANA_REGEN, RegaliaBonuses.GREAVES_CASTING_MOVESPEED}) {
            org.junit.jupiter.api.Assertions.assertTrue(own > 0.0 && own <= 0.15, "buff " + own);
        }
        assertEquals(1, RegaliaBonuses.IMBUED_SPELLS);
    }
}
