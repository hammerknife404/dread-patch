package io.github.hammerknife404.dreadpatch;

import io.github.hammerknife404.dreadpatch.magic.CourtRules;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The server config ({@code config/dreadpatch-server.toml}): how a court raised by
 * Summon Court behaves. By default it fights only hostile mobs: never friendly mobs or players, not
 * even when they hit it.
 */
public final class DreadPatchConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ATTACK_FRIENDLY_MOBS;
    private static final ModConfigSpec.BooleanValue RETALIATE;
    private static final ModConfigSpec.BooleanValue ATTACK_PLAYERS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("A court raised by the Summon Court spell: Ice and Fire's dread mobs fighting for whoever cast it.",
                "It fights hostile mobs that fight its caster (or that its caster fights) and never the Dread, nor its caster's side.").push("summoned_court");
        ATTACK_FRIENDLY_MOBS = builder.comment("Whether it joins its caster's fights with mobs that aren't hostile (animals, villagers, golems, other players' pets).",
                "Off, friendly mobs leave it be too: an iron golem won't go for it.").define("attack_friendly_mobs", false);
        RETALIATE = builder.comment("Whether it fights back against friendly mobs and players that hurt it.").define("retaliate", false);
        ATTACK_PLAYERS = builder.comment("Whether it joins its caster's fights with other players.").define("attack_players", false);
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
}
