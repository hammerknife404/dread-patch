package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.effect.RimefrostEffect;
import io.github.hammerknife404.dreadpatch.magic.DreadFishRules;
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
 * The Dread Fish (0.71.0, the owner's spec; numbers in {@link DreadFishRules}): a serpentine dread anglerfish of the
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
 * are drawn by {@code client/DreadFishRenderer}.
 */
public class DreadFish extends Monster implements IDreadMob {
    /** To its clients: it has pulsed (its lure flares). */
    public static final byte PULSE_EVENT = 101;

    private int auraCooldown = DreadFishRules.AURA_MIN_TICKS;
    private int frenzy;
    private int lungeCooldown;
    /** On a client: the tick it last pulsed. */
    private int flaredAt = -1000;
    /** Whether it has its lantern (0.76.0): most don't. Saved as {@code Lantern}; an fish from before had one. */
    private static final EntityDataAccessor<Boolean> LANTERN = SynchedEntityData.defineId(DreadFish.class, EntityDataSerializers.BOOLEAN);
    /** An alpha rearing back to bite (0.80.0), for its clients to draw. */
    private static final EntityDataAccessor<Boolean> WINDING = SynchedEntityData.defineId(DreadFish.class, EntityDataSerializers.BOOLEAN);
    /** How far it has reared back (0 to 1), this tick and last; on a client. */
    private float rear;
    private float rearO;
    /** Ticks left of an alpha's wind-up; 0 when it isn't winding. */
    int windup;
    /** How far its lure reaches out (0 drawn in, 1 out), this tick and last; on both sides, for {@link #lantern}. */
    private float reach;
    private float reachO;
    /** Whether its bite goal is running (0.93.0): only then does it lunge on land. */
    boolean hunting;
    /** How straight its body is (0 curled, 1 straight; {@link DreadFishRules#straightToward}), this tick and last; for drawing. */
    private float straight;
    private float straightO;

    public DreadFish(EntityType<? extends DreadFish> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, DreadFishRules.WATER_SPEED, DreadFishRules.LAND_SPEED, true);
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

    /** How straight its body is drawn, between last tick's and this one's. */
    public float straight(float partialTick) {
        return Mth.lerp(partialTick, this.straightO, this.straight);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                // A middling one's, until it's sized as it spawns (a summon with data isn't).
                .add(Attributes.SCALE, DreadFishRules.scale(0.5))
                .add(Attributes.MAX_HEALTH, DreadFishRules.health(DreadFishRules.scale(0.5)))
                .add(Attributes.ARMOR, DreadFishRules.ARMOR)
                .add(Attributes.ATTACK_DAMAGE, DreadFishRules.bite(DreadFishRules.scale(0.5)))
                .add(Attributes.MOVEMENT_SPEED, DreadFishRules.SPEED)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LeapGoal(this));
        this.goalSelector.addGoal(2, new BiteGoal(this));
        this.goalSelector.addGoal(2, new SeekWaterGoal(this));
        this.goalSelector.addGoal(3, new DreadFeeding.EatDroppedGoal(this, DreadFishRules.FOOD_SEARCH));
        // As a dolphin (0.90.1): swimming about at its pace, and breaching near the surface.
        this.goalSelector.addGoal(4, new BreachGoal(this));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, DreadFishRules.CRUISE_SPEED, DreadFishRules.SWIM_INTERVAL));
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
        // A Glowing target first, from twice as far (0.86.0); then any prey.
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                entity -> prey(entity) && entity.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)) {
            @Override
            protected double getFollowDistance() {
                return super.getFollowDistance() * DreadFishRules.GLOW_RANGE_FACTOR;
            }
        });
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, DreadFish::prey));
    }

    /** What it hunts: players and mobs not of the Dread; not dragons, bosses or armour stands. */
    static boolean prey(LivingEntity entity) {
        return (entity instanceof Player || entity instanceof Mob) && !(entity instanceof ArmorStand) && !RisenDread.dread(entity)
                && !(entity instanceof DragonBaseEntity) && !entity.getType().is(Tags.EntityTypes.BOSSES);
    }

    /** Its size, rolled as it spawns: its scale, and its health and bite with it. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        this.setSize(DreadFishRules.scale(this.random.nextDouble(), this.random.nextInt(DreadFishRules.ALPHA_ONE_IN)));
        this.setLantern(DreadFishRules.spawnsWithLantern(this.random.nextInt(DreadFishRules.LANTERN_ONE_IN)));
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    public void setSize(double scale) {
        set(this.getAttribute(Attributes.SCALE), scale);
        set(this.getAttribute(Attributes.MAX_HEALTH), DreadFishRules.health(scale));
        this.build(scale);
        this.setHealth(this.getMaxHealth());
    }

    /** Its bite, and an alpha's hide, weight and knockback (0.80.0), for its size; health aside. */
    private void build(double scale) {
        boolean alpha = DreadFishRules.alpha(scale);
        set(this.getAttribute(Attributes.ATTACK_DAMAGE), DreadFishRules.fullBite(scale));
        // An alpha's heavy hide and weight, and its knockback (0.80.0).
        set(this.getAttribute(Attributes.ARMOR), alpha ? DreadFishRules.ALPHA_ARMOR : DreadFishRules.ARMOR);
        set(this.getAttribute(Attributes.ARMOR_TOUGHNESS), alpha ? DreadFishRules.ALPHA_TOUGHNESS : 0.0);
        set(this.getAttribute(Attributes.KNOCKBACK_RESISTANCE), alpha ? DreadFishRules.ALPHA_KNOCKBACK_RESISTANCE : 0.0);
        set(this.getAttribute(Attributes.ATTACK_KNOCKBACK), alpha ? DreadFishRules.ALPHA_KNOCKBACK : 0.0);
    }

    /** Whether it's an alpha or matron (by its size). */
    public boolean isAlpha() {
        return DreadFishRules.alpha(this.getScale());
    }

    /** An alpha or matron is a Rime Wyrm by name (0.80.0, the owner: "Yes", to naming them so); its id stays the fish's. */
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
        this.reach = DreadFishRules.reachToward(this.reach, this.isAggressive() || this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4);
        // Its body by how fast it really went this tick, up and down too (on a client, where it was drawn).
        this.straightO = this.straight;
        double dx = this.getX() - this.xo;
        double dy = this.getY() - this.yo;
        double dz = this.getZ() - this.zo;
        this.straight = DreadFishRules.straightToward(this.straight, Math.sqrt(dx * dx + dy * dy + dz * dz), !this.isInWater());
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
            this.auraCooldown = Math.max(this.auraCooldown, DreadFishRules.AURA_MIN_TICKS / 2);
            return;
        }
        // A Glowing target sends it into a Frenzy (0.76.0): a lantern fish's pulse lights its prey for the rest.
        if (target.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)) {
            this.frenzy = Math.max(this.frenzy, DreadFishRules.GLOW_FRENZY_TICKS);
        }
        // Only its lantern pulses (0.76.0).
        if (this.hasLantern() && --this.auraCooldown <= 0) {
            this.auraCooldown = DreadFishRules.auraTicks(this.random.nextDouble());
            this.pulse();
        }
        // Only while it's hunting (0.93.0): heading for water instead, its lunges dragged it back each time, so on land near
        // water it shuttled to and fro by its prey without biting (the owner: "they curl up or spazz out still").
        if (this.hunting && DreadFishRules.lunges(this.isInWater(), this.onGround(), this.distanceTo(target), this.getSensing().hasLineOfSight(target), this.lungeCooldown)) {
            this.lungeCooldown = this.isAlpha() ? DreadFishRules.ALPHA_LUNGE_TICKS : DreadFishRules.LUNGE_TICKS;
            // On land too, it leaps as from the water (0.90.1, the owner: "leap like dolphins can at prey ... while on
            // land too"): the same arc, a block over its prey, at most 4.
            Vec3 flat = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
            Vec3 toward = flat.lengthSqr() < 1e-6 ? Vec3.ZERO : flat.normalize();
            double[] launch = DreadFishRules.leap(flat.length(), target.getY() - this.getY() + 1.0);
            this.setDeltaMovement(toward.x * launch[1], launch[0], toward.z * launch[1]);
            this.playSound(ModRegistries.DREAD_FISH_LUNGE.get(), 1.0F, this.getVoicePitch());
        }
    }

    /**
     * Its lure's pulse: Rimefrost (heavy) or Whiteout, by a coin, on each living thing not of the Dread within
     * {@link DreadFishRules#AURA_RADIUS} (players in creative or spectating aside); if it takes on anything, a Frenzy.
     * And (0.75.1) Glowing on every living thing in it but Dread Fish, the Dread too.
     */
    void pulse() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        double radius = DreadFishRules.AURA_RADIUS;
        boolean caught = false;
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
                entity -> entity != this && entity.isAlive() && !RisenDread.dread(entity) && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity)
                        && entity.distanceToSqr(this) <= radius * radius);
        for (LivingEntity entity : near) {
            MobEffectInstance effect = this.random.nextBoolean()
                    ? new MobEffectInstance(ModRegistries.RIMEFROST, DreadFishRules.AURA_EFFECT_TICKS, RimefrostEffect.HEAVY)
                    : new MobEffectInstance(ModRegistries.WHITEOUT, DreadFishRules.AURA_EFFECT_TICKS);
            caught |= entity.addEffect(effect, this);
        }
        if (caught) {
            this.frenzy = DreadFishRules.FRENZY_TICKS;
        }
        // Its lure lights up everything it reaches (0.75.1, the owner): Glowing on every other living thing in
        // the pulse, the Dread too, but no Dread Fish.
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
                entity -> !(entity instanceof DreadFish) && entity.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity)
                        && entity.distanceToSqr(this) <= radius * radius)) {
            entity.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, DreadFishRules.AURA_EFFECT_TICKS), this);
        }
        level.broadcastEntityEvent(this, PULSE_EVENT);
        Vec3 lure = this.lantern();
        level.sendParticles(ParticleTypes.GLOW, lure.x, lure.y, lure.z, 6, 0.1, 0.1, 0.1, 0.02);
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI * 2.0 / 24;
            level.sendParticles(ParticleTypes.SNOWFLAKE, this.getX() + Math.cos(angle) * radius * 0.6, lure.y - 0.2, this.getZ() + Math.sin(angle) * radius * 0.6,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        this.playSound(ModRegistries.DREAD_FISH_PULSE.get(), 1.0F, this.getVoicePitch());
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
     * Where its lantern's glass hangs: between {@link DreadFishRules#LANTERN_IN} and {@code LANTERN_OUT} by its reach (model
     * pixels from its feet, client/DreadFishModel), drawn as vanilla draws a living thing's model (turned by its body) and
     * scaled with it.
     */
    public Vec3 lantern() {
        double scale = this.getScale() / 16.0;
        double[] in = DreadFishRules.LANTERN_IN;
        double[] out = DreadFishRules.LANTERN_OUT;
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
        return this.hasLantern() && (this.tickCount - this.flaredAt < DreadFishRules.FLARE_TICKS
                || DreadFishRules.lureLit(this.getUUID().hashCode(), this.tickCount));
    }

    /** A bite: its own damage, then a chance of Withering Rime (laid by it, so the queen's siphon passes it by). */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.playSound(ModRegistries.DREAD_FISH_BITE.get(), 1.0F, this.getVoicePitch());
            if (target instanceof LivingEntity living && this.random.nextFloat() < DreadFishRules.WITHERING_CHANCE) {
                living.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, DreadFishRules.WITHERING_TICKS), this);
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
        return this.isInWater() ? ModRegistries.DREAD_FISH_AMBIENT.get() : ModRegistries.DREAD_FISH_FLOP.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModRegistries.DREAD_FISH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModRegistries.DREAD_FISH_DEATH.get();
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
    /**
     * A dolphin's breach (0.90.1; vanilla's {@code DolphinJumpGoal}, which takes only a dolphin): swimming near the
     * surface, now and then, with open water ahead and air over it, it leaps out along its way and dives back in.
     */
    static final class BreachGoal extends Goal {
        private final DreadFish fish;
        private boolean out;

        BreachGoal(DreadFish fish) {
            this.fish = fish;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (this.fish.getRandom().nextInt(DreadFishRules.BREACH_INTERVAL) != 0 || !this.fish.isInWater() || this.fish.getTarget() != null) {
                return false;
            }
            Vec3 motion = this.fish.getDeltaMovement().multiply(1.0, 0.0, 1.0);
            if (motion.lengthSqr() < 0.0025) {
                return false;
            }
            Vec3 way = motion.normalize();
            BlockPos at = this.fish.blockPosition();
            BlockPos ahead = BlockPos.containing(this.fish.position().add(way.scale(3.0)));
            return this.fish.level().getFluidState(at.above()).is(net.minecraft.tags.FluidTags.WATER)
                    && this.fish.level().getBlockState(at.above(2)).isAir()
                    && this.fish.level().getFluidState(ahead).is(net.minecraft.tags.FluidTags.WATER)
                    && this.fish.level().getBlockState(ahead.above(2)).isAir();
        }

        @Override
        public void start() {
            Vec3 way = this.fish.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize();
            this.fish.setDeltaMovement(way.x * 0.6, 0.7, way.z * 0.6);
            this.fish.getNavigation().stop();
            this.out = false;
        }

        @Override
        public boolean canContinueToUse() {
            Vec3 motion = this.fish.getDeltaMovement();
            return !(motion.y * motion.y < 0.03 && this.fish.getXRot() != 0.0F && Math.abs(this.fish.getXRot()) < 10.0F && this.fish.isInWater())
                    && !this.fish.onGround();
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public void stop() {
            this.fish.setXRot(0.0F);
        }

        @Override
        public void tick() {
            boolean wet = this.fish.isInWater();
            if (this.out != !wet) {
                this.out = !wet;
                this.fish.playSound(SoundEvents.DOLPHIN_JUMP, 1.0F, 0.7F);
            }
            Vec3 motion = this.fish.getDeltaMovement();
            if (motion.y * motion.y < 0.03 && this.fish.getXRot() != 0.0F) {
                this.fish.setXRot(Mth.rotLerp(0.2F, this.fish.getXRot(), 0.0F));
            } else if (motion.length() > 1e-5) {
                double flat = motion.horizontalDistance();
                this.fish.setXRot((float) (Math.atan2(-motion.y, flat) * Mth.RAD_TO_DEG));
            }
        }
    }

    /**
     * A Sea Serpent's leap (0.86.0): from just under the surface, at its target, or at food it eats lying by the water,
     * within {@link DreadFishRules#inLeapReach}, it leaps out, up to a few blocks, biting (or eating) what it touches on
     * the way; then {@link DreadFishRules#leapCooldown} before the next.
     */
    static final class LeapGoal extends Goal {
        private final DreadFish fish;
        private int ready;
        private @Nullable Entity aim;
        private boolean landed;
        private int airborne;

        LeapGoal(DreadFish fish) {
            this.fish = fish;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        private boolean nearSurface() {
            if (!this.fish.isInWater()) {
                return false;
            }
            BlockPos at = this.fish.blockPosition();
            for (int up = 1; up <= DreadFishRules.LEAP_DEPTH + 1; up++) {
                if (this.fish.level().getBlockState(at.above(up)).isAir()) {
                    return true;
                }
            }
            return false;
        }

        private boolean reachable(Entity entity) {
            double dx = Math.sqrt(Mth.square(entity.getX() - this.fish.getX()) + Mth.square(entity.getZ() - this.fish.getZ()));
            double dy = entity.getY() - this.fish.getY();
            return !entity.isInWater() && DreadFishRules.inLeapReach(dx, dy) && this.fish.getSensing().hasLineOfSight(entity);
        }

        @Override
        public boolean canUse() {
            if (this.fish.tickCount < this.ready || !this.nearSurface()) {
                return false;
            }
            LivingEntity target = this.fish.getTarget();
            if (target != null && target.isAlive() && this.reachable(target)) {
                this.aim = target;
                return true;
            }
            this.aim = this.fish.level().getEntitiesOfClass(ItemEntity.class, this.fish.getBoundingBox().inflate(DreadFishRules.LEAP_REACH),
                            item -> item.isAlive() && !item.hasPickUpDelay() && DreadFeeding.eats(item.getItem()) && this.reachable(item))
                    .stream().findFirst().orElse(null);
            return this.aim != null && this.fish.getRandom().nextInt(4) == 0;
        }

        @Override
        public void start() {
            Entity aim = this.aim;
            if (aim == null) {
                return;
            }
            double dx = aim.getX() - this.fish.getX();
            double dz = aim.getZ() - this.fish.getZ();
            double across = Math.sqrt(dx * dx + dz * dz);
            double[] launch = DreadFishRules.leap(across, aim.getY() - this.fish.getY() + 1.0);
            Vec3 way = across < 1e-3 ? Vec3.ZERO : new Vec3(dx / across, 0.0, dz / across);
            this.fish.setDeltaMovement(way.x * launch[1], launch[0], way.z * launch[1]);
            this.fish.getNavigation().stop();
            this.fish.playSound(SoundEvents.DOLPHIN_JUMP, 1.0F, 0.7F);
            this.landed = false;
            this.airborne = 0;
            this.ready = this.fish.tickCount + DreadFishRules.leapCooldown(this.fish.getRandom().nextDouble());
        }

        @Override
        public boolean canContinueToUse() {
            // Until it's back in the water, or down on land, or a few seconds on at most.
            return this.airborne < 60 && (this.airborne < 3 || !this.fish.isInWater() && !this.fish.onGround());
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        /** Level again as it comes down (0.92.1). */
        @Override
        public void stop() {
            this.fish.setXRot(0.0F);
        }

        @Override
        public void tick() {
            this.airborne++;
            Entity aim = this.aim;
            if (aim != null) {
                this.fish.getLookControl().setLookAt(aim, 30.0F, 30.0F);
            }
            // Nose along its arc, as its breach is (0.92.1): its pitch had stayed whatever it was swimming at.
            Vec3 motion = this.fish.getDeltaMovement();
            if (motion.lengthSqr() > 1.0E-6) {
                this.fish.setXRot(Mth.rotLerp(0.5F, this.fish.getXRot(), (float) (Math.atan2(-motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG)));
            }
            if (this.landed || aim == null || !aim.isAlive() || !this.fish.getBoundingBox().inflate(0.4).intersects(aim.getBoundingBox())) {
                return;
            }
            this.landed = true;
            if (aim instanceof ItemEntity item) {
                DreadFeeding.eat(this.fish, item);
            } else {
                this.fish.swing(InteractionHand.MAIN_HAND);
                this.fish.doHurtTarget(aim);
            }
        }
    }

    static final class BiteGoal extends MeleeAttackGoal {
        private final DreadFish fish;
        private int lastBite = -1000;

        BiteGoal(DreadFish fish) {
            super(fish, 1.0, true);
            this.fish = fish;
        }

        @Override
        protected void resetAttackCooldown() {
            super.resetAttackCooldown();
            this.lastBite = this.fish.tickCount;
        }

        @Override
        protected boolean isTimeToAttack() {
            boolean alpha = this.fish.isAlpha();
            int wait = this.fish.frenzied() ? (alpha ? DreadFishRules.ALPHA_FRENZY_BITE_TICKS : DreadFishRules.FRENZY_BITE_TICKS)
                    : (alpha ? DreadFishRules.ALPHA_BITE_TICKS : DreadFishRules.BITE_TICKS);
            return this.fish.tickCount - this.lastBite >= wait;
        }

        /**
         * An alpha winds up before it bites (0.80.0): in reach and ready, it stops, rears back and gapes, with a roar;
         * {@link DreadFishRules#ALPHA_WINDUP_TICKS} later it snaps, hitting if its target's still in reach. The rest bite
         * as vanilla's melee does.
         */
        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (!this.fish.isAlpha()) {
                super.checkAndPerformAttack(target);
                return;
            }
            if (this.fish.windup > 0) {
                this.fish.getNavigation().stop();
                this.fish.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (--this.fish.windup == 0) {
                    this.fish.setWinding(false);
                    this.fish.swing(InteractionHand.MAIN_HAND);
                    if (this.fish.isWithinMeleeAttackRange(target)) {
                        this.fish.doHurtTarget(target);
                    }
                    this.resetAttackCooldown();
                }
                return;
            }
            if (this.canPerformAttack(target)) {
                this.fish.windup = DreadFishRules.ALPHA_WINDUP_TICKS;
                this.fish.setWinding(true);
                this.fish.getNavigation().stop();
                this.fish.playSound(SoundEvents.RAVAGER_ROAR, 0.7F, 1.5F);
            }
        }

        @Override
        public void start() {
            super.start();
            this.fish.hunting = true;
        }

        @Override
        public void stop() {
            super.stop();
            this.fish.hunting = false;
            this.fish.windup = 0;
            this.fish.setWinding(false);
        }

        @Override
        public boolean canUse() {
            return super.canUse() && prey(this.fish.getTarget());
        }
    }

    /**
     * Out of water: to the nearest water within reach with room for it (a big one doesn't fit every pool). As high a
     * priority as a fight; whichever starts, it keeps to.
     */
    static final class SeekWaterGoal extends Goal {
        private final DreadFish fish;
        private @Nullable BlockPos water;

        SeekWaterGoal(DreadFish fish) {
            this.fish = fish;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // Now and then, not every check (vanilla checks a goal every other tick, so a tick count's step could miss them all).
            if (this.fish.isInWater() || this.fish.getRandom().nextInt(5) != 0 || this.preyAtHand()) {
                return false;
            }
            Level level = this.fish.level();
            Optional<BlockPos> found = BlockPos.findClosestMatch(this.fish.blockPosition(), DreadFishRules.WATER_SEARCH, DreadFishRules.WATER_SEARCH_DEPTH,
                    pos -> level.getFluidState(pos).is(FluidTags.WATER) && level.noCollision(this.fish,
                            this.fish.getDimensions(this.fish.getPose()).makeBoundingBox(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)));
            this.water = found.orElse(null);
            return this.water != null;
        }

        private int timer;

        @Override
        public boolean canContinueToUse() {
            return this.water != null && !this.fish.isInWater() && this.timer < 200 && !this.preyAtHand();
        }

        /**
         * Its prey within a lunge of it (0.93.0): it turns to fight rather than keep slithering off to water with its back
         * to it. Water and prey keep their one priority (0.71.0) past that.
         */
        private boolean preyAtHand() {
            LivingEntity target = this.fish.getTarget();
            return target != null && target.isAlive() && prey(target) && this.fish.distanceTo(target) <= DreadFishRules.LUNGE_MAX;
        }

        @Override
        public void start() {
            this.timer = 0;
            if (this.water != null) {
                this.fish.getNavigation().moveTo(this.water.getX() + 0.5, this.water.getY(), this.water.getZ() + 0.5, 1.0);
            }
        }

        /**
         * No path (a big one, a small pool: a path needs room for its box): it slithers straight for it. (Vanilla's
         * swimming move control drops a wanted spot while the navigation's done, so it's pushed itself.)
         */
        @Override
        public void tick() {
            this.timer++;
            if (this.water == null || !this.fish.getNavigation().isDone()) {
                return;
            }
            Vec3 to = new Vec3(this.water.getX() + 0.5 - this.fish.getX(), 0.0, this.water.getZ() + 0.5 - this.fish.getZ());
            if (to.lengthSqr() < 1.0E-4) {
                return;
            }
            Vec3 way = to.normalize();
            this.fish.setYRot((float) (Mth.atan2(way.z, way.x) * Mth.RAD_TO_DEG) - 90.0F);
            this.fish.yBodyRot = this.fish.getYRot();
            Vec3 motion = this.fish.getDeltaMovement();
            double push = DreadFishRules.LAND_SPEED * 0.5;
            this.fish.setDeltaMovement(way.x * push, motion.y, way.z * push);
        }

        @Override
        public void stop() {
            this.water = null;
            this.fish.getNavigation().stop();
        }
    }

    /** With nothing to fight: to the nearest food lying near, and eats it, an item at a time. */
}
