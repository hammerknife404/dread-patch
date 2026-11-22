package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegaliaDropsTest {
    @Test
    void everyKillGivesAPieceStillMissing() {
        Random random = new Random(1);
        boolean[] owned = {true, false, true, false};
        for (int i = 0; i < 200; i++) {
            List<Integer> drops = RegaliaDrops.pick(owned, 0, random);
            assertEquals(1, drops.size());
            assertFalse(owned[drops.get(0)], "only a missing piece");
        }
    }

    @Test
    void fourKillsCompleteTheSet() {
        Random random = new Random(2);
        boolean[] owned = new boolean[4];
        for (int kill = 0; kill < 4; kill++) {
            for (int piece : RegaliaDrops.pick(owned, 0, random)) {
                owned[piece] = true;
            }
        }
        assertTrue(owned[0] && owned[1] && owned[2] && owned[3]);
    }

    @Test
    void lootingSometimesGivesASecondMissingPiece() {
        Random random = new Random(3);
        int twos = 0;
        for (int i = 0; i < 2000; i++) {
            List<Integer> drops = RegaliaDrops.pick(new boolean[4], 3, random);
            assertTrue(drops.size() <= 2);
            if (drops.size() == 2) {
                twos++;
                assertTrue(!drops.get(0).equals(drops.get(1)), "two different pieces");
            }
        }
        assertTrue(twos > 450 && twos < 750, "Looting III: about 30%, got " + twos);
        assertEquals(1, RegaliaDrops.pick(new boolean[4], 0, new Random(4)).size(), "no Looting, never two");
    }

    @Test
    void withTheSetCompleteAPieceOneTimeInFour() {
        Random random = new Random(5);
        boolean[] all = {true, true, true, true};
        int got = 0;
        for (int i = 0; i < 4000; i++) {
            got += RegaliaDrops.pick(all, 0, random).size();
        }
        assertTrue(got > 900 && got < 1100, "25%, got " + got);
    }
}
