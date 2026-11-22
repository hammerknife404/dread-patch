package io.github.hammerknife404.dreadpatch.magic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Game-free: which pieces of the Dread Queen's regalia a kill gives its player (0.43.0, the owner: "each
 * kill guarantees a needed piece to complete the set", tracked per player by advancements, Looting
 * affecting it). With pieces still missing: one of them, and each Looting level a {@link #LOOTING_EXTRA}
 * chance of another. With the set complete: one at random, {@link #COMPLETE_CHANCE} and
 * {@link #LOOTING_COMPLETE} more a Looting level. Pieces are indices, helmet to boots.
 */
public final class RegaliaDrops {
    public static final int PIECES = 4;
    public static final double LOOTING_EXTRA = 0.1;
    public static final double COMPLETE_CHANCE = 0.25;
    public static final double LOOTING_COMPLETE = 0.05;

    private RegaliaDrops() {
    }

    public static List<Integer> pick(boolean[] owned, int looting, Random random) {
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < PIECES; i++) {
            if (!owned[i]) {
                missing.add(i);
            }
        }
        List<Integer> drops = new ArrayList<>();
        if (missing.isEmpty()) {
            if (random.nextDouble() < COMPLETE_CHANCE + LOOTING_COMPLETE * Math.max(0, looting)) {
                drops.add(random.nextInt(PIECES));
            }
            return drops;
        }
        drops.add(missing.remove(random.nextInt(missing.size())));
        if (!missing.isEmpty() && random.nextDouble() < LOOTING_EXTRA * Math.max(0, looting)) {
            drops.add(missing.remove(random.nextInt(missing.size())));
        }
        return drops;
    }
}
