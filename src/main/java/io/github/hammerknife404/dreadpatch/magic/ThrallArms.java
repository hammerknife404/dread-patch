package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: a dread thrall's weapon (0.84.4, the owner: "make sure dread thralls can spawn with a bow or cross bow, and
 * never without a weapon"). Ice and Fire arms three thralls in four with a sword (mostly its dread sword) and leaves the
 * rest empty-handed; an empty-handed one is given one of these ({@link #weapon}), and one holding a bow or crossbow
 * shoots (with {@link #DRAW_TICKS} or {@link #CHARGE_TICKS} to ready it) from up to {@link #RANGE} blocks.
 * <p>Now and then (0.85.0, the owner: "Add low chance to spawn unarmed; use Ghoul attacks at 50% damage with independent
 * 30% chances to apply non-stacking Rimefrost and healing Withering Rime") it stays unarmed ({@link Weapon#NONE}, one in
 * twenty of all thralls): it claws as a ghoul does, twice an attack ({@link #SECOND_SLASH_TICKS} apart) at half a ghoul's
 * damage ({@link #clawScale}), each slash rolling {@link #CLAW_EFFECT_CHANCE} for Rimefrost and for Withering Rime (which,
 * as all the Dread's, heals it: {@code effect/RimeSiphon}).
 */
public final class ThrallArms {
    public enum Weapon { NONE, BOW, CROSSBOW, DREAD_SWORD }

    /** Weights for an empty-handed thrall's none, bow, crossbow, dread sword (until 0.85.0 never none: 40, 20, 40). */
    static final int[] WEIGHTS = {20, 32, 16, 32};
    /** Ice and Fire's ghoul's attack damage, and its thrall's: a claw is half the ghoul's. */
    public static final double GHOUL_DAMAGE = 5.0;
    public static final double THRALL_DAMAGE = 2.0;
    /** A ghoul's slash lands twice: its animation's ticks 9 and 19. */
    public static final int SECOND_SLASH_TICKS = 10;
    public static final float CLAW_EFFECT_CHANCE = 0.3F;
    /** How far it shoots from; it closes in to {@link #CLOSE_TO} when it can't see its target or is further. */
    public static final double RANGE = 16.0;
    public static final double CLOSE_TO = 11.0;
    /** A bow's draw, as a skeleton's full draw (20 ticks); a crossbow's charge, as vanilla's (25). */
    public static final int DRAW_TICKS = 20;
    public static final int CHARGE_TICKS = 25;
    /** Ticks from a shot to the next draw. */
    public static final int BOW_COOLDOWN = 20;
    /**
     * One shot in five a tipped arrow (0.95.0, the owner: "arrow variants, and the thralls with bows or crossbows have a
     * chance of firing either type"): Rimefrost or Withering Rime, even odds, of the plain potions (an eighth of their time
     * on a hit, as any tipped arrow: 11 s and 3.75 s).
     */
    public static final float TIPPED_CHANCE = 0.2F;
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

    /** What an unarmed thrall's hit is multiplied by to be a claw: half a ghoul's damage over a thrall's own. */
    public static double clawScale() {
        return GHOUL_DAMAGE * 0.5 / THRALL_DAMAGE;
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
