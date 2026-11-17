package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadQueenEntity;
import com.iafenvoy.uranus.animation.AnimationHandler;
import io.github.hammerknife404.dreadpatch.magic.DragonAge;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.github.hammerknife404.dreadpatch.spell.BarrowStepSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Ice and Fire's Dread Queen with what its class leaves out. Ice and Fire's other dread mobs call
 * {@link AnimationHandler#updateAnimations} every tick; hers never does, so the spawn animation it
 * sets in {@code finalizeSpawn} stayed at tick 0 and never reached a client. Spawned as the boss she
 * rises riding an ancient Syndrigossa, bonded to it ({@link SoulTether}), until half her health is
 * gone.
 */
public class DreadQueen extends DreadQueenEntity {
    /**
     * Hers, in place of Ice and Fire's (its config's 750 health, 30 armor, no toughness): a lean
     * sovereign, kept alive by her Withering Rime ({@code RimeSiphon}). 20 armor and 12 toughness are
     * a full netherite set's.
     */
    public static final double MAX_HEALTH = 300.0;
    public static final double ARMOR = 20.0;
    public static final double ARMOR_TOUGHNESS = 12.0;
    /** Ticks of {@link #ANIMATION_SPAWN} (40 long) she spends coming out of the ground. */
    public static final int RISE_TICKS = 30;
    /** Her Withering Rime-Bolt: flat damage, then this share of the target's max health (a player's takes 8%). */
    public static final float BOLT_DAMAGE = 8.0F;
    public static final float BOLT_MAX_HEALTH_SHARE = 0.15F;
    public static final int BOLT_RIME_TICKS = 90;
    /** Ticks between her bolts, and the range (blocks) she shoots them from: not point-blank, where she uses her sword. */
    static final int BOLT_COOLDOWN = 80;
    static final double BOLT_MIN_RANGE = 3.0;
    static final double BOLT_MAX_RANGE = 32.0;

    /** Ticks between her Rimefire Crowns, how long each stands, and how far she casts one. */
    static final int CROWN_COOLDOWN = 300;
    static final int CROWN_LIFETIME = 100;
    static final double CROWN_RANGE = 20.0;
    /** After a crown, her next bolt comes this soon, to shatter it. */
    static final int CROWN_FOLLOW_UP = 25;

    /** Barrow-Step: ticks between steps (the spell's 12 s), and the nearest to her target she'll land. */
    static final int BARROW_STEP_COOLDOWN = 240;
    static final double BARROW_STEP_MIN_AWAY = 9.0;

    /** In the saddle until this share of her health is gone (the owner's notes' second phase), then she steps down for good. */
    static final float DISMOUNT_AT = 0.5F;
    /** Saved: the Syndrigossa she rose with, and whether she's still to call it (a boss spawn, or {@code WithSyndrigossa:1b}). */
    static final String DRAGON_KEY = "Syndrigossa";
    public static final String WITH_DRAGON_KEY = "WithSyndrigossa";

    private int boltCooldown = 40;
    private int crownCooldown = 100;
    private int barrowStepCooldown;
    private final DreadCourt court = new DreadCourt(this);
    @Nullable
    private UUID dragonId;
    private boolean withDragon;
    private boolean wasMounted;

    public DreadQueen(EntityType<? extends DreadQueen> type, Level level) {
        super(type, level);
    }

    /** Ice and Fire's goals, but she fights from range ({@link KeepDistanceGoal}) instead of walking into melee. */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(goal -> goal instanceof MeleeAttackGoal);
        this.goalSelector.addGoal(2, new KeepDistanceGoal(this));
    }

    /** The Syndrigossa she rose with ({@link SoulTether}), if any. */
    @Nullable
    public UUID getDragonId() {
        return this.dragonId;
    }

    /** Spawned as the boss (by the world, as an ancient Syndrigossa is), she rises riding one. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (DragonAge.spawnsAncient(reason)) {
            this.withDragon = true;
        }
        return result;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.dragonId != null) {
            tag.putUUID(DRAGON_KEY, this.dragonId);
        }
        tag.putBoolean(WITH_DRAGON_KEY, this.withDragon);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.dragonId = tag.hasUUID(DRAGON_KEY) ? tag.getUUID(DRAGON_KEY) : null;
        this.withDragon = tag.getBoolean(WITH_DRAGON_KEY);
    }

    /**
     * Her ancient Syndrigossa, raised under her as she spawns (so not out of the ground: her rise is
     * skipped), bonded to her and ridden.
     */
    private void riseWithDragon(ServerLevel level) {
        Syndrigossa dragon = ModRegistries.SYNDRIGOSSA.get().create(level);
        if (dragon == null || this.isPassenger()) {
            return;
        }
        dragon.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
        EventHooks.finalizeMobSpawn(dragon, level, level.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        dragon.makeAncient();
        dragon.bondTo(this);
        this.dragonId = dragon.getUUID();
        level.addFreshEntity(dragon);
        if (this.startRiding(dragon, true)) {
            this.wasMounted = true;
            this.setAnimation(NO_ANIMATION);
        }
    }

    /**
     * Off her dragon below {@link #DISMOUNT_AT} of her health, or thrown off in the air (it died): she
     * Barrow-Steps to the ground under it (her husk tumbles from the saddle). With no ground in reach,
     * she drifts down.
     */
    private void stepDown(@Nullable LivingEntity target) {
        this.wasMounted = false;
        Entity mount = this.getVehicle();
        Vec3 under = Utils.moveToRelativeGroundLevel(this.level(), (mount != null ? mount : this).position(), 64);
        Vec3 to = this.barrowStepDestination(under, target);
        if (to != null && BarrowStepSpell.step(this, to)) {
            this.barrowStepCooldown = BARROW_STEP_COOLDOWN;
            return;
        }
        this.stopRiding();
        this.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200));
    }

    public boolean isRising() {
        return this.getAnimation() == ANIMATION_SPAWN && this.getAnimationTick() < RISE_TICKS;
    }

    /** No walking, turning or attacking while she's still coming out of the ground. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isRising();
    }

    /**
     * Her loot table always drops her sword and staff, so the ones in her hands never drop as
     * equipment too (vanilla's 8.5% chance, more with looting, would sometimes give two). Applies
     * whatever drop chances a saved queen carries.
     */
    @Override
    protected float getEquipmentDropChance(EquipmentSlot slot) {
        return slot.getType() == EquipmentSlot.Type.HAND ? 0.0F : super.getEquipmentDropChance(slot);
    }

    /** Her spells and her court, on top of her movement ({@link KeepDistanceGoal}). Not called while she's rising ({@link #isImmobile}). */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.boltCooldown > 0) {
            this.boltCooldown--;
        }
        if (this.crownCooldown > 0) {
            this.crownCooldown--;
        }
        if (this.barrowStepCooldown > 0) {
            this.barrowStepCooldown--;
        }
        LivingEntity target = this.getTarget();
        if (target != null && !target.isAlive()) {
            target = null;
        } else if (target != null && !this.canAttack(target)) {
            // One her dragon handed her, or a player gone creative or spectator: no target goal rechecks it.
            this.setTarget(null);
            target = null;
        }
        if (this.getVehicle() instanceof Syndrigossa && this.getHealth() <= this.getMaxHealth() * DISMOUNT_AT) {
            this.stepDown(target);
        }
        if (this.tickCount % 20 == 0) {
            target = this.shareTarget(target);
        }
        this.court.tick(target);
        if (target == null) {
            return;
        }
        double distance = this.distanceTo(target);
        if (this.crownCooldown == 0 && distance <= CROWN_RANGE && this.hasLineOfSight(target)) {
            this.raiseCrown(target);
            this.crownCooldown = CROWN_COOLDOWN;
            this.boltCooldown = Math.min(this.boltCooldown, CROWN_FOLLOW_UP);
            return;
        }
        if (this.boltCooldown == 0 && distance >= BOLT_MIN_RANGE && distance <= BOLT_MAX_RANGE && this.hasLineOfSight(target)) {
            this.shootBolt(target);
            this.boltCooldown = BOLT_COOLDOWN;
        }
    }

    /** She and her dragon hunt together while bonded: whichever has no target takes the other's. Returns hers. */
    @Nullable
    private LivingEntity shareTarget(@Nullable LivingEntity mine) {
        if (!(SoulTether.partner(this) instanceof Syndrigossa dragon)) {
            return mine;
        }
        LivingEntity its = dragon.getTarget();
        boolean itsLive = its != null && its.isAlive();
        if (mine != null && !itsLive && dragon.canAttack(mine)) {
            dragon.setTarget(mine);
        } else if (mine == null && itsLive && this.canAttack(its)) {
            this.setTarget(its);
            return its;
        }
        return mine;
    }

    /** Rimefire Crown round her target, then a bolt to shatter it ({@link #CROWN_FOLLOW_UP}). */
    private void raiseCrown(LivingEntity target) {
        this.level().addFreshEntity(RimefireCrown.create(this.level(), this, target.position(), CROWN_LIFETIME));
        this.swing(InteractionHand.OFF_HAND);
        this.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.0F, 0.5F);
    }

    /**
     * Barrow-Step away from a target that's cornered her ({@link KeepDistanceGoal}): to ground she
     * can stand on at least {@link #BARROW_STEP_MIN_AWAY} blocks from it, leaving her husk behind.
     * Whether she went; on cooldown, or with nowhere to go, she runs instead.
     */
    boolean barrowStep(LivingEntity target) {
        if (this.barrowStepCooldown > 0) {
            return false;
        }
        Vec3 to = this.barrowStepDestination(this.position(), target);
        if (to == null || !BarrowStepSpell.step(this, to)) {
            return false;
        }
        this.barrowStepCooldown = BARROW_STEP_COOLDOWN;
        this.getNavigation().stop();
        return true;
    }

    /**
     * 10-15 blocks from {@code origin} (where she stands, or the ground under her dragon), straight
     * away from her target first, then wider: on solid ground with room for her, and if she has a
     * target, at least {@link #BARROW_STEP_MIN_AWAY} from it.
     */
    @Nullable
    private Vec3 barrowStepDestination(Vec3 origin, @Nullable LivingEntity target) {
        Vec3 away = target == null ? Vec3.ZERO : origin.subtract(target.position()).multiply(1.0, 0.0, 1.0);
        away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.getYRot()).reverse() : away.normalize();
        for (int attempt = 0; attempt < 16; attempt++) {
            double spread = attempt < 8 ? Math.PI / 4 : Math.PI / 2;
            Vec3 dir = away.yRot((float) ((this.random.nextDouble() * 2.0 - 1.0) * spread));
            Vec3 guess = origin.add(dir.scale(10.0 + this.random.nextDouble() * 5.0));
            // Iron's lands a hair under a block's top: lift it before looking below.
            Vec3 ground = Utils.moveToRelativeGroundLevel(this.level(), guess, 6);
            BlockPos feet = BlockPos.containing(ground.x, ground.y + 0.01, ground.z);
            Vec3 to = new Vec3(ground.x, feet.getY(), ground.z);
            if ((target == null || to.distanceTo(target.position()) >= BARROW_STEP_MIN_AWAY)
                    && this.level().getBlockState(feet.below()).isFaceSturdy(this.level(), feet.below(), Direction.UP)
                    && this.level().getFluidState(feet).isEmpty()
                    && this.level().noCollision(this, this.getBoundingBox().move(to.subtract(this.position())))) {
                return to;
            }
        }
        return null;
    }

    private void shootBolt(LivingEntity target) {
        WitheringRimeBolt bolt = new WitheringRimeBolt(this.level(), this, BOLT_DAMAGE, BOLT_MAX_HEALTH_SHARE, BOLT_RIME_TICKS);
        bolt.setPos(this.getX(), this.getEyeY() - 0.4, this.getZ());
        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.6, 0).subtract(bolt.position());
        bolt.shoot(aim.normalize());
        this.level().addFreshEntity(bolt);
        this.swing(InteractionHand.OFF_HAND);
        this.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 0.6F);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && this.isRising()) {
            // The ground breaking around her, as for Ice and Fire's rising dread thralls.
            BlockState below = this.level().getBlockState(this.blockPosition().below());
            if (!below.isAir()) {
                for (int i = 0; i < 5; i++) {
                    this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, below),
                            this.getX() + (this.random.nextFloat() * 2.0F - 1.0F) * this.getBbWidth(),
                            this.getBoundingBox().minY,
                            this.getZ() + (this.random.nextFloat() * 2.0F - 1.0F) * this.getBbWidth(),
                            this.random.nextGaussian() * 0.02D, this.random.nextGaussian() * 0.02D, this.random.nextGaussian() * 0.02D);
                }
            }
        }
        if (this.level() instanceof ServerLevel level) {
            // Before the animation's first tick goes out, so a queen rising with her dragon never shows her rise.
            if (this.withDragon) {
                this.withDragon = false;
                this.riseWithDragon(level);
            }
            boolean mounted = this.getVehicle() instanceof Syndrigossa;
            if (this.wasMounted && !mounted && this.isAlive() && !this.onGround() && !this.isInWater()) {
                this.stepDown(this.getTarget());
            }
            this.wasMounted = mounted;
        }
        // Counts the animation along on both sides; on its first server tick it also tells clients
        // it started, and at its end it goes back to NO_ANIMATION.
        AnimationHandler.INSTANCE.updateAnimations(this);
    }
}
