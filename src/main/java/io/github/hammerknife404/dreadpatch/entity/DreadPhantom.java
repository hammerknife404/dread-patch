package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import io.github.hammerknife404.dreadpatch.magic.PhantomRules;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * The dread phantom (0.45.0): vanilla's phantom, of the Dread. The owner's terms: vanilla's stats
 * ("they're hated enough"), but it doesn't burn by day; the Dread's eyes and a look of both, with
 * more holes in its wings showing bone (drawn client-side); a sparse Rimefrost trail; a ghostlier
 * sound on top of a phantom's; a bite that may leave Rimefrost and Withering Rime, each rolled apart
 * ({@link PhantomRules}).
 * <p>
 * Called by the queen, a Dread Lich and Syndrigossa ({@link PhantomCalling}), it escorts its summoner,
 * circling above them, takes its summoner's fights, and turns on whatever hurts its summoner or it,
 * diving the sooner the harder the difficulty. Called by a player's spell, it keeps a summoned
 * court's rules ({@link SummonedCourt}). With no summoner, it hunts players as a phantom does. It's
 * of the Dread: allied with them, refusing their rime.
 * <p>
 * Vanilla's phantom flies by goals and a move control that share package-private fields, so its own
 * are copied here onto fields of ours ({@link #moveTarget}, {@link #anchor}, {@link #diving}), with
 * changes: any living target, not only players; the target kept after a dive (vanilla drops it after
 * every bite and finds a player again); an anchor that follows its summoner. Saved under vanilla's
 * keys ({@code AX}, {@code AY}, {@code AZ}), plus {@code Summoner}.
 */
public class DreadPhantom extends Phantom implements IDreadMob {
    public static final String SUMMONER_KEY = "Summoner";
    /** How high over its summoner it circles, idle. */
    static final int ESCORT_HEIGHT = 10;
    /** Past this far, it doesn't try to keep up (another dimension, or lost). */
    static final double ESCORT_RANGE = 96.0;

    @Nullable
    private UUID summonerId;
    Vec3 moveTarget = Vec3.ZERO;
    BlockPos anchor = BlockPos.ZERO;
    boolean diving;
    /** Set when it takes a new target, so its next dive comes sooner. */
    private boolean freshTarget;

    public DreadPhantom(EntityType<? extends DreadPhantom> type, Level level) {
        super(type, level);
        this.moveControl = new FlightControl(this);
    }

    /** A phantom's (vanilla gives it a monster's), with the reach to keep a target it circles 20–40 blocks above. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new AttackStrategyGoal());
        this.goalSelector.addGoal(2, new DiveGoal());
        this.goalSelector.addGoal(3, new CircleGoal());
        this.targetSelector.addGoal(1, new RevengeGoal(this));
        this.targetSelector.addGoal(2, new AvengeSummonerGoal());
        this.targetSelector.addGoal(3, new HuntPlayersGoal());
    }

    @Nullable
    public UUID getSummonerId() {
        return this.summonerId;
    }

    public void setSummoner(Entity summoner) {
        this.summonerId = summoner.getUUID();
    }

    /** Its summoner, if they're in this level: a player, or any entity. */
    @Nullable
    public Entity summoner() {
        if (this.summonerId == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        Player player = level.getPlayerByUUID(this.summonerId);
        return player != null ? player : level.getEntity(this.summonerId);
    }

    @Override
    public Entity getCommander() {
        return this.summoner();
    }

    /** Never burns by day. */
    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity instanceof IDreadMob || super.isAlliedTo(entity);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof IDreadMob) && super.canAttack(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity before = this.getTarget();
        super.setTarget(target);
        if (target != null && this.getTarget() == target && target != before) {
            this.freshTarget = true;
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        this.anchor = this.blockPosition().above(5);
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || !this.canAttack(target))) {
            this.setTarget(null);
            target = null;
        }
        Entity summoner = this.summoner();
        if (summoner == null || !summoner.isAlive() || summoner.distanceToSqr(this) > ESCORT_RANGE * ESCORT_RANGE) {
            return;
        }
        if (target == null && this.tickCount % 20 == 0 && summoner instanceof Mob mob && mob.getTarget() != null
                && mob.getTarget().isAlive() && this.canAttack(mob.getTarget())) {
            // Its summoner's fight is its own.
            this.setTarget(mob.getTarget());
            target = this.getTarget();
        }
        if (target == null) {
            // Idle, it circles over its summoner.
            this.anchor = summoner.blockPosition().above(ESCORT_HEIGHT);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity victim) {
            if (PhantomRules.rimefrost(this.random.nextFloat())) {
                victim.addEffect(new MobEffectInstance(ModRegistries.RIMEFROST, PhantomRules.RIMEFROST_TICKS), this);
            }
            if (PhantomRules.witheringRime(this.random.nextFloat())) {
                victim.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, PhantomRules.WITHERING_TICKS), this);
            }
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.tickCount % 3 == 0 && this.random.nextFloat() < 0.6F) {
            // A sparse Rimefrost trail, off its tail: snowflakes and soot, and now and then a cyan flame.
            float yaw = this.getYRot() * Mth.DEG_TO_RAD;
            double x = this.getX() + Mth.sin(yaw) * 0.9 + (this.random.nextDouble() - 0.5) * 0.3;
            double y = this.getY() + 0.2;
            double z = this.getZ() - Mth.cos(yaw) * 0.9 + (this.random.nextDouble() - 0.5) * 0.3;
            float pick = this.random.nextFloat();
            if (pick < 0.15F) {
                // Now and then a cold cyan flame, the Rimefrost breath's at the bolt's size (0.50.0).
                this.level().addParticle(ModRegistries.RIME_FLAME_SMALL.get(), x, y, z, 0.0, -0.005, 0.0);
            } else if (pick < 0.575F) {
                this.level().addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0.0, -0.03, 0.0);
            } else {
                this.level().addParticle(ModRegistries.RIME_SMOKE_SMALL.get(), x, y, z, 0.0, -0.01, 0.0);
            }
        }
    }

    /** A phantom's ambient, hurt and death sounds, with something ghostlier under each. */
    @Override
    public void makeSound(@Nullable SoundEvent sound) {
        super.makeSound(sound);
        SoundEvent ghost = sound == SoundEvents.PHANTOM_AMBIENT ? ModRegistries.DREAD_PHANTOM_WAIL.get()
                : sound == SoundEvents.PHANTOM_HURT ? ModRegistries.DREAD_PHANTOM_HURT.get()
                : sound == SoundEvents.PHANTOM_DEATH ? ModRegistries.DREAD_PHANTOM_DEATH.get()
                : null;
        if (ghost != null) {
            this.playSound(ghost, this.getSoundVolume(), this.getVoicePitch());
        }
    }

    /** A player's are a summoned court's: called up, they leave nothing behind. */
    @Override
    protected boolean shouldDropLoot() {
        return !SummonedCourt.isMember(this) && super.shouldDropLoot();
    }

    @Override
    protected int getBaseExperienceReward() {
        return SummonedCourt.isMember(this) ? 0 : super.getBaseExperienceReward();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AX", this.anchor.getX());
        tag.putInt("AY", this.anchor.getY());
        tag.putInt("AZ", this.anchor.getZ());
        if (this.summonerId != null) {
            tag.putUUID(SUMMONER_KEY, this.summonerId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AX")) {
            this.anchor = new BlockPos(tag.getInt("AX"), tag.getInt("AY"), tag.getInt("AZ"));
        }
        this.summonerId = tag.hasUUID(SUMMONER_KEY) ? tag.getUUID(SUMMONER_KEY) : null;
    }

    private int difficulty() {
        return this.level().getDifficulty().getId();
    }

    /** Vanilla's phantom flight, on our fields. */
    static class FlightControl extends MoveControl {
        private final DreadPhantom phantom;
        private float speed = 0.1F;

        FlightControl(DreadPhantom phantom) {
            super(phantom);
            this.phantom = phantom;
        }

        @Override
        public void tick() {
            DreadPhantom p = this.phantom;
            if (p.horizontalCollision) {
                p.setYRot(p.getYRot() + 180.0F);
                this.speed = 0.1F;
            }
            double dx = p.moveTarget.x - p.getX();
            double dy = p.moveTarget.y - p.getY();
            double dz = p.moveTarget.z - p.getZ();
            double flat = Math.sqrt(dx * dx + dz * dz);
            if (Math.abs(flat) > 1.0E-5F) {
                double level = 1.0 - Math.abs(dy * 0.7F) / flat;
                dx *= level;
                dz *= level;
                flat = Math.sqrt(dx * dx + dz * dz);
                double all = Math.sqrt(dx * dx + dz * dz + dy * dy);
                float yaw = p.getYRot();
                float heading = (float) Mth.atan2(dz, dx);
                float current = Mth.wrapDegrees(p.getYRot() + 90.0F);
                float wanted = Mth.wrapDegrees(heading * Mth.RAD_TO_DEG);
                p.setYRot(Mth.approachDegrees(current, wanted, 4.0F) - 90.0F);
                p.yBodyRot = p.getYRot();
                if (Mth.degreesDifferenceAbs(yaw, p.getYRot()) < 3.0F) {
                    this.speed = Mth.approach(this.speed, 1.8F, 0.005F * (1.8F / this.speed));
                } else {
                    this.speed = Mth.approach(this.speed, 0.2F, 0.025F);
                }
                float pitch = (float) (-(Mth.atan2(-dy, flat) * Mth.RAD_TO_DEG));
                p.setXRot(pitch);
                float facing = p.getYRot() + 90.0F;
                double vx = this.speed * Mth.cos(facing * Mth.DEG_TO_RAD) * Math.abs(dx / all);
                double vz = this.speed * Mth.sin(facing * Mth.DEG_TO_RAD) * Math.abs(dz / all);
                double vy = this.speed * Mth.sin(pitch * Mth.DEG_TO_RAD) * Math.abs(dy / all);
                Vec3 motion = p.getDeltaMovement();
                p.setDeltaMovement(motion.add(new Vec3(vx, vy, vz).subtract(motion).scale(0.2)));
            }
        }
    }

    /** Vanilla's: with a target, circle high over it, and every so often dive. Sooner, the harder the difficulty. */
    class AttackStrategyGoal extends Goal {
        private int nextDive;

        @Override
        public boolean canUse() {
            LivingEntity target = DreadPhantom.this.getTarget();
            return target != null && DreadPhantom.this.canAttack(target, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            this.nextDive = this.adjustedTickDelay(PhantomRules.firstSwoopTicks(DreadPhantom.this.difficulty()));
            DreadPhantom.this.freshTarget = false;
            DreadPhantom.this.diving = false;
            this.anchorAboveTarget();
        }

        @Override
        public void stop() {
            DreadPhantom.this.anchor = DreadPhantom.this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, DreadPhantom.this.anchor)
                    .above(10 + DreadPhantom.this.random.nextInt(20));
        }

        @Override
        public void tick() {
            if (DreadPhantom.this.freshTarget) {
                // A new target (revenge, or its summoner's): at it soon.
                DreadPhantom.this.freshTarget = false;
                this.nextDive = Math.min(this.nextDive, this.adjustedTickDelay(PhantomRules.firstSwoopTicks(DreadPhantom.this.difficulty())));
            }
            if (!DreadPhantom.this.diving && --this.nextDive <= 0) {
                DreadPhantom.this.diving = true;
                this.anchorAboveTarget();
                this.nextDive = this.adjustedTickDelay(PhantomRules.swoopTicks(DreadPhantom.this.difficulty(), DreadPhantom.this.random.nextDouble()));
                DreadPhantom.this.playSound(SoundEvents.PHANTOM_SWOOP, 10.0F, 0.95F + DreadPhantom.this.random.nextFloat() * 0.1F);
                DreadPhantom.this.playSound(ModRegistries.DREAD_PHANTOM_SWOOP.get(), 3.0F, 0.95F + DreadPhantom.this.random.nextFloat() * 0.1F);
            }
        }

        private void anchorAboveTarget() {
            BlockPos above = DreadPhantom.this.getTarget().blockPosition().above(20 + DreadPhantom.this.random.nextInt(20));
            int sea = DreadPhantom.this.level().getSeaLevel();
            DreadPhantom.this.anchor = above.getY() < sea ? new BlockPos(above.getX(), sea + 1, above.getZ()) : above;
        }
    }

    abstract class FlightGoal extends Goal {
        FlightGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        protected boolean touchingTarget() {
            return DreadPhantom.this.moveTarget.distanceToSqr(DreadPhantom.this.getX(), DreadPhantom.this.getY(), DreadPhantom.this.getZ()) < 4.0;
        }
    }

    /**
     * Vanilla's dive, at any living target, committed: it presses on through hits until it bites or meets a
     * wall. A cat still turns it away. It keeps its target after.
     */
    class DiveGoal extends FlightGoal {
        private boolean scaredOfCat;
        private int catSearchTick;

        @Override
        public boolean canUse() {
            return DreadPhantom.this.getTarget() != null && DreadPhantom.this.diving;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = DreadPhantom.this.getTarget();
            if (target == null || !target.isAlive() || !this.canUse()) {
                return false;
            }
            if (target instanceof Player player && (player.isSpectator() || player.isCreative())) {
                return false;
            }
            if (DreadPhantom.this.tickCount > this.catSearchTick) {
                this.catSearchTick = DreadPhantom.this.tickCount + 20;
                List<Cat> cats = DreadPhantom.this.level().getEntitiesOfClass(Cat.class, DreadPhantom.this.getBoundingBox().inflate(16.0), EntitySelector.ENTITY_STILL_ALIVE);
                cats.forEach(Cat::hiss);
                this.scaredOfCat = !cats.isEmpty();
            }
            return !this.scaredOfCat;
        }

        @Override
        public void stop() {
            DreadPhantom.this.diving = false;
        }

        @Override
        public void tick() {
            LivingEntity target = DreadPhantom.this.getTarget();
            if (target == null) {
                return;
            }
            DreadPhantom.this.moveTarget = new Vec3(target.getX(), target.getY(0.5), target.getZ());
            if (DreadPhantom.this.getBoundingBox().inflate(0.2F).intersects(target.getBoundingBox())) {
                DreadPhantom.this.doHurtTarget(target);
                DreadPhantom.this.diving = false;
                if (!DreadPhantom.this.isSilent()) {
                    DreadPhantom.this.level().levelEvent(1039, DreadPhantom.this.blockPosition(), 0);
                }
            } else if (DreadPhantom.this.horizontalCollision) {
                // Only a wall turns it aside. Vanilla's also pulls out when hit (hurtTime); the owner (0.45.1):
                // "commit to hitting and ignore being hit causing them to retreat".
                DreadPhantom.this.diving = false;
            }
        }
    }

    /** Vanilla's circling round its anchor (over its target, or over its summoner). */
    class CircleGoal extends FlightGoal {
        private float angle;
        private float distance;
        private float height;
        private float clockwise;

        @Override
        public boolean canUse() {
            return DreadPhantom.this.getTarget() == null || !DreadPhantom.this.diving;
        }

        @Override
        public void start() {
            this.distance = 5.0F + DreadPhantom.this.random.nextFloat() * 10.0F;
            this.height = -4.0F + DreadPhantom.this.random.nextFloat() * 9.0F;
            this.clockwise = DreadPhantom.this.random.nextBoolean() ? 1.0F : -1.0F;
            this.selectNext();
        }

        @Override
        public void tick() {
            if (DreadPhantom.this.random.nextInt(this.adjustedTickDelay(350)) == 0) {
                this.height = -4.0F + DreadPhantom.this.random.nextFloat() * 9.0F;
            }
            if (DreadPhantom.this.random.nextInt(this.adjustedTickDelay(250)) == 0) {
                this.distance++;
                if (this.distance > 15.0F) {
                    this.distance = 5.0F;
                    this.clockwise = -this.clockwise;
                }
            }
            if (DreadPhantom.this.random.nextInt(this.adjustedTickDelay(450)) == 0) {
                this.angle = DreadPhantom.this.random.nextFloat() * 2.0F * Mth.PI;
                this.selectNext();
            }
            if (this.touchingTarget()) {
                this.selectNext();
            }
            if (DreadPhantom.this.moveTarget.y < DreadPhantom.this.getY() && !DreadPhantom.this.level().isEmptyBlock(DreadPhantom.this.blockPosition().below(1))) {
                this.height = Math.max(1.0F, this.height);
                this.selectNext();
            }
            if (DreadPhantom.this.moveTarget.y > DreadPhantom.this.getY() && !DreadPhantom.this.level().isEmptyBlock(DreadPhantom.this.blockPosition().above(1))) {
                this.height = Math.min(-1.0F, this.height);
                this.selectNext();
            }
        }

        private void selectNext() {
            if (BlockPos.ZERO.equals(DreadPhantom.this.anchor)) {
                DreadPhantom.this.anchor = DreadPhantom.this.blockPosition();
            }
            this.angle += this.clockwise * 15.0F * Mth.DEG_TO_RAD;
            DreadPhantom.this.moveTarget = Vec3.atLowerCornerOf(DreadPhantom.this.anchor)
                    .add(this.distance * Mth.cos(this.angle), -4.0F + this.height, this.distance * Mth.sin(this.angle));
        }
    }

    /**
     * Turns on what hurt it (vanilla's {@code HurtByTargetGoal} needs a walking mob), and its
     * summoner's others with it. A summoned court's keeps its rules: what it mayn't fight, it won't
     * take ({@link SummonedCourt}).
     */
    public static class RevengeGoal extends TargetGoal {
        private final DreadPhantom phantom;
        private int timestamp;

        public RevengeGoal(DreadPhantom phantom) {
            super(phantom, false);
            this.phantom = phantom;
        }

        @Override
        public boolean canUse() {
            LivingEntity attacker = this.phantom.getLastHurtByMob();
            return attacker != null && this.phantom.getLastHurtByMobTimestamp() != this.timestamp
                    && this.canAttack(attacker, TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting());
        }

        @Override
        public void start() {
            LivingEntity attacker = this.phantom.getLastHurtByMob();
            this.timestamp = this.phantom.getLastHurtByMobTimestamp();
            this.phantom.setTarget(attacker);
            this.targetMob = this.phantom.getTarget();
            UUID summoner = this.phantom.getSummonerId();
            if (summoner != null && attacker != null) {
                for (DreadPhantom other : this.phantom.level().getEntitiesOfClass(DreadPhantom.class, this.phantom.getBoundingBox().inflate(32.0),
                        p -> p != this.phantom && summoner.equals(p.getSummonerId()) && p.getTarget() == null)) {
                    other.setTarget(attacker);
                }
            }
            super.start();
        }
    }

    /** Turns on whatever hurts its summoner. */
    class AvengeSummonerGoal extends TargetGoal {
        private int timestamp;

        AvengeSummonerGoal() {
            super(DreadPhantom.this, false);
        }

        @Override
        public boolean canUse() {
            if (!(DreadPhantom.this.summoner() instanceof LivingEntity summoner)) {
                return false;
            }
            LivingEntity attacker = summoner.getLastHurtByMob();
            return attacker != null && summoner.getLastHurtByMobTimestamp() != this.timestamp && attacker != DreadPhantom.this
                    && this.canAttack(attacker, TargetingConditions.forCombat().ignoreLineOfSight().ignoreInvisibilityTesting());
        }

        @Override
        public void start() {
            LivingEntity summoner = (LivingEntity) DreadPhantom.this.summoner();
            if (summoner != null) {
                this.timestamp = summoner.getLastHurtByMobTimestamp();
                DreadPhantom.this.setTarget(summoner.getLastHurtByMob());
                this.targetMob = DreadPhantom.this.getTarget();
            }
            super.start();
        }
    }

    /**
     * Vanilla's: the highest player it can attack within 64 blocks below it, looked for every 3 s. Only
     * while it has nothing else to fight, so a player never pulls it off a summoner's fight.
     */
    class HuntPlayersGoal extends Goal {
        private final TargetingConditions attackTargeting = TargetingConditions.forCombat().range(64.0);
        private int nextScanTick = reducedTickDelay(20);

        @Override
        public boolean canUse() {
            if (DreadPhantom.this.getTarget() != null) {
                return false;
            }
            if (this.nextScanTick > 0) {
                this.nextScanTick--;
                return false;
            }
            this.nextScanTick = reducedTickDelay(60);
            List<Player> players = DreadPhantom.this.level().getNearbyPlayers(this.attackTargeting, DreadPhantom.this, DreadPhantom.this.getBoundingBox().inflate(16.0, 64.0, 16.0));
            players.sort(Comparator.<Player, Double>comparing(Entity::getY).reversed());
            for (Player player : players) {
                if (DreadPhantom.this.canAttack(player, TargetingConditions.DEFAULT)) {
                    DreadPhantom.this.setTarget(player);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = DreadPhantom.this.getTarget();
            return target != null && DreadPhantom.this.canAttack(target, TargetingConditions.DEFAULT);
        }
    }
}
