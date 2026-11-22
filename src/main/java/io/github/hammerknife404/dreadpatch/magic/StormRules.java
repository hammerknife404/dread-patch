package io.github.hammerknife404.dreadpatch.magic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Game-free rules for the Dreadlands' storms (0.48.0). The owner: "Weather follows the overworld's:
 * in a storm, heavy fog (less than Whiteout Dominion's, 'a warning'), no damage, and Rimefrost;
 * lightning ignites a rimefire patch". Ours: any snowfall there is a storm; lightning only in thunder.
 */
public final class StormRules {
    /** How far the fog lets you see at the storm's height (Whiteout Dominion's: 10). */
    public static final float FOG_FAR = 24.0F;
    /** In plain snow, no thunder: lighter (0.55.1, the owner: the fog was "just as thick during normal rain"). Eased toward {@link #FOG_FAR} as the thunder rises. */
    public static final float FOG_FAR_SNOW = 56.0F;
    /** Rimefrost on everyone under the open sky, renewed every second while it lasts. */
    public static final int RIMEFROST_TICKS = 60;
    /**
     * Lightning: vanilla's never strikes ground that cold (only where it rains), so ours: in thunder,
     * each player has a 1-in-400 chance a tick (one strike in about 20 s) of one within {@link #STRIKE_RANGE}.
     */
    public static final int STRIKE_ONE_IN = 400;
    public static final int STRIKE_RANGE = 48;
    /** Its rimefire patch: most of the ground within this of the strike. */
    public static final double PATCH_RADIUS = 2.5;
    public static final double PATCH_FILL = 0.7;

    private StormRules() {
    }

    /** The offsets round a strike its rimefire takes: the strike's own block always, most of the rest of the disc. */
    public static List<int[]> patch(Random random) {
        List<int[]> cells = new ArrayList<>();
        int reach = (int) Math.ceil(PATCH_RADIUS);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx * dx + dz * dz > PATCH_RADIUS * PATCH_RADIUS) {
                    continue;
                }
                if (dx == 0 && dz == 0 || random.nextDouble() < PATCH_FILL) {
                    cells.add(new int[]{dx, dz});
                }
            }
        }
        return cells;
    }
}
