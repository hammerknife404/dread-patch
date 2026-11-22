package io.github.hammerknife404.dreadpatch;

import io.github.hammerknife404.dreadpatch.magic.CourtRules;
import io.github.hammerknife404.dreadpatch.magic.DreadlandsSpawning;
import io.github.hammerknife404.dreadpatch.magic.WeatherRules;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The server config ({@code config/dreadpatch-server.toml}): how a court raised by
 * Summon Court behaves (by default it fights only hostile mobs: never friendly mobs or players, not
 * even when they hit it); and how long until a Dreadlands spire's encounter comes again to a player
 * who's met it (0.47.0); how often the Dread rise there (0.52.0); how often its storms are whiteouts (0.54.0).
 */
public final class DreadPatchConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ATTACK_FRIENDLY_MOBS;
    private static final ModConfigSpec.BooleanValue RETALIATE;
    private static final ModConfigSpec.BooleanValue ATTACK_PLAYERS;
    private static final ModConfigSpec.DoubleValue ENCOUNTER_RESET_HOURS;
    private static final ModConfigSpec.IntValue DREAD_CLEAR;
    private static final ModConfigSpec.IntValue DREAD_SNOW;
    private static final ModConfigSpec.IntValue DREAD_STORM;
    private static final ModConfigSpec.IntValue DREAD_CAP;
    private static final ModConfigSpec.IntValue PATROLS;
    private static final ModConfigSpec.IntValue PHANTOMS;
    private static final ModConfigSpec.IntValue PHANTOMS_NEAR_SPIRES;
    private static final ModConfigSpec.IntValue WHITEOUT_STORMS;
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
        DREAD_CLEAR = builder.comment("Dread mobs rising in the Dreadlands in clear weather: each player has a one-in-this chance every 10 s",
                "of 1-3 rising 24-48 blocks off, day or night. 0: never.").defineInRange("dread_spawns_clear", DreadlandsSpawning.CLEAR_ONE_IN, 0, 10000);
        DREAD_SNOW = builder.comment("The same while it snows there.").defineInRange("dread_spawns_snow", DreadlandsSpawning.SNOW_ONE_IN, 0, 10000);
        DREAD_STORM = builder.comment("The same in a heavy snowstorm (thunder).").defineInRange("dread_spawns_storm", DreadlandsSpawning.STORM_ONE_IN, 0, 10000);
        DREAD_CAP = builder.comment("No more rise while this many of the Dread are within 64 blocks of the player.")
                .defineInRange("dread_cap", DreadlandsSpawning.CAP, 0, 100);
        PATROLS = builder.comment("Dread patrols (a dread knight leading 2-4 more): each player has a one-in-this chance every 2 minutes. 0: never.")
                .defineInRange("patrols", DreadlandsSpawning.PATROL_ONE_IN, 0, 10000);
        PHANTOMS = builder.comment("Dread phantoms coming down on a player under the open sky: a one-in-this chance every 30 s. 0: never.")
                .defineInRange("phantoms", DreadlandsSpawning.PHANTOM_ONE_IN, 0, 10000);
        PHANTOMS_NEAR_SPIRES = builder.comment("The same within 96 blocks of a spire.")
                .defineInRange("phantoms_near_spires", DreadlandsSpawning.PHANTOM_NEAR_SPIRE_ONE_IN, 0, 10000);
        WHITEOUT_STORMS = builder.comment("About one storm in this many in the Dreadlands is a Whiteout Dominion: fog to 10 blocks, slowed,",
                "and a point of freezing damage every 3 s under the open sky. Never two in a row (so 2 or less: every other storm),",
                "never starting while a Dread Queen is about there, and it breaks into a thunderstorm when her fight starts. 0: never.")
                .defineInRange("whiteout_storms", WeatherRules.WHITEOUT_ONE_IN, 0, 10000);
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

    /** One in how many for a rising of the Dread, by the weather (0.52.0). */
    public static int dreadSpawnOneIn(DreadlandsSpawning.Weather weather) {
        if (!SPEC.isLoaded()) {
            return DreadlandsSpawning.oneIn(weather, DreadlandsSpawning.CLEAR_ONE_IN, DreadlandsSpawning.SNOW_ONE_IN, DreadlandsSpawning.STORM_ONE_IN);
        }
        return DreadlandsSpawning.oneIn(weather, DREAD_CLEAR.get(), DREAD_SNOW.get(), DREAD_STORM.get());
    }

    public static int dreadCap() {
        return SPEC.isLoaded() ? DREAD_CAP.get() : DreadlandsSpawning.CAP;
    }

    public static int patrolOneIn() {
        return SPEC.isLoaded() ? PATROLS.get() : DreadlandsSpawning.PATROL_ONE_IN;
    }

    public static int phantomOneIn(boolean nearSpire) {
        if (!SPEC.isLoaded()) {
            return nearSpire ? DreadlandsSpawning.PHANTOM_NEAR_SPIRE_ONE_IN : DreadlandsSpawning.PHANTOM_ONE_IN;
        }
        return nearSpire ? PHANTOMS_NEAR_SPIRES.get() : PHANTOMS.get();
    }

    /** About one Dreadlands storm in how many is a whiteout (0.54.0). */
    public static int whiteoutOneIn() {
        return SPEC.isLoaded() ? WHITEOUT_STORMS.get() : WeatherRules.WHITEOUT_ONE_IN;
    }

    /** How long until a spire's encounter comes again to a player, in milliseconds. */
    public static long encounterResetMillis() {
        double hours = SPEC.isLoaded() ? ENCOUNTER_RESET_HOURS.get() : DEFAULT_ENCOUNTER_RESET_HOURS;
        return (long) (hours * 3_600_000L);
    }
}
