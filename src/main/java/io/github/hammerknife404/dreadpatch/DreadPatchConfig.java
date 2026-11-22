package io.github.hammerknife404.dreadpatch;

import io.github.hammerknife404.dreadpatch.magic.CourtRules;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The server config ({@code config/dreadpatch-server.toml}): how a court raised by
 * Summon Court behaves (by default it fights only hostile mobs: never friendly mobs or players, not
 * even when they hit it); and how long until a Dreadlands spire's encounter comes again to a player
 * who's met it (0.47.0).
 */
public final class DreadPatchConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ATTACK_FRIENDLY_MOBS;
    private static final ModConfigSpec.BooleanValue RETALIATE;
    private static final ModConfigSpec.BooleanValue ATTACK_PLAYERS;
    private static final ModConfigSpec.DoubleValue ENCOUNTER_RESET_HOURS;
    /** The default: the owner's "after a lengthy time like several IRL hours"; ours, 6. */
    public static final double DEFAULT_ENCOUNTER_RESET_HOURS = 6.0;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("A court raised by the Summon Court spell: Ice and Fire's dread mobs fighting for whoever cast it.",
                "It fights hostile mobs that fight its caster (or that its caster fights) and never the Dread, nor its caster's side.").push("summoned_court");
        ATTACK_FRIENDLY_MOBS = builder.comment("Whether it joins its caster's fights with mobs that aren't hostile (animals, villagers, golems, other players' pets).",
                "Off, friendly mobs leave it be too: an iron golem won't go for it.").define("attack_friendly_mobs", false);
        RETALIATE = builder.comment("Whether it fights back against friendly mobs and players that hurt it.").define("retaliate", false);
        ATTACK_PLAYERS = builder.comment("Whether it joins its caster's fights with other players.").define("attack_players", false);
        builder.pop();
        builder.comment("The Dreadlands.").push("dreadlands");
        ENCOUNTER_RESET_HOURS = builder.comment("Real hours before a spire's Queen and Syndrigossa encounter comes again to a player who's met it",
                "(each player, each spire). 0: every time they climb back up, once she's gone.")
                .defineInRange("encounter_reset_hours", DEFAULT_ENCOUNTER_RESET_HOURS, 0.0, 8760.0);
        builder.pop();
        SPEC = builder.build();
    }

    private DreadPatchConfig() {
    }

    /** The summoned court's terms, or the defaults before a world's config has loaded. */
    public static CourtRules.Terms courtTerms() {
        if (!SPEC.isLoaded()) {
            return CourtRules.Terms.DEFAULT;
        }
        return new CourtRules.Terms(ATTACK_FRIENDLY_MOBS.get(), RETALIATE.get(), ATTACK_PLAYERS.get());
    }

    /** How long until a spire's encounter comes again to a player, in milliseconds. */
    public static long encounterResetMillis() {
        double hours = SPEC.isLoaded() ? ENCOUNTER_RESET_HOURS.get() : DEFAULT_ENCOUNTER_RESET_HOURS;
        return (long) (hours * 3_600_000L);
    }
}
