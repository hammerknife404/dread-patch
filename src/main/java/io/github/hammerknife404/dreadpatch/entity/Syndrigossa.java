package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonAttacks;
import com.iafenvoy.uranus.animation.Animation;
import io.github.hammerknife404.dreadpatch.magic.DragonBreath;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Syndrigossa, the Dread Queen's undead frost wyrm: proof of concept. An Ice and Fire ice dragon
 * underneath (its model, animations, growth, flight, riding and AI all come with
 * {@link IceDragonEntity}; Ice and Fire's renderer picks the ice dragon model by {@code instanceof}),
 * one of the Dread (allied with the queen and her court, immune to Withering Rime), drawn rotted to
 * the bone by {@code SyndrigossaRenderer}. She breathes Withering Rime instead of frost
 * ({@link #witheringBreath}, through {@code SyndrigossaEvents}: Ice and Fire's breath stream, aim and
 * timing, with her damage where it lands), her charge is Shatter-Spine ({@link ShatterSpineCore}),
 * and every 30 s she raises Sovereign's Eclipse round her target. Her drops and her stats are still an
 * ice dragon's.
 */
public class Syndrigossa extends IceDragonEntity implements IDreadMob {
    /**
     * Ice and Fire looks a dragon's colour up by name for its textures, eyes and drops, and a colour
     * of our own would add itself to every ice dragon's palette (wild ones would spawn in it). So
     * underneath she's always white, and our renderer paints over it.
     */
    public static final String BASE_COLOR = "white";
    /** Sovereign's Eclipse: ticks between them, how far off her target may be, and how grown she must be. */
    static final int ECLIPSE_COOLDOWN = 600;
    static final double ECLIPSE_RANGE = 24.0;
    static final int ECLIPSE_MIN_STAGE = 3;

    private int eclipseCooldown = 200;
    /** On the ground with a live target, the chance each tick she takes off: about every 3 s (Ice and Fire: 1 in 1,500, and only after a target 5+ blocks above or below). */
    static final int TAKEOFF_CHANCE = 60;
    /** Breath particles a tick, through the cone. */
    static final int BREATH_PARTICLES = 18;

    public Syndrigossa(EntityType<? extends Syndrigossa> type, Level level) {
        super(type, level);
    }

    @Override
    public String getVariant() {
        return BASE_COLOR;
    }

    /** Whom she answers to: no one yet. */
    @Nullable
    @Override
    public Entity getCommander() {
        return null;
    }

    /**
     * A dragon made from a tag ({@code /summon} with NBT skips {@code finalizeSpawn}) reads its
     * health while its max is still a hatchling's 20 (the tag has no saved attributes, which a saved
     * dragon's does), and Ice and Fire only grows the max to its age afterwards, here: summoned
     * full-grown she had 20 of 419 and fell to one warden hit, and {@code Health:300} came out 20.
     * Ice and Fire's own ice dragons do the same. So once her max is set: the tag's health, or whole.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setHealth(tag.contains("Health", Tag.TAG_ANY_NUMERIC) ? tag.getFloat("Health") : this.getMaxHealth());
    }

    /** Every 30 s, grown, with her target in sight within 24 blocks: Sovereign's Eclipse round it. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !this.isAlive() || this.isModelDead()) {
            return;
        }
        LivingEntity hunted = this.getTarget();
        if (hunted != null && hunted.isAlive() && !this.isFlying() && !this.isHovering() && !this.isVehicle() && !this.isOrderedToSit()
                && !this.isSleeping() && this.isAllowedToTriggerFlight() && this.random.nextInt(TAKEOFF_CHANCE) == 0) {
            this.takeOff();
        }
        if (this.eclipseCooldown > 0) {
            this.eclipseCooldown--;
            return;
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.getDragonStage() >= ECLIPSE_MIN_STAGE
                && this.distanceTo(target) <= ECLIPSE_RANGE && this.hasLineOfSight(target)) {
            // On the ground under it: a target she's carrying, or one in the air, would leave it floating, holding nothing.
            Vec3 ground = Utils.moveToRelativeGroundLevel(this.level(), target.position(), 16);
            this.level().addFreshEntity(SovereignsEclipse.create(this.level(), this, ground));
            this.playSound(this.getRoarSound(), 3.0F, 0.7F);
            this.eclipseCooldown = ECLIPSE_COOLDOWN;
        }
    }

    /** Up, as Ice and Fire takes a dragon off: hovering first, then flying. */
    private void takeOff() {
        this.setHovering(true);
        this.setInSittingPose(false);
        this.flyHovering = 0;
        this.hoverTicks = 0;
        this.flyTicks = 0;
    }

    /**
     * Her air attacks weighted toward the strafing fly-by (Ice and Fire's Scorch Stream, 50%) and
     * Shatter-Spine (its Hover Blast, the only air attack that charges, 35%), over the dive (Tackle,
     * 15%); Ice and Fire picks each a third of the time. Ground attacks stay its own.
     */
    @Override
    public void randomizeAttacks() {
        super.randomizeAttacks();
        int roll = this.getRandom().nextInt(20);
        this.airAttack = roll < 10 ? IafDragonAttacks.Air.SCORCH_STREAM : roll < 17 ? IafDragonAttacks.Air.HOVER_BLAST : IafDragonAttacks.Air.TACKLE;
    }

    /**
     * Her breath, a cone from her head where Ice and Fire's is a thin stream: each tick she breathes,
     * particles spread through it, and everyone she may harm and can see in it, up to where it lands,
     * or within reach of where it lands, takes a breath hit ({@link #breathHit}). It grows out over
     * Ice and Fire's 40 ticks of breathing, as its stream does.
     */
    @Override
    protected void performNormalBreathAttack(double x, double y, double z) {
        this.getNavigation().stop();
        this.burnParticleX = x;
        this.burnParticleY = y;
        this.burnParticleZ = z;
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 head = this.getHeadPosition();
        Vec3 aim = new Vec3(x, y, z).subtract(head);
        if (aim.lengthSqr() < 0.25) {
            return;
        }
        Vec3 axis = aim.normalize();
        double length = DragonBreath.coneLength(aim.length(), this.burnProgress);
        for (int i = 0; i < BREATH_PARTICLES; i++) {
            Vec3 v = this.spread(axis);
            level.sendParticles(this.createBreathParticle(), head.x, head.y, head.z, 0, v.x, v.y, v.z, 0.9 + this.random.nextDouble() * 0.6);
        }
        Vec3 end = head.add(axis.scale(length));
        BlockHitResult block = level.clip(new ClipContext(head, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 lands = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        double reach = head.distanceTo(lands);
        int stage = this.getDragonStage();
        double radius = DragonBreath.breathRadius(stage);
        float damage = DragonBreath.breathDamage(stage, attackDamageIce());
        DamageSource source = ModRegistries.damageSource(level, ModRegistries.WITHERING_BREATH_DAMAGE, this, this);
        AABB box = new AABB(head, lands).inflate(reach * Math.tan(DragonBreath.CONE_HALF_ANGLE) + radius);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive() && !this.hasPassenger(e) && RimefireCrown.canHarm(this, e))) {
            Vec3 at = victim.getBoundingBox().getCenter().subtract(head);
            boolean inCone = DragonBreath.inCone(at.x, at.y, at.z, axis.x, axis.y, axis.z, reach, victim.getBbWidth() / 2.0);
            boolean splashed = victim.getBoundingBox().distanceToSqr(lands) <= radius * radius;
            if ((inCone || splashed) && this.hasLineOfSight(victim)) {
                this.breathHit(victim, source, damage);
            }
        }
        if (this.tickCount % 4 == 0) {
            BlockPos center = BlockPos.containing(lands);
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
                RimefireCrown.drinkFire(level, pos);
            }
        }
    }

    /** A direction within the breath cone round {@code axis}, spread evenly over it. */
    private Vec3 spread(Vec3 axis) {
        Vec3 side = Math.abs(axis.y) < 0.9 ? axis.cross(new Vec3(0, 1, 0)).normalize() : axis.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 up = axis.cross(side);
        double cosTheta = 1.0 - this.random.nextDouble() * (1.0 - Math.cos(DragonBreath.CONE_HALF_ANGLE));
        double sinTheta = Math.sqrt(1.0 - cosTheta * cosTheta);
        double phi = this.random.nextDouble() * Math.PI * 2.0;
        return axis.scale(cosTheta).add(side.scale(Math.cos(phi) * sinTheta)).add(up.scale(Math.sin(phi) * sinTheta));
    }

    /** One breath hit: her breath damage (and its siphon, in {@code SyndrigossaEvents}), then Withering Rime. */
    private void breathHit(LivingEntity victim, DamageSource source, float damage) {
        if (victim.hurt(source, damage) && victim.isAlive()) {
            victim.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, DragonBreath.RIME_TICKS), this);
        }
    }

    /** Ice and Fire's ice breath multiplier ({@code dragon.attackDamageIce}; 2.1 in the owner's pack). */
    public static double attackDamageIce() {
        return IafCommonConfig.INSTANCE.dragon.attackDamageIce.getValue();
    }

    /**
     * Her breath's splash where Ice and Fire would have frozen a spot (its {@code DragonFireDamageWorldEvent},
     * now only from what still reaches Ice and Fire's breath code: her own breath is {@link #performNormalBreathAttack}):
     * everyone she may harm and can see within reach takes an ice dragon's breath hit of her stage
     * and Withering Rime (each hit also siphons {@link DragonBreath#SIPHON} into her, in
     * {@code SyndrigossaEvents}), and fire there is drunk. Nothing freezes.
     */
    void witheringBreath(Vec3 at) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        int stage = this.getDragonStage();
        double radius = DragonBreath.breathRadius(stage);
        float damage = DragonBreath.breathDamage(stage, attackDamageIce());
        DamageSource source = ModRegistries.damageSource(level, ModRegistries.WITHERING_BREATH_DAMAGE, this, this);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                e -> e != this && e.isAlive() && !this.hasPassenger(e) && RimefireCrown.canHarm(this, e) && this.hasLineOfSight(e))) {
            this.breathHit(victim, source, damage);
        }
        BlockPos center = BlockPos.containing(at);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
            RimefireCrown.drinkFire(level, pos);
        }
    }

    /** Cold dread fire, soot and a little snow, in place of the ice dragon's frost: our own particles, several times vanilla's size. */
    @Override
    public ParticleOptions createBreathParticle() {
        int pick = this.random.nextInt(10);
        return pick < 6 ? ModRegistries.RIME_FLAME.get() : pick < 9 ? ModRegistries.RIME_SMOKE.get() : ParticleTypes.SNOWFLAKE;
    }

    /**
     * Her animations in three layers where Ice and Fire's dragons have one. Its attack animations
     * move only the head, neck, jaw, claws and wings (held spread), so with one layer, attacking in
     * the air froze her body, tail and legs in the model's rest pose (the owner saw her "posed still in
     * a sitting idle animation except the mouth and head"), and Ice and Fire played the hover-in-place
     * breath even on a fly-by. So: the fly-by breath or charge under her body (the body's flight wins
     * where both move a bone, so her wings beat and only the jaw and claws take the breath's pose), her
     * body (Ice and Fire's choice of flight, swimming, ground, walking, sitting, sleeping, dead), and
     * over it the hovering or standing breath and charge and her other actions (bite, shake, wing blast,
     * roars, tail), as Ice and Fire plays them.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "flyby_attack", 2, this::flybyAttack));
        controllers.add(new AnimationController<>(this, "body", 2, this::body));
        controllers.add(new AnimationController<>(this, "attack", 2, this::attack));
    }

    private boolean airborne() {
        return this.isFlying() || this.isHovering() || this.flyProgress > 0.0F || this.hoverProgress > 0.0F;
    }

    /** Flying past, not hovering in place. */
    private boolean onFlyby() {
        return this.isFlying() && !this.isHovering();
    }

    private PlayState flybyAttack(AnimationState<Syndrigossa> state) {
        if (!this.isModelDead() && this.onFlyby()) {
            if (this.getAnimation() == ANIMATION_FIRECHARGE) {
                return once(state, "attack_stream_charge");
            }
            if (this.isBreathingFire()) {
                return loop(state, "attack_stream_breath", 1.0F);
            }
        }
        return PlayState.STOP;
    }

    private PlayState body(AnimationState<Syndrigossa> state) {
        if (this.isModelDead()) {
            return loop(state, "dead", 1.0F);
        }
        if ((this.isSwimming || this.isInWater()) && this.swimProgress > 0.0F) {
            return loop(state, "swim", 1.0F);
        }
        if (this.airborne()) {
            return loop(state, "flight", 0.4F);
        }
        if (this.isSleeping() || this.sleepProgress > 0.0F) {
            return loop(state, "sleeping", 1.0F);
        }
        if (this.isOrderedToSit()) {
            return loop(state, "sitting", 1.0F);
        }
        return state.isMoving() ? loop(state, "walk", 0.4F) : loop(state, "ground", 1.0F);
    }

    private PlayState attack(AnimationState<Syndrigossa> state) {
        if (this.isModelDead() || this.isSleeping() || this.onFlyby()) {
            return PlayState.STOP;
        }
        Animation action = this.getAnimation();
        if (action == ANIMATION_FIRECHARGE) {
            return once(state, "attack_blast_charge");
        }
        if (this.isBreathingFire()) {
            return loop(state, "attack_blast_breath", 1.0F);
        }
        String name = this.airborne() ? null : groundAction(action);
        return name != null ? once(state, name) : PlayState.STOP;
    }

    /** Ice and Fire's names for its dragons' one-off ground animations. */
    @Nullable
    private static String groundAction(Animation action) {
        if (action == ANIMATION_BITE || action == ANIMATION_EAT || action == ANIMATION_SPEAK) {
            return "bite";
        }
        if (action == ANIMATION_SHAKEPREY) {
            return "grab_shake";
        }
        if (action == ANIMATION_WINGBLAST) {
            return "wing_blast";
        }
        if (action == ANIMATION_ROAR) {
            return "roar";
        }
        if (action == ANIMATION_EPIC_ROAR) {
            return "epic_roar";
        }
        return action == ANIMATION_TAILWHACK ? "tail" : null;
    }

    private static PlayState loop(AnimationState<Syndrigossa> state, String name, float speed) {
        state.setControllerSpeed(speed);
        return state.setAndContinue(RawAnimation.begin().thenLoop("animation.icedragon." + name));
    }

    private static PlayState once(AnimationState<Syndrigossa> state, String name) {
        state.setControllerSpeed(1.0F);
        return state.setAndContinue(RawAnimation.begin().thenPlay("animation.icedragon." + name));
    }

    /** Her charge (Ice and Fire's charge attack, hers or a rider's) is Shatter-Spine, bursting over her target. */
    @Override
    public Entity createCharge(double x, double y, double z) {
        return ShatterSpineCore.launch(this.level(), this, this.getHeadPosition(), new Vec3(x, y, z), ShatterSpineCore.aimOver(this.getTarget()));
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof IDreadMob || super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof IDreadMob) && super.canAttack(target);
    }

    @Override
    protected boolean shouldTarget(Entity entity) {
        return !(entity instanceof IDreadMob) && super.shouldTarget(entity);
    }
}
