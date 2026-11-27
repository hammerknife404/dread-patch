package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SwarmRulesTest {
    private static final double INF = Double.POSITIVE_INFINITY;
    private static final int PLAYER = Integer.MAX_VALUE;

    @Test
    void idleOnesNearAFightJoinIt() {
        // One thrall fights a player; two idle ones, 10 and 20 off, out of sight of the player, are called in.
        double[][] raw = {{2}, {10}, {20}};
        double[][] seen = {{2}, {INF}, {INF}};
        int[] chosen = SwarmRules.swarm(seen, raw, new int[]{0, -1, -1}, new int[]{PLAYER}, new boolean[]{false, true, true});
        assertArrayEquals(new int[]{0, 0, 0}, chosen);
    }

    @Test
    void oneTooFarIsLeftBe() {
        double[][] raw = {{2}, {SwarmRules.RALLY_RANGE + 1}};
        int[] chosen = SwarmRules.swarm(raw, raw, new int[]{0, -1}, new int[]{PLAYER}, new boolean[]{false, true});
        assertEquals(-1, chosen[1]);
    }

    @Test
    void theyCoverTheFieldNotOneChicken() {
        // Three on a chicken (4 health: room for one) with two cows (10 health each: one apiece) near: one stays, two move on.
        double[][] d = {{1, 3, 5}, {2, 4, 3}, {3, 6, 2}};
        int[] chosen = SwarmRules.swarm(d, d, new int[]{0, 0, 0}, new int[]{1, 1, 1}, new boolean[]{false, false, false});
        // The nearest keeps the chicken; the others take a cow each (greedily, in the order they were nearest their old target).
        assertEquals(0, chosen[0]);
        assertEquals(3, chosen[1] + chosen[2]);
        assertEquals(1, Math.abs(chosen[1] - chosen[2]));
    }

    @Test
    void ralliersTakeWhatHasRoomNearest() {
        // A fight over a cow (one place, taken) and a player: the idle one goes for the player, though the cow's nearer.
        double[][] d = {{1, 8}, {3, 9}};
        int[] chosen = SwarmRules.swarm(d, d, new int[]{0, -1}, new int[]{1, PLAYER}, new boolean[]{false, true});
        assertArrayEquals(new int[]{0, 1}, chosen);
    }

    @Test
    void noOneRalliesWithNoFight() {
        double[][] d = {{INF}, {INF}};
        int[] chosen = SwarmRules.swarm(d, d, new int[]{-1, -1}, new int[]{1}, new boolean[]{false, false});
        assertArrayEquals(new int[]{-1, -1}, chosen);
    }
}
