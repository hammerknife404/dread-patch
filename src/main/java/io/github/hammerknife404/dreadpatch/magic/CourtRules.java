package io.github.hammerknife404.dreadpatch.magic;

/**
 * Game-free rules for her Court of the Dread: which of Ice and Fire's dread mobs answer her call,
 * how often each, and how many rise at once. And for a court a caster raises with Summon Court: how
 * many, for how long, and what it may fight.
 */
public final class CourtRules {
    /** The dread she raises and their weights: mostly thralls, ghouls next, knights and scuttlers rarer. */
    public enum Kind {
        THRALL(3), GHOUL(2), KNIGHT(1), SCUTTLER(1);

        public final int weight;

        Kind(int weight) {
            this.weight = weight;
        }
    }

    public static final int TOTAL_WEIGHT;

    static {
        int total = 0;
        for (Kind kind : Kind.values()) {
            total += kind.weight;
        }
        TOTAL_WEIGHT = total;
    }

    private CourtRules() {
    }

    /** The kind a roll in {@code [0, TOTAL_WEIGHT)} picks. */
    public static Kind pick(int roll) {
        int r = Math.floorMod(roll, TOTAL_WEIGHT);
        for (Kind kind : Kind.values()) {
            if (r < kind.weight) {
                return kind;
            }
            r -= kind.weight;
        }
        throw new AssertionError("weights don't sum to TOTAL_WEIGHT");
    }

    /** How many to raise: a wave, but never past the court's limit. */
    public static int toRaise(int current, int max, int wave) {
        return Math.max(0, Math.min(wave, max - current));
    }

    /** Summon Court: 1 + level members (2 at level 1, 6 at level 5, her own court's limit). */
    public static int summonCount(int spellLevel) {
        return 1 + Math.max(1, spellLevel);
    }

    /** Summon Court: how long its court stands, (60 + 10 × level) s: 70 s at level 1, 110 s at 5. */
    public static int summonSeconds(int spellLevel) {
        return 60 + 10 * Math.max(1, spellLevel);
    }

    /** What a would-be target is to a court member its caster raised. */
    public enum Stance {
        /** Its caster, their allies (a team), their pets and summons, their court. */
        CASTERS_SIDE,
        /** One of the Dread (the queen, her court, any dread mob): they don't fight their own. */
        DREAD,
        /** A hostile mob, a monster. */
        HOSTILE,
        /** Another player. */
        PLAYER,
        /** Anything else: animals, villagers, golems, others' pets. */
        FRIENDLY
    }

    /** The server config's terms for a summoned court; by default it fights only hostile mobs. */
    public record Terms(boolean attackFriendlyMobs, boolean retaliate, boolean attackPlayers) {
        public static final Terms DEFAULT = new Terms(false, false, false);
    }

    /**
     * Whether a court member its caster raised may fight (target or hurt) something of this stance;
     * {@code retaliating} when that's what last hurt it. Hostile mobs, always; its caster's side and
     * the Dread, never; friendly mobs and players only as the config allows: joining its caster's
     * fights with them, or only fighting back.
     */
    public static boolean mayFight(Stance stance, boolean retaliating, Terms terms) {
        return switch (stance) {
            case CASTERS_SIDE, DREAD -> false;
            case HOSTILE -> true;
            case PLAYER -> terms.attackPlayers() || retaliating && terms.retaliate();
            case FRIENDLY -> terms.attackFriendlyMobs() || retaliating && terms.retaliate();
        };
    }
}
