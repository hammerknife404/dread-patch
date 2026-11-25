package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free rules for Sublimate (0.74.0, the owner's Feature_Dump_11-24 "Whiteout" spell, renamed: "a brief
 * near instant cast piercing cone"): a burst of rime that turns straight from ice to vapour, a cone from the
 * caster's eyes that passes through everyone in it (blocks stop it) and leaves one level of Rimefrost, and (0.74.1)
 * a moment's Whiteout.
 */
public final class SublimateRules {
    /** Its reach: mid-range, 8 blocks and one more a level. */
    public static final double BASE_RANGE = 8.0;
    public static final double RANGE_PER_LEVEL = 1.0;
    /** Half the cone's angle: about a few blocks wide at its far end. */
    public static final double HALF_ANGLE_DEGREES = 20.0;
    /** Its Rimefrost: I (the mild slow), 3 s and a second more a level. */
    public static final int RIMEFROST_AMPLIFIER = 0;
    public static final int BASE_RIMEFROST_TICKS = 60;
    public static final int RIMEFROST_TICKS_PER_LEVEL = 20;

    /** Its Whiteout (0.74.1, the owner: "a 1-2 sec whiteout effect too"): 1 s at level 1 to 2 s at level 5. */
    public static final int BASE_WHITEOUT_TICKS = 20;
    public static final int WHITEOUT_TICKS_PER_LEVEL = 5;

    private SublimateRules() {
    }

    public static double range(int level) {
        return BASE_RANGE + RANGE_PER_LEVEL * Math.max(0, level - 1);
    }

    public static int whiteoutTicks(int level) {
        return BASE_WHITEOUT_TICKS + WHITEOUT_TICKS_PER_LEVEL * Math.max(0, Math.min(4, level - 1));
    }

    public static int rimefrostTicks(int level) {
        return BASE_RIMEFROST_TICKS + RIMEFROST_TICKS_PER_LEVEL * Math.max(0, level - 1);
    }

    /**
     * Whether a point at offset ({@code dx}, {@code dy}, {@code dz}) from the cone's tip lies in it: within
     * {@code range} along any direction no more than {@link #HALF_ANGLE_DEGREES} from {@code look} (a unit vector),
     * padded by {@code pad} (the target's half-width), so a body at its edge counts.
     */
    public static boolean inCone(double dx, double dy, double dz, double lx, double ly, double lz, double range, double pad) {
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > range + pad) {
            return false;
        }
        if (distance <= pad) {
            return true;
        }
        double along = (dx * lx + dy * ly + dz * lz) / distance;
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, along))));
        double padAngle = Math.toDegrees(Math.atan2(pad, distance));
        return angle <= HALF_ANGLE_DEGREES + padAngle;
    }
}
