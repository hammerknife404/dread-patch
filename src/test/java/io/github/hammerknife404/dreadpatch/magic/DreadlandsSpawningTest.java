package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DreadlandsSpawningTest {
    @Test
    void snowBringsMoreAndAStormMoreAgain() {
        int clear = DreadlandsSpawning.oneIn(DreadlandsSpawning.Weather.CLEAR, DreadlandsSpawning.CLEAR_ONE_IN, DreadlandsSpawning.SNOW_ONE_IN, DreadlandsSpawning.STORM_ONE_IN);
        int snow = DreadlandsSpawning.oneIn(DreadlandsSpawning.Weather.SNOW, DreadlandsSpawning.CLEAR_ONE_IN, DreadlandsSpawning.SNOW_ONE_IN, DreadlandsSpawning.STORM_ONE_IN);
        int storm = DreadlandsSpawning.oneIn(DreadlandsSpawning.Weather.STORM, DreadlandsSpawning.CLEAR_ONE_IN, DreadlandsSpawning.SNOW_ONE_IN, DreadlandsSpawning.STORM_ONE_IN);
        assertTrue(clear > snow && snow > storm, "rarer in clear weather, commonest in a storm");
        assertTrue(snow - storm < clear - snow, "a storm only a little more than snow");
        Random random = new Random(3);
        int hits = 0;
        for (int i = 0; i < 60000; i++) {
            hits += DreadlandsSpawning.comesUp(snow, random) ? 1 : 0;
        }
        assertEquals(1.0 / snow, hits / 60000.0, 0.01);
    }

    @Test
    void zeroMeansNever() {
        Random random = new Random(1);
        for (int i = 0; i < 1000; i++) {
            assertFalse(DreadlandsSpawning.comesUp(0, random));
        }
    }

    @Test
    void groupsAreSmallAndCapped() {
        Random random = new Random(2);
        for (int i = 0; i < 1000; i++) {
            int size = DreadlandsSpawning.groupSize(0, DreadlandsSpawning.CAP, random);
            assertTrue(size >= 1 && size <= 3);
            assertTrue(DreadlandsSpawning.groupSize(7, 8, random) <= 1);
            assertEquals(0, DreadlandsSpawning.groupSize(8, 8, random));
            int followers = DreadlandsSpawning.patrolFollowers(random);
            assertTrue(followers >= 2 && followers <= 4, "a small patrol");
        }
    }

    @Test
    void phantomsComeMoreOftenNearASpire() {
        assertTrue(DreadlandsSpawning.PHANTOM_NEAR_SPIRE_ONE_IN < DreadlandsSpawning.PHANTOM_ONE_IN);
    }
}
