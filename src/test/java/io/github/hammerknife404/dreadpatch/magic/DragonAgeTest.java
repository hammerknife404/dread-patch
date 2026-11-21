package io.github.hammerknife404.dreadpatch.magic;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonAgeTest {
    @Test
    void theWorldsBossRisesAncient() {
        for (MobSpawnType reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION, MobSpawnType.STRUCTURE,
                MobSpawnType.EVENT, MobSpawnType.MOB_SUMMONED}) {
            assertTrue(DragonAge.spawnsAncient(reason), reason.name());
        }
    }

    @Test
    void oneSpawnedByHandOrBredGrowsAsAnIceDragon() {
        for (MobSpawnType reason : new MobSpawnType[]{MobSpawnType.SPAWN_EGG, MobSpawnType.COMMAND, MobSpawnType.DISPENSER,
                MobSpawnType.BREEDING, MobSpawnType.SPAWNER, MobSpawnType.TRIGGERED}) {
            assertFalse(DragonAge.spawnsAncient(reason), reason.name());
        }
    }

    @Test
    void ancientIsWhereIceAndFiresStatsStopGrowingAndPastStageFive() {
        assertEquals(125, DragonAge.ANCIENT_DAYS);
        assertTrue(DragonAge.ANCIENT_DAYS >= 100, "stage 5 from day 100");
    }

    @Test
    void herFullHealthIsTheOwnersByDifficulty() {
        assertEquals(500.0, DragonAge.fullHealth(Difficulty.PEACEFUL));
        assertEquals(500.0, DragonAge.fullHealth(Difficulty.EASY));
        assertEquals(750.0, DragonAge.fullHealth(Difficulty.NORMAL));
        assertEquals(1000.0, DragonAge.fullHealth(Difficulty.HARD));
    }

    @Test
    void herHealthGrowsAsIceAndFireGrowsADragons() {
        // A hatchling's 4%, all of it at 125 days and no more after.
        assertEquals(20.0, DragonAge.maxHealth(500.0, 0));
        assertEquals(40.0, DragonAge.maxHealth(1000.0, 0));
        assertEquals(1000.0, DragonAge.maxHealth(1000.0, DragonAge.ANCIENT_DAYS));
        assertEquals(750.0, DragonAge.maxHealth(750.0, 128));
        // As Ice and Fire's own for its default 500: 104 days (AgeTicks 2,500,000) is 419.
        assertEquals(419.0, DragonAge.maxHealth(500.0, 104));
        double last = 0.0;
        for (int day = 0; day <= DragonAge.ANCIENT_DAYS; day++) {
            double now = DragonAge.maxHealth(1000.0, day);
            assertTrue(now >= last, "day " + day);
            last = now;
        }
    }
}
