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
 * to. With nothing to fight it eats dropped food ({@link #EATS}, any food but {@link #REFUSES}), one item at a
 * time, healing 0.25-1 each.</li>
 * </ul>
 * Raised from a water creature in her storm ({@link RisenDread#rise}) it takes its raiser's target, as the risen
 * do; it stirs on its own in the Dreadlands' dark water ({@code world/RisenSpawns}). Its lure's flicker and flare
 * are drawn by {@code client/RimeEelRenderer}.
 */
public class RimeEel extends Monster implements IDreadMob {
    /** Not food, but eaten (ink sacs). */
    public static final TagKey<Item> EATS = TagKey.create(Registries.ITEM, DreadPatch.id("rime_eel_eats"));
    /** Food, but left (golden apples and carrots). */
    public static final TagKey<Item> REFUSES = TagKey.create(Registries.ITEM, DreadPatch.id("rime_eel_refuses"));
    /** To its clients: it has pulsed (its lure flares). */
    public static final byte PULSE_EVENT = 101;

    private int auraCooldown = RimeEelRules.AURA_MIN_TICKS;
    private int frenzy;
    private int lungeCooldown;
    /** On a client: the tick it last pulsed. */
    private int flaredAt = -1000;

    public RimeEel(EntityType<? extends RimeEel> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, RimeEelRules.WATER_SPEED, RimeEelRules.LAND_SPEED, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
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
        this.goalSelector.addGoal(3, new EatFoodGoal(this));
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
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    public void setSize(double scale) {
        set(this.getAttribute(Attributes.SCALE), scale);
        set(this.getAttribute(Attributes.MAX_HEALTH), RimeEelRules.health(scale));
        set(this.getAttribute(Attributes.ATTACK_DAMAGE), RimeEelRules.bite(scale));
        this.setHealth(this.getMaxHealth());
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
        if (--this.auraCooldown <= 0) {
            this.auraCooldown = RimeEelRules.auraTicks(this.random.nextDouble());
            this.pulse();
        }
        if (RimeEelRules.lunges(this.isInWater(), this.onGround(), this.distanceTo(target), this.getSensing().hasLineOfSight(target), this.lungeCooldown)) {
            this.lungeCooldown = RimeEelRules.LUNGE_TICKS;
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
     * Where its lantern hangs, at rest: in model pixels (-6, 1, -14) from its feet (client/RimeEelModel), drawn as
     * vanilla draws a living thing's model (turned by its body, flipped in x and y) and scaled with it.
     */
    public Vec3 lantern() {
        double scale = this.getScale() / 16.0;
        double x = 6.0 * scale;
        double z = -14.0 * scale;
        double turn = Math.toRadians(180.0F - this.yBodyRot);
        double wx = x * Math.cos(turn) + z * Math.sin(turn);
        double wz = -x * Math.sin(turn) + z * Math.cos(turn);
        return this.position().add(wx, 23.0 * scale, wz);
    }

    /** On a client: whether its lure burns now (its flicker, or just after a pulse). */
    public boolean lureLit() {
        return this.tickCount - this.flaredAt < RimeEelRules.FLARE_TICKS || RimeEelRules.lureLit(this.getUUID().hashCode(), this.tickCount);
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
    public static boolean eats(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(EATS) || stack.has(DataComponents.FOOD) && !stack.is(REFUSES));
    }

    /** Eats one of {@code item}'s stack, healing by its food. */
    void eat(ItemEntity item) {
        ItemStack stack = item.getItem().copy();
        FoodProperties food = stack.get(DataComponents.FOOD);
        stack.shrink(1);
        if (stack.isEmpty()) {
            item.discard();
        } else {
            item.setItem(stack);
        }
        this.heal(RimeEelRules.heal(food == null ? 0 : food.nutrition()));
        this.playSound(SoundEvents.GENERIC_EAT, 0.6F, this.getVoicePitch());
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.BUBBLE, item.getX(), item.getY() + 0.1, item.getZ(), 4, 0.1, 0.1, 0.1, 0.02);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Frenzy", this.frenzy);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.frenzy = tag.getInt("Frenzy");
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
            int wait = this.eel.frenzied() ? RimeEelRules.FRENZY_BITE_TICKS : RimeEelRules.BITE_TICKS;
            return this.eel.tickCount - this.lastBite >= wait;
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
    static final class EatFoodGoal extends Goal {
        private final RimeEel eel;
        private @Nullable ItemEntity food;
        private int timer;

        EatFoodGoal(RimeEel eel) {
            this.eel = eel;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean wanted(ItemEntity item) {
            return item.isAlive() && !item.hasPickUpDelay() && eats(item.getItem());
        }

        @Override
        public boolean canUse() {
            if (this.eel.getTarget() != null || this.eel.getRandom().nextInt(5) != 0) {
                return false;
            }
            this.food = this.eel.level().getEntitiesOfClass(ItemEntity.class, this.eel.getBoundingBox().inflate(RimeEelRules.FOOD_SEARCH), this::wanted)
                    .stream().min(java.util.Comparator.comparingDouble(this.eel::distanceToSqr)).orElse(null);
            return this.food != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.food != null && this.wanted(this.food) && this.eel.getTarget() == null && this.timer < 300
                    && this.eel.distanceToSqr(this.food) < (RimeEelRules.FOOD_SEARCH + 4) * (RimeEelRules.FOOD_SEARCH + 4);
        }

        @Override
        public void start() {
            this.timer = 0;
        }

        @Override
        public void stop() {
            this.food = null;
            this.eel.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        /** Within a block of its snout across, and up to a block and a half over or under it (food floats on water it swims in). */
        private boolean inReach(ItemEntity item) {
            double across = this.eel.getBbWidth() / 2.0 + 1.0;
            double dx = item.getX() - this.eel.getX();
            double dz = item.getZ() - this.eel.getZ();
            double dy = item.getY() - this.eel.getY();
            return dx * dx + dz * dz <= across * across && dy >= -1.5 && dy <= this.eel.getBbHeight() + 1.5;
        }

        @Override
        public void tick() {
            if (this.food == null) {
                return;
            }
            this.timer++;
            this.eel.getLookControl().setLookAt(this.food);
            if (this.inReach(this.food)) {
                this.eel.getNavigation().stop();
                if (this.timer % RimeEelRules.EAT_TICKS == 0) {
                    this.eel.eat(this.food);
                }
            } else if (this.timer % 10 == 1) {
                this.eel.getNavigation().moveTo(this.food, 1.0);
            }
        }
    }
}
