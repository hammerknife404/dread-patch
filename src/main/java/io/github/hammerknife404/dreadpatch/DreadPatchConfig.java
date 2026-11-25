package io.github.hammerknife404.dreadpatch;

import io.github.hammerknife404.dreadpatch.magic.ColdRules;
import io.github.hammerknife404.dreadpatch.magic.CourtRules;
import io.github.hammerknife404.dreadpatch.magic.DreadlandsSpawning;
import io.github.hammerknife404.dreadpatch.magic.EscalationRules;
import io.github.hammerknife404.dreadpatch.magic.RimeEelRules;
import io.github.hammerknife404.dreadpatch.magic.RisenRules;
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
    private static final ModConfigSpec.IntValue UNDEAD;
    private static final ModConfigSpec.IntValue UNDEAD_CAP;
    private static final ModConfigSpec.IntValue VILLAGE_LICHES;
    private static final ModConfigSpec.IntValue MAUSOLEUM_DREAD;
    private static final ModConfigSpec.IntValue CAVE_PHANTOMS;
    private static final ModConfigSpec.IntValue CAVE_WATER;
    private static final ModConfigSpec.IntValue CHASMA_WATER;
    private static final ModConfigSpec.IntValue DARK_WATER_EELS;
    private static final ModConfigSpec.DoubleValue DEFEAT_STORM_HOURS;
    private static final ModConfigSpec.BooleanValue DEFEAT_STORM_COLD_ONLY;
    private static final ModConfigSpec.IntValue PHANTOMS;
    private static final ModConfigSpec.IntValue PHANTOMS_NEAR_SPIRES;
    private static final ModConfigSpec.IntValue WHITEOUT_STORMS;
    private static final ModConfigSpec.IntValue FLAME_FADE_MIN;
    private static final ModConfigSpec.IntValue FLAME_FADE_MAX;
    private static final ModConfigSpec.DoubleValue DREAD_LIGHT_FREEZE;
    private static final ModConfigSpec.BooleanValue CROWN_STORM_SIGHT;
    private static final ModConfigSpec.DoubleValue CROWN_WHITEOUT_SIGHT;
    private static final ModConfigSpec.BooleanValue REGALIA_EASES_WEATHER;
    private static final ModConfigSpec.IntValue SIPHON_TARGETS;
    private static final ModConfigSpec.BooleanValue SIPHON_SPLIT;
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
        UNDEAD = builder.comment("Vanilla's undead (strays, zombies, skeletons, zombie villagers) strayed into the Dreadlands: each player has a",
                "one-in-this chance every 10 s of 1-2 rising 24-48 blocks off, day or night (the sun there doesn't burn them). 0: never.")
                .defineInRange("undead_spawns", DreadlandsSpawning.UNDEAD_ONE_IN, 0, 10000);
        UNDEAD_CAP = builder.comment("No more of them rise while this many of vanilla's undead are within 64 blocks of the player.")
                .defineInRange("undead_cap", DreadlandsSpawning.UNDEAD_CAP, 0, 100);
        VILLAGE_LICHES = builder.comment("One abandoned village in this many has a Dread Lich at its heart, from when it's made (villages already made",
                "keep what they had). 0: none.").defineInRange("village_liches", DreadlandsSpawning.VILLAGE_LICH_ONE_IN, 0, 10000);
        PHANTOMS = builder.comment("Dread phantoms coming down on a player under the open sky: a one-in-this chance every 30 s. 0: never.")
                .defineInRange("phantoms", DreadlandsSpawning.PHANTOM_ONE_IN, 0, 10000);
        PHANTOMS_NEAR_SPIRES = builder.comment("The same within 96 blocks of a spire.")
                .defineInRange("phantoms_near_spires", DreadlandsSpawning.PHANTOM_NEAR_SPIRE_ONE_IN, 0, 10000);
        WHITEOUT_STORMS = builder.comment("About one storm in this many in the Dreadlands is a Whiteout Dominion: fog to 10 blocks, slowed,",
                "and a point of freezing damage every 3 s under the open sky. Never two in a row (so 2 or less: every other storm),",
                "never starting while a Dread Queen is about there, and it breaks into a thunderstorm when her fight starts. 0: never.")
                .defineInRange("whiteout_storms", WeatherRules.WHITEOUT_ONE_IN, 0, 10000);
        FLAME_FADE_MIN = builder.comment("A lantern or torch placed in the Dreadlands steps toward the Dread's (soul, then dread) after this many seconds at least...")
                .defineInRange("flame_fade_min_seconds", 30, 1, 3600);
        FLAME_FADE_MAX = builder.comment("...and at most (if less than the least, the least).").defineInRange("flame_fade_max_seconds", 45, 1, 3600);
        DREAD_LIGHT_FREEZE = builder.comment("Dread torches and lanterns are sources of cold, anywhere: water within 2 blocks across (2 below, 1 above) turns",
                "to ice, packed ice, then blue ice, a third of this many minutes of game time each (sleeping doesn't skip it). 20: a day. 0: never.")
                .defineInRange("dread_light_freeze_minutes", ColdRules.DEFAULT_MINUTES, 0.0, 100000.0);
        builder.pop();
        builder.comment("What her defeat stirs in the overworld.").push("overworld");
        MAUSOLEUM_DREAD = builder.comment("Once a Dread Queen has been defeated in this world, the Dread rise round its Mausoleums for good: each",
                "player within 128 blocks of one, in a cold biome, has a one-in-this chance every 10 s of 1-3 rising 24-48 blocks off on",
                "snow, ice or dreadstone (never in a village), and patrols cross there as in the Dreadlands. 0: never, nor the patrols.")
                .defineInRange("mausoleum_dread", EscalationRules.MAUSOLEUM_ONE_IN, 0, 10000);
        DEFEAT_STORM_HOURS = builder.comment("Her defeat brings a dry storm over the cold lands for 30-60 s, then none for this many real hours,",
                "whoever defeats her again. 0: one every defeat.")
                .defineInRange("defeat_storm_hours", EscalationRules.DEFAULT_STORM_HOURS, 0.0, 8760.0);
        DEFEAT_STORM_COLD_ONLY = builder.comment("Whether it reaches only players in cold and snowy biomes (off: everyone in the overworld under the sky).")
                .define("defeat_storm_cold_only", true);
        builder.pop();
        builder.comment("Risen Dread: bats, parrots and water creatures the Dread raise, and those stirring on their own; and Rime Eels.").push("risen");
        CAVE_PHANTOMS = builder.comment("Risen Dread stirring on their own, in the overworld and the Dreadlands: each player has a one-in-this chance",
                "every 10 s of a miniature dread phantom in dark cave air near them. 0: never.")
                .defineInRange("cave_dread_phantoms", RisenRules.CAVE_PHANTOM_ONE_IN, 0, 10000);
        CAVE_WATER = builder.comment("The same for a Dread water creature (glow squid mostly, squid, cod, salmon) in underground water near them;",
                "in the Dreadlands, a Rime Eel in dark underground water. 0: never.")
                .defineInRange("cave_dread_water", RisenRules.CAVE_WATER_ONE_IN, 0, 10000);
        CHASMA_WATER = builder.comment("In the Dreadlands' Rime Chasma, each player's one-in-this chance every 10 s of a Rime Eel in one of",
                "the pockets of water sealed in its ice near them (on top of the above). 0: never.")
                .defineInRange("chasma_dread_water", RisenRules.CHASMA_WATER_ONE_IN, 0, 10000);
        DARK_WATER_EELS = builder.comment("In the Dreadlands, each player's one-in-this chance every 10 s of a Rime Eel in any dark water near them",
                "(two deep, light 6 or less), on top of the above; never past 4 eels near. 0: never.")
                .defineInRange("dreadlands_rime_eels", RimeEelRules.DARK_WATER_ONE_IN, 0, 10000);
        builder.pop();
        builder.comment("The Dread Queen's siphon: how her Withering Rime heals her when it's on several at once.").push("siphon");
        SIPHON_TARGETS = builder.comment("How many of those she's withering feed her at once, the nearest first. 0: every one.")
                .defineInRange("siphon_targets", 0, 0, 64);
        SIPHON_SPLIT = builder.comment("Whether her heal is shared among those feeding her, so all of them heal her what one would.")
                .define("siphon_split", false);
        builder.pop();
        builder.comment("The Dread Queen's regalia.").push("regalia");
        CROWN_STORM_SIGHT = builder.comment("Whether her crown keeps the Dreadlands' storm fog (snow and heavy snowstorms) off its wearer: near clear sight.")
                .define("crown_storm_sight", true);
        CROWN_WHITEOUT_SIGHT = builder.comment("How much further her crown lets its wearer see in any whiteout (the weather, or a Whiteout Dominion's):",
                "0.75 is 75% further. 0: no further.").defineInRange("crown_whiteout_sight", 0.75, 0.0, 10.0);
        REGALIA_EASES_WEATHER = builder.comment("Whether her full regalia eases the Dreadlands' weather slow a step: a heavy snowstorm slows as plain snow,",
                "and plain snow not at all.").define("regalia_eases_weather", true);
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

    public static int undeadOneIn() {
        return SPEC.isLoaded() ? UNDEAD.get() : DreadlandsSpawning.UNDEAD_ONE_IN;
    }

    public static int undeadCap() {
        return SPEC.isLoaded() ? UNDEAD_CAP.get() : DreadlandsSpawning.UNDEAD_CAP;
    }

    public static int villageLichOneIn() {
        return SPEC.isLoaded() ? VILLAGE_LICHES.get() : DreadlandsSpawning.VILLAGE_LICH_ONE_IN;
    }

    public static int mausoleumDreadOneIn() {
        return SPEC.isLoaded() ? MAUSOLEUM_DREAD.get() : EscalationRules.MAUSOLEUM_ONE_IN;
    }

    public static long defeatStormCooldownMillis() {
        return (long) ((SPEC.isLoaded() ? DEFEAT_STORM_HOURS.get() : EscalationRules.DEFAULT_STORM_HOURS) * 3_600_000.0);
    }

    public static boolean defeatStormColdOnly() {
        return !SPEC.isLoaded() || DEFEAT_STORM_COLD_ONLY.get();
    }

    public static int cavePhantomOneIn() {
        return SPEC.isLoaded() ? CAVE_PHANTOMS.get() : RisenRules.CAVE_PHANTOM_ONE_IN;
    }

    public static int caveWaterOneIn() {
        return SPEC.isLoaded() ? CAVE_WATER.get() : RisenRules.CAVE_WATER_ONE_IN;
    }

    public static int chasmaWaterOneIn() {
        return SPEC.isLoaded() ? CHASMA_WATER.get() : RisenRules.CHASMA_WATER_ONE_IN;
    }

    public static int darkWaterEelOneIn() {
        return SPEC.isLoaded() ? DARK_WATER_EELS.get() : RimeEelRules.DARK_WATER_ONE_IN;
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

    /** Ticks a fading lantern or torch waits at least, and at most, for its next step (0.58.0). */
    public static double dreadLightFreezeMinutes() {
        return SPEC.isLoaded() ? DREAD_LIGHT_FREEZE.get() : ColdRules.DEFAULT_MINUTES;
    }

    public static int flameFadeMinTicks() {
        return (SPEC.isLoaded() ? FLAME_FADE_MIN.get() : 30) * 20;
    }

    public static int flameFadeMaxTicks() {
        return Math.max(flameFadeMinTicks(), (SPEC.isLoaded() ? FLAME_FADE_MAX.get() : 45) * 20);
    }

    public static boolean crownStormSight() {
        return !SPEC.isLoaded() || CROWN_STORM_SIGHT.get();
    }

    public static double crownWhiteoutSight() {
        return SPEC.isLoaded() ? CROWN_WHITEOUT_SIGHT.get() : 0.75;
    }

    public static int siphonTargets() {
        return SPEC.isLoaded() ? SIPHON_TARGETS.get() : 0;
    }

    public static boolean siphonSplit() {
        return SPEC.isLoaded() && SIPHON_SPLIT.get();
    }

    public static boolean regaliaEasesWeather() {
        return !SPEC.isLoaded() || REGALIA_EASES_WEATHER.get();
    }

    /** Every setting by its name (the last part of its path; they're unique), in the file's order (0.58.0, for {@code /dreadpatch config}). */
    public static java.util.Map<String, ModConfigSpec.ConfigValue<?>> values() {
        java.util.Map<String, ModConfigSpec.ConfigValue<?>> out = new java.util.LinkedHashMap<>();
        collect(SPEC.getValues(), out);
        return out;
    }

    private static void collect(com.electronwill.nightconfig.core.UnmodifiableConfig config, java.util.Map<String, ModConfigSpec.ConfigValue<?>> out) {
        for (com.electronwill.nightconfig.core.UnmodifiableConfig.Entry entry : config.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof ModConfigSpec.ConfigValue<?> setting) {
                out.put(entry.getKey(), setting);
            } else if (value instanceof com.electronwill.nightconfig.core.UnmodifiableConfig section) {
                collect(section, out);
            }
        }
    }

    /**
     * Sets {@code setting} from text, if it parses as its kind and is in its range, and saves the file (NeoForge
     * reloads it, so it applies at once). Whether it took.
     */
    @SuppressWarnings("unchecked")
    public static boolean set(ModConfigSpec.ConfigValue<?> setting, String text) {
        Object current = setting.get();
        Object parsed;
        try {
            if (current instanceof Boolean) {
                if (!text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false")) {
                    return false;
                }
                parsed = Boolean.parseBoolean(text);
            } else if (current instanceof Integer) {
                parsed = Integer.parseInt(text);
            } else if (current instanceof Double) {
                parsed = Double.parseDouble(text);
            } else {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        ModConfigSpec.ValueSpec spec = SPEC.getSpec().get(setting.getPath());
        if (spec == null || !spec.test(parsed)) {
            return false;
        }
        ((ModConfigSpec.ConfigValue<Object>) setting).set(parsed);
        setting.save();
        return true;
    }

    /** How long until a spire's encounter comes again to a player, in milliseconds. */
    public static long encounterResetMillis() {
        double hours = SPEC.isLoaded() ? ENCOUNTER_RESET_HOURS.get() : DEFAULT_ENCOUNTER_RESET_HOURS;
        return (long) (hours * 3_600_000L);
    }
}
