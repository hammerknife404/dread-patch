package io.github.hammerknife404.dreadpatch.magic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PermafrostConduitTest {
    @Test
    void takesAFifthOff() {
        assertEquals(40, PermafrostConduit.manaCost(50));   // her crown at level 1
        assertEquals(72, PermafrostConduit.manaCost(90));   // and at level 5
        assertEquals(0, PermafrostConduit.manaCost(0));
    }

    @Test
    void roundsToTheNearestPoint() {
        assertEquals(10, PermafrostConduit.manaCost(13));   // 10.4
        assertEquals(12, PermafrostConduit.manaCost(15));   // 12.0
        assertEquals(14, PermafrostConduit.manaCost(17));   // 13.6
    }
}
