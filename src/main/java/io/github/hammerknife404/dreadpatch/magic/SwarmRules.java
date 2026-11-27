package io.github.hammerknife404.dreadpatch.magic;

import java.util.Arrays;
import java.util.Comparator;

/**
 * The wild Dread swarming a fight (0.94.0, the owner: "The dread mob ai should generally swarm the battle field"). Ours,
 * after her court's spreading (0.91.0, {@link CourtRules#spread}), for the Dread that are no one's:
 * <ul>
 * <li><b>Rallying:</b> one hunting nothing within {@link #RALLY_RANGE} of one that's fighting joins in: the nearest of the
 * fight's targets with room, within that range, whether it sees it or not (the fight calls it).</li>
 * <li><b>Spreading:</b> those fighting share out what's near, the fight's targets and anything else they'd hunt within
 * {@link CourtRules#SPREAD_RANGE} of one of them, by her court's caps ({@link CourtRules#pursuerCap}: one per 10 max
 * health, any number on a player), so they cover the field rather than all chasing one mob.</li>
 * </ul>
 */
public final class SwarmRules {
    /** How far from one fighting an idle one is called in. */
    public static final double RALLY_RANGE = 24.0;
    /** How often (ticks). */
    public static final int EVERY = 20;

    private SwarmRules() {
    }

    /**
     * Each member's target (an index, or −1). {@code seen[m][t]}: member {@code m}'s distance to target {@code t}, or
     * {@link Double#POSITIVE_INFINITY} where it can't see it (its own target it always may); {@code raw[m][t]} the distance
     * regardless; {@code current[m]} its target, or −1; {@code caps[t]} by {@link CourtRules#pursuerCap};
     * {@code rallying[m]} whether it's idle and near enough a fight to be called.
     */
    public static int[] swarm(double[][] seen, double[][] raw, int[] current, int[] caps, boolean[] rallying) {
        int[] chosen = CourtRules.spread(seen, current, caps, -1);
        int[] load = new int[caps.length];
        for (int t : chosen) {
            if (t >= 0) {
                load[t]++;
            }
        }
        Integer[] order = new Integer[current.length];
        for (int m = 0; m < order.length; m++) {
            order[m] = m;
        }
        // The nearest to a target first, so the near ones join the near fight.
        Arrays.sort(order, Comparator.comparingDouble(m -> nearest(raw[m])));
        for (int m : order) {
            if (!rallying[m] || chosen[m] >= 0) {
                continue;
            }
            int best = -1;
            for (int t = 0; t < caps.length; t++) {
                if (load[t] < caps[t] && raw[m][t] <= RALLY_RANGE && (best < 0 || raw[m][t] < raw[m][best])) {
                    best = t;
                }
            }
            if (best >= 0) {
                chosen[m] = best;
                load[best]++;
            }
        }
        return chosen;
    }

    private static double nearest(double[] distances) {
        double best = Double.MAX_VALUE;
        for (double d : distances) {
            best = Math.min(best, d);
        }
        return best;
    }
}
