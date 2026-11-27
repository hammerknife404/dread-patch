package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: a dread thrall's weapon (0.84.4, the owner: "make sure dread thralls can spawn with a bow or cross bow, and
 * never without a weapon"). Ice and Fire arms three thralls in four with a sword (mostly its dread sword) and leaves the
 * rest empty-handed; an empty-handed one is given one of these ({@link #weapon}), and one holding a bow or crossbow
 * shoots (with {@link #DRAW_TICKS} or {@link #CHARGE_TICKS} to ready it) from up to {@link #RANGE} blocks.
 */
public final class ThrallArms {
    public enum Weapon { BOW, CROSSBOW, DREAD_SWORD }

    /** Weights for an empty-handed thrall's bow, crossbow, dread sword. */
    static final int[] WEIGHTS = {40, 20, 40};
    /** How far it shoots from; it closes in to {@link #CLOSE_TO} when it can't see its target or is further. */
    public static final double RANGE = 16.0;
    public static final double CLOSE_TO = 11.0;
    /** A bow's draw, as a skeleton's full draw (20 ticks); a crossbow's charge, as vanilla's (25). */
    public static final int DRAW_TICKS = 20;
    public static final int CHARGE_TICKS = 25;
    /** Ticks from a shot to the next draw. */
    public static final int BOW_COOLDOWN = 20;
    public static final int CROSSBOW_COOLDOWN = 30;
    /** An arrow's speed: a skeleton's from a bow; a crossbow's harder (more damage, as a crossbow bolt is). */
    public static final float BOW_VELOCITY = 1.6F;
    public static final float CROSSBOW_VELOCITY = 2.4F;

    private ThrallArms() {
    }

    /** An empty-handed thrall's weapon, from a roll in [0, 1). */
    public static Weapon weapon(double roll) {
        int sum = 0;
        for (int w : WEIGHTS) {
            sum += w;
        }
        double at = roll * sum;
        for (int i = 0; i < WEIGHTS.length; i++) {
            at -= WEIGHTS[i];
            if (at < 0) {
                return Weapon.values()[i];
            }
        }
        return Weapon.values()[WEIGHTS.length - 1];
    }

    /** How long a weapon takes to ready. */
    public static int readyTicks(boolean crossbow) {
        return crossbow ? CHARGE_TICKS : DRAW_TICKS;
    }

    /** Its arrow's spread, as a skeleton's: 10 on easy, 6 on normal, 2 on hard. */
    public static float inaccuracy(int difficultyId) {
        return 14 - difficultyId * 4;
    }
}
