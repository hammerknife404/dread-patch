package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThrallArmsTest {
    @Test
    void everyWeaponTurnsUp() {
        Map<ThrallArms.Weapon, Integer> seen = new EnumMap<>(ThrallArms.Weapon.class);
        for (int i = 0; i < 1000; i++) {
            seen.merge(ThrallArms.weapon(i / 1000.0), 1, Integer::sum);
        }
        assertEquals(ThrallArms.Weapon.values().length, seen.size(), "each of them: " + seen);
        assertEquals(400, seen.get(ThrallArms.Weapon.BOW), 2);
        assertEquals(200, seen.get(ThrallArms.Weapon.CROSSBOW), 2);
        assertEquals(ThrallArms.Weapon.DREAD_SWORD, ThrallArms.weapon(0.9999));
    }

    @Test
    void aCrossbowIsSlowerToReadyAndShootsHarder() {
        assertTrue(ThrallArms.readyTicks(true) > ThrallArms.readyTicks(false));
        assertTrue(ThrallArms.CROSSBOW_VELOCITY > ThrallArms.BOW_VELOCITY);
        assertTrue(ThrallArms.CLOSE_TO < ThrallArms.RANGE);
        assertEquals(2.0F, ThrallArms.inaccuracy(3));
    }
}
