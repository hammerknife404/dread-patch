package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadMobEntity;
import io.github.hammerknife404.dreadpatch.magic.CourtRules;
import io.github.hammerknife404.dreadpatch.magic.DreadlandsSpawning;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Dread patrols (0.52.0, the owner's second drop: "small Dread patrols spawn naturally day and
 * night"): a dread knight on foot (its horse sent away: mounted, it'd leave the rest behind) leading
 * 2-4 more of her court's kinds across the Dreadlands. Its leader walks to a waypoint, then picks the
 * next 64-128 blocks on; the rest keep up with it; anything they meet that the Dread fight, they fight,
 * then walk on. If the leader falls, the first to notice leads. Marked in their persistent data
 * ({@link #LEADER_KEY}, the leader's waypoint {@link #WAYPOINT_KEY}); the goal is added each time one
 * joins a level (goals aren't saved). Not kept: they despawn as monsters do.
 */
public final class DreadPatrols {
    /** On every member: its leader's UUID (the leader's own, on the leader). */
    public static final String LEADER_KEY = "dreadpatch:patrol_leader";
    /** On the leader: where it's walking, x, y, z. */
    public static final String WAYPOINT_KEY = "dreadpatch:patrol_waypoint";
    static final double KEEP_UP = 6.0;
    static final double ARRIVED = 6.0;

    private DreadPatrols() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadPatrols::onJoin);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof DreadMobEntity member && member.getPersistentData().hasUUID(LEADER_KEY)) {
            // Between Ice and Fire's melee (2) and its stroll (5), as a summoned court follows.
            member.goalSelector.addGoal(4, new PatrolGoal(member));
        }
    }

    /**
     * A patrol rises round {@code at} (on the ground there): its knight, then 2-4 more, the knight's first
     * waypoint {@code toward}. How many rose.
     */
    public static int raise(ServerLevel level, Vec3 at, BlockPos toward, RandomSource random) {
        DreadMobEntity leader = DreadCourt.create(level, CourtRules.Kind.KNIGHT);
        if (leader == null || !rise(level, leader, at, random)) {
            return 0;
        }
        UUID id = leader.getUUID();
        leader.getPersistentData().putUUID(LEADER_KEY, id);
        setWaypoint(leader, toward);
        int raised = 1;
        int followers = DreadlandsSpawning.patrolFollowers(new java.util.Random(random.nextLong()));
        for (int i = 0; i < followers; i++) {
            CourtRules.Kind kind;
            do {
                kind = CourtRules.pick(random.nextInt(CourtRules.TOTAL_WEIGHT));
            } while (kind == CourtRules.Kind.KNIGHT);
            DreadMobEntity member = DreadCourt.create(level, kind);
            if (member != null) {
                member.getPersistentData().putUUID(LEADER_KEY, id);
                if (rise(level, member, at, random)) {
                    raised++;
                }
            }
        }
        return raised;
    }

    /**
     * {@code count} of her court's kinds rise round {@code at} (on the ground there), loose: no patrol, no
     * commander (0.52.0, the Dreadlands' own). How many rose.
     */
    public static int raiseLoose(ServerLevel level, Vec3 at, int count, RandomSource random) {
        int raised = 0;
        for (int i = 0; i < count; i++) {
            DreadMobEntity mob = DreadCourt.create(level, CourtRules.pick(random.nextInt(CourtRules.TOTAL_WEIGHT)));
            if (mob != null && rise(level, mob, at, random)) {
                raised++;
            }
        }
        return raised;
    }

    /** Whether a patrol's leader is within {@code range} of {@code at}. */
    public static boolean patrolNear(ServerLevel level, Vec3 at, double range) {
        return !level.getEntitiesOfClass(DreadMobEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(range), mob -> {
            CompoundTag data = mob.getPersistentData();
            return data.hasUUID(LEADER_KEY) && mob.getUUID().equals(data.getUUID(LEADER_KEY));
        }).isEmpty();
    }

    /** One of a patrol, on the ground near {@code at}, risen as the Dread rise, on foot. */
    private static boolean rise(ServerLevel level, DreadMobEntity mob, Vec3 at, RandomSource random) {
        Vec3 spot = DreadCourt.findSpot(level, at, random, mob);
        if (spot == null) {
            return false;
        }
        mob.moveTo(spot.x, spot.y, spot.z, random.nextFloat() * 360.0F, 0.0F);
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.NATURAL, null);
        if (!level.addFreshEntity(mob)) {
            return false;
        }
        Entity mount = mob.getVehicle();
        if (mount != null) {
            mob.stopRiding();
            mount.discard();
        }
        return true;
    }

    static void setWaypoint(Mob leader, BlockPos at) {
        leader.getPersistentData().putIntArray(WAYPOINT_KEY, new int[]{at.getX(), at.getY(), at.getZ()});
    }

    @Nullable
    static BlockPos waypoint(Mob leader) {
        int[] at = leader.getPersistentData().getIntArray(WAYPOINT_KEY);
        return at.length == 3 ? new BlockPos(at[0], at[1], at[2]) : null;
    }

    /** The next waypoint: 64-128 blocks off, on the ground. */
    static BlockPos nextWaypoint(Mob leader) {
        RandomSource random = leader.getRandom();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = DreadlandsSpawning.PATROL_LEG_MIN + random.nextDouble() * (DreadlandsSpawning.PATROL_LEG_MAX - DreadlandsSpawning.PATROL_LEG_MIN);
        int x = Mth.floor(leader.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(leader.getZ() + Math.sin(angle) * distance);
        int y = leader.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    /** Walk the patrol: the leader to its waypoint, the rest after the leader. Not while fighting. */
    static class PatrolGoal extends Goal {
        private final DreadMobEntity mob;
        private int repath;

        PatrolGoal(DreadMobEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        private boolean leads() {
            CompoundTag data = this.mob.getPersistentData();
            return data.hasUUID(LEADER_KEY) && this.mob.getUUID().equals(data.getUUID(LEADER_KEY));
        }

        @Nullable
        private Mob leader() {
            CompoundTag data = this.mob.getPersistentData();
            if (!data.hasUUID(LEADER_KEY) || !(this.mob.level() instanceof ServerLevel level)) {
                return null;
            }
            return level.getEntity(data.getUUID(LEADER_KEY)) instanceof Mob leader && leader.isAlive() ? leader : null;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob.getTarget();
            if (target != null && target.isAlive()) {
                return false;
            }
            if (this.leads()) {
                return true;
            }
            Mob leader = this.leader();
            if (leader == null) {
                // The leader's gone: this one leads now.
                this.mob.getPersistentData().putUUID(LEADER_KEY, this.mob.getUUID());
                setWaypoint(this.mob, nextWaypoint(this.mob));
                return true;
            }
            return this.mob.distanceToSqr(leader) > KEEP_UP * KEEP_UP;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.repath = 0;
        }

        @Override
        public void stop() {
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (--this.repath > 0) {
                return;
            }
            this.repath = 20;
            if (this.leads()) {
                BlockPos to = waypoint(this.mob);
                if (to == null || this.mob.distanceToSqr(Vec3.atBottomCenterOf(to)) < ARRIVED * ARRIVED) {
                    to = nextWaypoint(this.mob);
                    setWaypoint(this.mob, to);
                }
                this.mob.getNavigation().moveTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 0.9);
            } else {
                Mob leader = this.leader();
                if (leader != null) {
                    this.mob.getNavigation().moveTo(leader, 1.0);
                }
            }
        }
    }
}
