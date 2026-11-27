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

    /**
     * Her court's limit by difficulty (0.79.0, asked whether her storm's risen should keep counting against her court's 6:
     * "scale it to difficulty?"; ours: they still count, and the limit is 4 on Easy (and Peaceful), 6 on Normal as before,
     * 8 on Hard). {@code difficulty} is the difficulty's id, 0 to 3.
     */
    public static int courtLimit(int difficulty) {
        return difficulty >= 3 ? 8 : difficulty == 2 ? 6 : 4;
    }

    /** How many to raise: a wave, but never past the court's limit. */
    public static int toRaise(int current, int max, int wave) {
        return Math.max(0, Math.min(wave, max - current));
    }

    /**
     * Her court spreads out over what's near (0.91.0, the owner: "target other nearby mobs instead of all on the same ... not
     * have 2 dread knights, 3 ghouls and 6 dread beasts ... all chasing the same chicken when a dozen other slower things are
     * nearby too"; ours): at most this many of them on one mob, one per {@link #HEALTH_PER_PURSUER} of its max health, at
     * least one. A player has no limit: they're her fight.
     */
    public static final float HEALTH_PER_PURSUER = 10.0F;
    /** How far from a court member a fresh target can be, when it's sent off one that's crowded. */
    public static final double SPREAD_RANGE = 16.0;

    /** How many of her court may go for one target: {@code maxHealth / HEALTH_PER_PURSUER} rounded up (at least 1), any for a player. */
    public static int pursuerCap(float maxHealth, boolean player) {
        if (player) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) Math.ceil(maxHealth / HEALTH_PER_PURSUER));
    }

    /**
     * Shares targets out among her court. {@code distance[m][t]} is member {@code m}'s distance to target {@code t}
     * ({@link Double#POSITIVE_INFINITY} where it can't take it: out of sight, say); {@code current[m]} the target it has (an
     * index, or −1); {@code caps[t]} {@link #pursuerCap}; {@code preferred} her own target's index, or −1. Returns each member's
     * target, or −1 for none.
     * <ol>
     * <li>Her target first: the members nearest it, up to its cap (all of them, for a player).</li>
     * <li>Then the rest keep what they have while it's not crowded, the nearer to it first.</li>
     * <li>Whoever's left goes for the nearest target with room within {@link #SPREAD_RANGE}; with none, her target, or what it had.
     * With no target of hers, one hunting nothing is left hunting nothing.</li>
     * </ol>
     */
    public static int[] spread(double[][] distance, int[] current, int[] caps, int preferred) {
        int members = current.length;
        int[] load = new int[caps.length];
        int[] chosen = new int[members];
        boolean[] placed = new boolean[members];
        java.util.Arrays.fill(chosen, -1);
        if (preferred >= 0) {
            for (int m : byDistance(distance, members, preferred)) {
                if (load[preferred] >= caps[preferred] || distance[m][preferred] == Double.POSITIVE_INFINITY && current[m] != preferred) {
                    continue;
                }
                chosen[m] = preferred;
                placed[m] = true;
                load[preferred]++;
            }
        }
        Integer[] keepers = new Integer[members];
        for (int m = 0; m < members; m++) {
            keepers[m] = m;
        }
        java.util.Arrays.sort(keepers, java.util.Comparator.comparingDouble(m -> current[m] >= 0 ? distance[m][current[m]] : Double.MAX_VALUE));
        for (int m : keepers) {
            int t = current[m];
            if (!placed[m] && t >= 0 && t != preferred && load[t] < caps[t]) {
                chosen[m] = t;
                placed[m] = true;
                load[t]++;
            }
        }
        for (int m : keepers) {
            if (placed[m] || current[m] < 0 && preferred < 0) {
                // Out of a fight, one hunting nothing is left be: it isn't handed prey.
                continue;
            }
            int best = -1;
            for (int t = 0; t < caps.length; t++) {
                if (load[t] < caps[t] && distance[m][t] <= SPREAD_RANGE && (best < 0 || distance[m][t] < distance[m][best])) {
                    best = t;
                }
            }
            if (best < 0) {
                best = preferred >= 0 ? preferred : current[m];
            }
            chosen[m] = best;
            if (best >= 0) {
                load[best]++;
            }
        }
        return chosen;
    }

    private static Integer[] byDistance(double[][] distance, int members, int target) {
        Integer[] order = new Integer[members];
        for (int m = 0; m < members; m++) {
            order[m] = m;
        }
        java.util.Arrays.sort(order, java.util.Comparator.comparingDouble(m -> distance[m][target]));
        return order;
    }

    /**
     * With nothing to fight, her court lazily follows her (0.91.0, the owner: "Still lazily fallow the dread queen
     * otherwise"): a member further than this walks back toward her, at {@link #FOLLOW_SPEED}, stopping {@link #FOLLOW_TO} off.
     */
    public static final double FOLLOW_FROM = 12.0;
    public static final double FOLLOW_TO = 5.0;
    public static final double FOLLOW_SPEED = 0.8;

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
