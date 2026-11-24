package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free: the Rimefrost dragonforge's rules (0.59.0), Ice and Fire's dragonforge's
 * ({@code DragonForgeBlockEntity}, {@code IafDragonDestructionManager}) as its code actually runs.
 * <ul>
 * <li><b>Its shape</b> ({@link #assembled}): a 3×3×3 cube round the core. Above and below it, a layer of
 * bricks with a dragon bone block on each corner; round it, bricks on the four corners and on at least
 * three of the four sides (the aperture takes the fourth's place, or a brick does).</li>
 * <li><b>Its heat</b> ({@link #advance}, {@link #heat}): a breath hit lights it for {@link #LIT_TICKS}
 * and adds the dragon's forge speed to its progress (Ice and Fire's {@code iceandfire:dragon_forge_speed},
 * 0.025: it only starts the cook); while lit, the cook goes on a tick a tick; unlit, it holds where it
 * is. Ice and Fire's code has a branch that would cool an unlit forge 2 a tick, but it can never run (it
 * needs a forge that is both burning and not), so its forge holds too. With nothing it can make, the
 * cook is lost.</li>
 * <li><b>Where breath reaches it</b> ({@link #breathReach}): an aperture this far (a cube) from where the
 * breath lands, as Ice and Fire's breath reaches its own.</li>
 * </ul>
 */
public final class ForgeRules {
    /** How long a breath hit keeps the forge lit (Ice and Fire's {@code lastDragonFlameTimer}). */
    public static final int LIT_TICKS = 40;

    private ForgeRules() {
    }

    /** What's at a spot round the core. */
    public enum Part {
        BRICK, BONE, OTHER
    }

    /** What's at (dx, dy, dz) from the core. */
    @FunctionalInterface
    public interface Parts {
        Part at(int dx, int dy, int dz);
    }

    /** Whether the cube round the core is whole (Ice and Fire's {@code assembled}). */
    public static boolean assembled(Parts parts) {
        for (int dy : new int[]{-1, 1}) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Part want = dx != 0 && dz != 0 ? Part.BONE : Part.BRICK;
                    if (parts.at(dx, dy, dz) != want) {
                        return false;
                    }
                }
            }
        }
        for (int dx : new int[]{-1, 1}) {
            for (int dz : new int[]{-1, 1}) {
                if (parts.at(dx, 0, dz) != Part.BRICK) {
                    return false;
                }
            }
        }
        int sides = 0;
        for (int[] side : new int[][]{{0, -1}, {0, 1}, {-1, 0}, {1, 0}}) {
            if (parts.at(side[0], 0, side[1]) == Part.BRICK) {
                sides++;
            }
        }
        return sides >= 3;
    }

    /**
     * Its progress after a tick: lost if it can't make anything; nothing until a breath starts it; a
     * tick on while lit; held while not.
     */
    public static double advance(double cookTime, boolean lit, boolean canMake) {
        if (!canMake || cookTime <= 0) {
            return 0;
        }
        return lit ? cookTime + 1 : cookTime;
    }

    /** Its progress after a breath hit of {@code power} (the dragon's forge speed): never more than a tick past done. */
    public static double heat(double cookTime, double power, double cookFor, boolean canMake) {
        return canMake ? Math.min(cookFor + 1, cookTime + power) : 0;
    }

    /** Whether the cook is done. */
    public static boolean done(double cookTime, double cookFor) {
        return cookTime >= cookFor;
    }

    /** How far (in blocks, a cube) from where breath lands an aperture is reached: 1 to stage 3, 2 at stage 4, 3 at stage 5. */
    public static int breathReach(int stage) {
        return stage <= 3 ? 1 : stage == 4 ? 2 : 3;
    }

    /** Whether a dragon that far (squared) from the aperture breathes at it: Ice and Fire's 115 a stage (squared distance). */
    public static boolean withinBreath(double distanceSqr, int stage) {
        return distanceSqr < 115.0 * stage;
    }

    /** Whether an aperture lures a dragon whose sight line to it stops {@code clear} blocks out: Ice and Fire's 10 + twice its width. */
    public static boolean seesAperture(double clear, double width) {
        return clear < 10.0 + width * 2.0;
    }
}
