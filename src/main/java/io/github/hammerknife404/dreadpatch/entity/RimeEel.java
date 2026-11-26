package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.effect.RimefrostEffect;
import io.github.hammerknife404.dreadpatch.magic.RimeEelRules;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * The Rime Eel (0.71.0, the owner's spec; numbers in {@link RimeEelRules}): a serpentine dread anglerfish of the
 * Dreadlands' dark water, 0.6 to 2.5 blocks across, now and then an alpha or matron to 4 (its {@code generic.scale},
 * rolled as it spawns), its health and bite growing with it, 4 armour. Of the Dread ({@link IDreadMob}: Ice and Fire's dread mobs take it for one of theirs, and
 * the Dread's rime and storms don't touch it).
 * <ul>
 * <li>Amphibious, as an axolotl moves: swift in water, a slither on land, up single blocks; it never drowns, chokes
 * in a wall or dries out.</li>
 * <li>In a fight its lure pulses every 3-5 s ({@link #pulse}): Rimefrost or Whiteout on everything not of the Dread
 * within 4 blocks, and if that catches anything, a Frenzy that halves the time between its bites. A bite may leave
 * Withering Rime. On land it lunges, then waits 2 s.</li>
 * <li>Out of water, seeking water and chasing prey have the same priority: whichever it takes up first it keeps
 * to. With nothing to fight it eats what's dropped ({@link DreadFeeding}: since 0.83.0 most organic things, only food healing), one item at a
 * time, healing 0.25-1 each.</li>
 * </ul>
 * Raised from a water creature in her storm ({@link RisenDread#rise}) it takes its raiser's target, as the risen
 * do; it stirs on its own in the Dreadlands' dark water ({@code world/RisenSpawns}). Its lure's flicker and flare
 * are drawn by {@code client/RimeEelRenderer}.
 */
public class RimeEel extends Monster implements IDreadMob {
    /** To its clients: it has pulsed (its lure flares). */
    public static final byte PULSE_EVENT = 101;

    private int auraCooldown = RimeEelRules.AURA_MIN_TICKS;
    private int frenzy;
    private int lungeCooldown;
    /** On a client: the tick it last pulsed. */
    private int flaredAt = -1000;
    /** Whether it has its lantern (0.76.0): most don't. Saved as {@code Lantern}; an eel from before had one. */
    private static final EntityDataAccessor<Boolean> LANTERN = SynchedEntityData.defineId(RimeEel.class, EntityDataSerializers.BOOLEAN);
    /** An alpha rearing back to bite (0.80.0), for its clients to draw. */
    private static final EntityDataAccessor<Boolean> WINDING = SynchedEntityData.defineId(RimeEel.class, EntityDataSerializers.BOOLEAN);
    /** How far it has reared back (0 to 1), this tick and last; on a client. */
    private float rear;
    private float rearO;
    /** Ticks left of an alpha's wind-up; 0 when it isn't winding. */
    int windup;
    /** How far its lure reaches out (0 drawn in, 1 out), this tick and last; on both sides, for {@link #lantern}. */
    private float reach;
    private float reachO;

    public RimeEel(EntityType<? extends RimeEel> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, RimeEelRules.WATER_SPEED, RimeEelRules.LAND_SPEED, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LANTERN, true);
        builder.define(WINDING, false);
    }

    public boolean hasLantern() {
        return this.entityData.get(LANTERN);
    }

    public void setLantern(boolean lantern) {
        this.entityData.set(LANTERN, lantern);
    }

    /** Its lure's reach, between last tick's and this one's. */
    public float reach(float partialTick) {
        return Mth.lerp(partialTick, this.reachO, this.reach);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                // A middling one's, until it's sized as it spawns (a summon with data isn't).
                .add(Attributes.SCALE, RimeEelRules.scale(0.5))
                .add(Attributes.MAX_HEALTH, RimeEelRules.health(RimeEelRules.scale(0.5)))
                .add(Attributes.ARMOR, RimeEelRules.ARMOR)
                .add(Attributes.ATTACK_DAMAGE, RimeEelRules.bite(RimeEelRules.scale(0.5)))
                .add(Attributes.MOVEMENT_SPEED, RimeEelRules.SPEED)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new BiteGoal(this));
        this.goalSelector.addGoal(2, new SeekWaterGoal(this));
        this.goalSelector.addGoal(3, new DreadFeeding.EatDroppedGoal(this, RimeEelRules.FOOD_SEARCH));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0, 30));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                return !this.mob.isInWater() && super.canUse();
            }
        });
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new RisenDread.RaiserTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, RimeEel::prey));
    }

    /** What it hunts: players and mobs not of the Dread; not dragons, bosses or armour stands. */
    static boolean prey(LivingEntity entity) {
        return (entity instanceof Player || entity instanceof Mob) && !(entity instanceof ArmorStand) && !RisenDread.dread(entity)
                && !(entity instanceof DragonBaseEntity) && !entity.getType().is(Tags.EntityTypes.BOSSES);
    }

    /** Its size, rolled as it spawns: its scale, and its health and bite with it. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        this.setSize(RimeEelRules.scale(this.random.nextDouble(), this.random.nextInt(RimeEelRules.ALPHA_ONE_IN)));
        this.setLantern(RimeEelRules.spawnsWithLantern(this.random.nextInt(RimeEelRules.LANTERN_ONE_IN)));
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    public void setSize(double scale) {
        set(this.getAttribute(Attributes.SCALE), scale);
        set(this.getAttribute(Attributes.MAX_HEALTH), RimeEelRules.health(scale));
        this.build(scale);
        this.setHealth(this.getMaxHealth());
    }

    /** Its bite, and an alpha's hide, weight and knockback (0.80.0), for its size; health aside. */
    private void build(double scale) {
        boolean alpha = RimeEelRules.alpha(scale);
        set(this.getAttribute(Attributes.ATTACK_DAMAGE), RimeEelRules.fullBite(scale));
        // An alpha's heavy hide and weight, and its knockback (0.80.0).
        set(this.getAttribute(Attributes.ARMOR), alpha ? RimeEelRules.ALPHA_ARMOR : RimeEelRules.ARMOR);
        set(this.getAttribute(Attributes.ARMOR_TOUGHNESS), alpha ? RimeEelRules.ALPHA_TOUGHNESS : 0.0);
        set(this.getAttribute(Attributes.KNOCKBACK_RESISTANCE), alpha ? RimeEelRules.ALPHA_KNOCKBACK_RESISTANCE : 0.0);
        set(this.getAttribute(Attributes.ATTACK_KNOCKBACK), alpha ? RimeEelRules.ALPHA_KNOCKBACK : 0.0);
    }

    /** Whether it's an alpha or matron (by its size). */
    public boolean isAlpha() {
        return RimeEelRules.alpha(this.getScale());
    }

    /** An alpha or matron is a Rime Wyrm by name (0.80.0, the owner: "Yes", to naming them so); its id stays the eel's. */
    @Override
    protected Component getTypeName() {
        return this.isAlpha() ? Component.translatable("entity.dreadpatch.rime_wyrm") : super.getTypeName();
    }

    /** Whether it's rearing back to bite (an alpha's wind-up), and how far, this tick and last, for drawing. */
    public boolean winding() {
        return this.entityData.get(WINDING);
    }

    void setWinding(boolean winding) {
        this.entityData.set(WINDING, winding);
    }

    public float rear(float partialTick) {
        return Mth.lerp(partialTick, this.rearO, this.rear);
    }

    private static void set(@Nullable AttributeInstance attribute, double value) {
        if (attribute != null) {
            attribute.setBaseValue(value);
        }
    }

    /** Of the Dread: never at odds with them. */
    @Override
    public boolean isAlliedTo(Entity entity) {
        return entity instanceof IDreadMob || super.isAlliedTo(entity);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !RisenDread.dread(target) && super.canAttack(target);
    }

    @Override
    public @Nullable Entity getCommander() {
        return RisenDread.raiser(this);
    }

    // It breathes under water by #minecraft:can_breathe_under_water (vanilla's check is final).

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    /** No drowning, and no choking in a wall. */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL) || super.isInvulnerableTo(source);
    }

    /** As an axolotl swims: its own push in water, with water's drag. */
    @Override
    public void travel(Vec3 input) {
        if (this.isControlledByLocalInstance() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        } else {
            super.travel(input);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Its lure reaches out while it hunts or swims about, and draws in while it's still.
        this.reachO = this.reach;
        this.reach = RimeEelRules.reachToward(this.reach, this.isAggressive() || this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4);
        this.rearO = this.rear;
        this.rear = this.winding() ? Math.min(1.0F, this.rear + 1.0F / 6.0F) : Math.max(0.0F, this.rear - 0.34F);
        if (this.level().isClientSide()) {
            if (this.lureLit() && this.random.nextFloat() < 0.35F) {
                Vec3 lantern = this.lantern();
                this.level().addParticle(this.random.nextInt(3) == 0 ? ParticleTypes.SNOWFLAKE : ParticleTypes.GLOW,
                        lantern.x + (this.random.nextDouble() - 0.5) * 0.5, lantern.y + (this.random.nextDouble() - 0.5) * 0.5,
                        lantern.z + (this.random.nextDouble() - 0.5) * 0.5, 0.0, 0.01, 0.0);
            }
            return;
        }
        if (this.frenzy > 0) {
            this.frenzy--;
        }
        if (this.lungeCooldown > 0) {
            this.lungeCooldown--;
        }
        LivingEntity target = this.getTarget();
        boolean fighting = target != null && target.isAlive() && prey(target);
        if (!fighting) {
            this.auraCooldown = Math.max(this.auraCooldown, RimeEelRules.AURA_MIN_TICKS / 2);
            return;
        }
        // A Glowing target sends it into a Frenzy (0.76.0): a lantern eel's pulse lights its prey for the rest.
        if (target.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)) {
            this.frenzy = Math.max(this.frenzy, RimeEelRules.GLOW_FRENZY_TICKS);
        }
        // Only its lantern pulses (0.76.0).
        if (this.hasLantern() && --this.auraCooldown <= 0) {
            this.auraCooldown = RimeEelRules.auraTicks(this.random.nextDouble());
            this.pulse();
        }
        if (RimeEelRules.lunges(this.isInWater(), this.onGround(), this.distanceTo(target), this.getSensing().hasLineOfSight(target), this.lungeCooldown)) {
            this.lungeCooldown = this.isAlpha() ? RimeEelRules.ALPHA_LUNGE_TICKS : RimeEelRules.LUNGE_TICKS;
            Vec3 toward = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize();
            this.setDeltaMovement(toward.x * RimeEelRules.LUNGE_SPEED, RimeEelRules.LUNGE_LIFT, toward.z * RimeEelRules.LUNGE_SPEED);
            this.playSound(ModRegistries.RIME_EEL_LUNGE.get(), 1.0F, this.getVoicePitch());
        }
    }

    /**
     * Its lure's pulse: Rimefrost (heavy) or Whiteout, by a coin, on each living thing not of the Dread within
     * {@link RimeEelRules#AURA_RADIUS} (players in creative or spectating aside); if it takes on anything, a Frenzy.
     * And (0.75.1) Glowing on every living thing in it but Rime Eels, the Dread too.
     */
    void pulse() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        double radius = RimeEelRules.AURA_RADIUS;
        boolean caught = false;
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
                entity -> entity != this && entity.isAlive() && !RisenDread.dread(entity) && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity)
                        && entity.distanceToSqr(this) <= radius * radius);
        for (LivingEntity entity : near) {
            MobEffectInstance effect = this.random.nextBoolean()
                    ? new MobEffectInstance(ModRegistries.RIMEFROST, RimeEelRules.AURA_EFFECT_TICKS, RimefrostEffect.HEAVY)
                    : new MobEffectInstance(ModRegistries.WHITEOUT, RimeEelRules.AURA_EFFECT_TICKS);
            caught |= entity.addEffect(effect, this);
        }
        if (caught) {
            this.frenzy = RimeEelRules.FRENZY_TICKS;
        }
        // Its lure lights up everything it reaches (0.75.1, the owner): Glowing on every other living thing in
        // the pulse, the Dread too, but no Rime Eel.
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
                entity -> !(entity instanceof RimeEel) && entity.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity)
                        && entity.distanceToSqr(this) <= radius * radius)) {
            entity.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, RimeEelRules.AURA_EFFECT_TICKS), this);
        }
        level.broadcastEntityEvent(this, PULSE_EVENT);
        Vec3 lure = this.lantern();
        level.sendParticles(ParticleTypes.GLOW, lure.x, lure.y, lure.z, 6, 0.1, 0.1, 0.1, 0.02);
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI * 2.0 / 24;
            level.sendParticles(ParticleTypes.SNOWFLAKE, this.getX() + Math.cos(angle) * radius * 0.6, lure.y - 0.2, this.getZ() + Math.sin(angle) * radius * 0.6,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        this.playSound(ModRegistries.RIME_EEL_PULSE.get(), 1.0F, this.getVoicePitch());
    }

    public boolean frenzied() {
        return this.frenzy > 0;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == PULSE_EVENT) {
            this.flaredAt = this.tickCount;
        } else {
            super.handleEntityEvent(id);
        }
    }

    /**
     * Where its lantern's glass hangs: between {@link RimeEelRules#LANTERN_IN} and {@code LANTERN_OUT} by its reach (model
     * pixels from its feet, client/RimeEelModel), drawn as vanilla draws a living thing's model (turned by its body) and
     * scaled with it.
     */
    public Vec3 lantern() {
        double scale = this.getScale() / 16.0;
        double[] in = RimeEelRules.LANTERN_IN;
        double[] out = RimeEelRules.LANTERN_OUT;
        double x = Mth.lerp(this.reach, in[0], out[0]) * scale;
        double y = Mth.lerp(this.reach, in[1], out[1]) * scale;
        double z = Mth.lerp(this.reach, in[2], out[2]) * scale;
        double turn = Math.toRadians(180.0F - this.yBodyRot);
        double wx = x * Math.cos(turn) + z * Math.sin(turn);
        double wz = -x * Math.sin(turn) + z * Math.cos(turn);
        return this.position().add(wx, y, wz);
    }

    /** On a client: whether its lure burns now (its flicker, or just after a pulse). */
    public boolean lureLit() {
        return this.hasLantern() && (this.tickCount - this.flaredAt < RimeEelRules.FLARE_TICKS
                || RimeEelRules.lureLit(this.getUUID().hashCode(), this.tickCount));
    }

    /** A bite: its own damage, then a chance of Withering Rime (laid by it, so the queen's siphon passes it by). */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.playSound(ModRegistries.RIME_EEL_BITE.get(), 1.0F, this.getVoicePitch());
            if (target instanceof LivingEntity living && this.random.nextFloat() < RimeEelRules.WITHERING_CHANCE) {
                living.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, RimeEelRules.WITHERING_TICKS), this);
            }
        }
        return hit;
    }

    /** Whether it eats {@code stack}: its extras (ink), or any food it doesn't refuse. */
    /** What it eats: the Dread's list since 0.83.0 ({@link DreadFeeding#eats}). */
    public static boolean eats(ItemStack stack) {
        return DreadFeeding.eats(stack);
    }

    void eat(ItemEntity item) {
        DreadFeeding.eat(this, item);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Frenzy", this.frenzy);
        tag.putBoolean("Lantern", this.hasLantern());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.frenzy = tag.getInt("Frenzy");
        this.setLantern(!tag.contains("Lantern") || tag.getBoolean("Lantern"));
        // An alpha saved before 0.80.0 kept its old bite and hide: its build for its size again.
        this.build(this.getScale());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? ModRegistries.RIME_EEL_AMBIENT.get() : ModRegistries.RIME_EEL_FLOP.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModRegistries.RIME_EEL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModRegistries.RIME_EEL_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    /** Smaller ones higher. */
    @Override
    public float getVoicePitch() {
        AttributeInstance scale = this.getAttribute(Attributes.SCALE);
        double size = scale == null ? 0.5 : scale.getBaseValue();
        return super.getVoicePitch() * (float) Math.max(0.6, 1.4 - 0.6 * size);
    }

    /** Swims and walks at its target and bites: every second, twice as often in a Frenzy. */
    static final class BiteGoal extends MeleeAttackGoal {
        private final RimeEel eel;
        private int lastBite = -1000;

        BiteGoal(RimeEel eel) {
            super(eel, 1.0, true);
            this.eel = eel;
        }

        @Override
        protected void resetAttackCooldown() {
            super.resetAttackCooldown();
            this.lastBite = this.eel.tickCount;
        }

        @Override
        protected boolean isTimeToAttack() {
            boolean alpha = this.eel.isAlpha();
            int wait = this.eel.frenzied() ? (alpha ? RimeEelRules.ALPHA_FRENZY_BITE_TICKS : RimeEelRules.FRENZY_BITE_TICKS)
                    : (alpha ? RimeEelRules.ALPHA_BITE_TICKS : RimeEelRules.BITE_TICKS);
            return this.eel.tickCount - this.lastBite >= wait;
        }

        /**
         * An alpha winds up before it bites (0.80.0): in reach and ready, it stops, rears back and gapes, with a roar;
         * {@link RimeEelRules#ALPHA_WINDUP_TICKS} later it snaps, hitting if its target's still in reach. The rest bite
         * as vanilla's melee does.
         */
        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (!this.eel.isAlpha()) {
                super.checkAndPerformAttack(target);
                return;
            }
            if (this.eel.windup > 0) {
                this.eel.getNavigation().stop();
                this.eel.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (--this.eel.windup == 0) {
                    this.eel.setWinding(false);
                    this.eel.swing(InteractionHand.MAIN_HAND);
                    if (this.eel.isWithinMeleeAttackRange(target)) {
                        this.eel.doHurtTarget(target);
                    }
                    this.resetAttackCooldown();
                }
                return;
            }
            if (this.canPerformAttack(target)) {
                this.eel.windup = RimeEelRules.ALPHA_WINDUP_TICKS;
                this.eel.setWinding(true);
                this.eel.getNavigation().stop();
                this.eel.playSound(SoundEvents.RAVAGER_ROAR, 0.7F, 1.5F);
            }
        }

        @Override
        public void stop() {
            super.stop();
            this.eel.windup = 0;
            this.eel.setWinding(false);
        }

        @Override
        public boolean canUse() {
            return super.canUse() && prey(this.eel.getTarget());
        }
    }

    /**
     * Out of water: to the nearest water within reach with room for it (a big one doesn't fit every pool). As high a
     * priority as a fight; whichever starts, it keeps to.
     */
    static final class SeekWaterGoal extends Goal {
        private final RimeEel eel;
        private @Nullable BlockPos water;

        SeekWaterGoal(RimeEel eel) {
            this.eel = eel;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // Now and then, not every check (vanilla checks a goal every other tick, so a tick count's step could miss them all).
            if (this.eel.isInWater() || this.eel.getRandom().nextInt(5) != 0) {
                return false;
            }
            Level level = this.eel.level();
            Optional<BlockPos> found = BlockPos.findClosestMatch(this.eel.blockPosition(), RimeEelRules.WATER_SEARCH, RimeEelRules.WATER_SEARCH_DEPTH,
                    pos -> level.getFluidState(pos).is(FluidTags.WATER) && level.noCollision(this.eel,
                            this.eel.getDimensions(this.eel.getPose()).makeBoundingBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)));
            this.water = found.orElse(null);
            return this.water != null;
        }

        private int timer;

        @Override
        public boolean canContinueToUse() {
            return this.water != null && !this.eel.isInWater() && this.timer < 200;
        }

        @Override
        public void start() {
            this.timer = 0;
            if (this.water != null) {
                this.eel.getNavigation().moveTo(this.water.getX() + 0.5, this.water.getY(), this.water.getZ() + 0.5, 1.0);
            }
        }

        /**
         * No path (a big one, a small pool: a path needs room for its box): it slithers straight for it. (Vanilla's
         * swimming move control drops a wanted spot while the navigation's done, so it's pushed itself.)
         */
        @Override
        public void tick() {
            this.timer++;
            if (this.water == null || !this.eel.getNavigation().isDone()) {
                return;
            }
            Vec3 to = new Vec3(this.water.getX() + 0.5 - this.eel.getX(), 0.0, this.water.getZ() + 0.5 - this.eel.getZ());
            if (to.lengthSqr() < 1.0E-4) {
                return;
            }
            Vec3 way = to.normalize();
            this.eel.setYRot((float) (Mth.atan2(way.z, way.x) * Mth.RAD_TO_DEG) - 90.0F);
            this.eel.yBodyRot = this.eel.getYRot();
            Vec3 motion = this.eel.getDeltaMovement();
            double push = RimeEelRules.LAND_SPEED * 0.5;
            this.eel.setDeltaMovement(way.x * push, motion.y, way.z * push);
        }

        @Override
        public void stop() {
            this.water = null;
            this.eel.getNavigation().stop();
        }
    }

    /** With nothing to fight: to the nearest food lying near, and eats it, an item at a time. */
}
