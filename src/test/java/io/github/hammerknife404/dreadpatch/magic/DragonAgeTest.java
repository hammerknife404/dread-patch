package io.github.hammerknife404.dreadpatch.magic;

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
}
