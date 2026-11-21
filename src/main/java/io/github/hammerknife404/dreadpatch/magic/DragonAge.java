package io.github.hammerknife404.dreadpatch.magic;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;

/**
 * How old Syndrigossa rises, and how much health that gives her. As the boss she's ancient:
 * {@link #ANCIENT_DAYS} old, where Ice and Fire's dragon stats stop growing (health, bite, speed and
 * armor step up each day to day 125), so at full strength: {@link #fullHealth} by difficulty, and in
 * the owner's pack 21 bite, 20 armor. Otherwise she gets Ice and Fire's random age (1-80 days) and
 * grows as an ice dragon does (the owner's call).
 */
public final class DragonAge {
    public static final int ANCIENT_DAYS = 125;
    /** Her health at full strength by difficulty (the owner's numbers), in place of Ice and Fire's {@code dragon.maxHealth}. */
    public static final double HEALTH_EASY = 500.0;
    public static final double HEALTH_NORMAL = 750.0;
    public static final double HEALTH_HARD = 1000.0;
    /** Ice and Fire's hatchling share of a dragon's full health. */
    static final double HATCHLING_SHARE = 0.04;

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

    /** Her health at full strength: {@link #HEALTH_EASY} on Easy (and Peaceful), {@link #HEALTH_NORMAL} on Normal, {@link #HEALTH_HARD} on Hard. */
    public static double fullHealth(Difficulty difficulty) {
        return switch (difficulty) {
            case HARD -> HEALTH_HARD;
            case NORMAL -> HEALTH_NORMAL;
            default -> HEALTH_EASY;
        };
    }

    /**
     * Her max health at this age, grown as Ice and Fire grows a dragon's ({@code refreshDirtyAttributes}):
     * {@link #HATCHLING_SHARE} of {@code full} at day 0, the same step each day to all of it at
     * {@link #ANCIENT_DAYS}, rounded.
     */
    public static double maxHealth(double full, int days) {
        double least = full * HATCHLING_SHARE;
        return Math.round(least + (full - least) / ANCIENT_DAYS * Math.min(days, ANCIENT_DAYS));
    }
}
