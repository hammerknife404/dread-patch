package io.github.hammerknife404.dreadpatch.magic;

import org.jetbrains.annotations.Nullable;

/**
 * Game-free: when a spire's encounter comes to a player again (0.47.0). The owner: "per player, once,
 * but resetting after a lengthy time like several IRL hours" (ours: the server config, 6 h by default).
 */
public final class EncounterRules {
    private EncounterRules() {
    }

    /** Whether {@code last} (epoch ms, or never) is long enough before {@code now} for the encounter to come again. */
    public static boolean mayMeet(@Nullable Long last, long now, long resetMillis) {
        return last == null || now - last >= resetMillis;
    }
}
