package io.github.hammerknife404.dreadpatch.magic;

import net.minecraft.world.entity.MobSpawnType;

/**
 * How old Syndrigossa rises. As the boss she's ancient: {@link #ANCIENT_DAYS} old, where Ice and
 * Fire's dragon stats stop growing (health, bite, speed and armor step up each day to day 125), so at
 * full strength: in the owner's pack 1,000 health, 21 bite, 20 armor. Otherwise she gets Ice and
 * Fire's random age (1-80 days) and grows as an ice dragon does (the owner's call).
 */
public final class DragonAge {
    public static final int ANCIENT_DAYS = 125;

    private DragonAge() {
    }

    /**
     * Whether she rises ancient: spawned by the world (naturally, with its chunks, in a structure, by
     * an event) or raised by another mob. By hand (spawn egg, command, dispenser), from a spawner or
     * bred, she doesn't.
     */
    public static boolean spawnsAncient(MobSpawnType reason) {
        return switch (reason) {
            case NATURAL, CHUNK_GENERATION, STRUCTURE, EVENT, MOB_SUMMONED -> true;
            default -> false;
        };
    }
}
