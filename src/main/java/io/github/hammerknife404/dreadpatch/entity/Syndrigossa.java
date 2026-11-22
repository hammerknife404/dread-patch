package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.entity.DreadQueenEntity;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.ai.DragonAITargetNonTamedGoal;
import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonAttacks;
import com.iafenvoy.iceandfire.item.DragonEggItem;
import com.iafenvoy.uranus.animation.Animation;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.magic.DragonAge;
import io.github.hammerknife404.dreadpatch.magic.DragonBreath;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
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
 * and every 30 s she raises Sovereign's Eclipse round her target. She grows as an ice dragon does,
 * but rises ancient, at full strength, as the boss ({@link DragonAge}); she's undead (tagged; no
 * Wither, in {@code SyndrigossaEvents}), hunts players whatever Ice and Fire's config says, and her
 * corpse is Dread. A queen spawned as the boss rises riding one, bonded to it ({@link SoulTether}).
 */
public class Syndrigossa extends IceDragonEntity implements IDreadMob {
    /**
     * Ice and Fire looks a dragon's colour up by name for its textures, eyes and drops, and a colour
     * of our own would add itself to every ice dragon's palette (wild ones would spawn in it). So
     * underneath she's always white, and our renderer paints over it.
     */
    public static final String BASE_COLOR = "white";
    /** Her bite's Withering Rime: II (amplifier 1, withering twice as fast), for 4.5 s, as her queen's bolt lasts. */
    public static final int BITE_RIME_TICKS = 90;
    public static final int BITE_RIME_AMPLIFIER = 1;
    /** Sovereign's Eclipse: ticks between them, how far off her target may be, and how grown she must be. */
    static final int ECLIPSE_COOLDOWN = 600;
    static final double ECLIPSE_RANGE = 24.0;
    static final int ECLIPSE_MIN_STAGE = 3;

    private int eclipseCooldown = 200;
    /** Her own Shatter-Spine in the air: ticks between them, the range she looses one at, and how long her charge (Ice and Fire's) runs. */
    static final int SPINE_COOLDOWN = 200;
    static final double SPINE_MIN_RANGE = 6.0;
    static final double SPINE_MAX_RANGE = 32.0;
    static final int SPINE_CHARGE_TICKS = 24;
    private int spineCooldown = 60;
    private int spineCharging;
    /** On the ground with a live target, the chance each tick she takes off: about every 3 s (Ice and Fire: 1 in 1,500, and only after a target 5+ blocks above or below). */
    static final int TAKEOFF_CHANCE = 60;
    /** Breath particles a tick: sent from her mouth to carry along the cone, and set in it, so a short burst shows it whole. */
    static final int BREATH_PARTICLES_SENT = 10;
    static final int BREATH_PARTICLES_SET = 10;

    /** Saved, and what makes a summon or a spawn egg's entity data ancient ({@code Ancient:1b}). */
    public static final String ANCIENT_KEY = "Ancient";

    /** Her corpse (Dread: no flesh, blood or heart to take), her bones, and what she leaves in place of a heart (her egg too, if she's ancient). */
    public static final ResourceLocation CORPSE_LOOT = DreadPatch.id("entities/syndrigossa/corpse");
    public static final ResourceLocation BONES_LOOT = DreadPatch.id("entities/syndrigossa/skeleton");
    public static final ResourceLocation HEART_LOOT = DreadPatch.id("entities/syndrigossa/heart");

    /** Saved: the queen she rose under ({@link SoulTether}). */
    static final String QUEEN_KEY = "Queen";
    /** Saved: the difficulty her max health was last set for ({@link #refreshDirtyAttributes}). */
    static final String HEALTH_DIFFICULTY_KEY = "HealthDifficulty";
    /**
     * With a queen on her back, at this share of her health she's forced down, and once she lands her
     * queen steps off for good (the owner's choice, once, at half: the queen can't be hit in the saddle).
     */
    static final float QUEEN_OFF_AT = 0.5F;
    /** Then she can't take off for this long (10 s, the owner's choice): a window to punish her on the ground. */
    static final int GROUNDED_TICKS = 200;

    private boolean ancient;
    /**
     * Her own boss bar (0.34.0, the owner's ask), white beside her queen's blue: shown to every player
     * tracking her (as vanilla's Wither), hidden while she's tame and once she's a corpse.
     */
    private final ServerBossEvent bossBar = new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
    /** Set while Ice and Fire takes a harvest from her corpse, so its heart and egg can be swapped. */
    private boolean harvesting;
    @Nullable
    private UUID queenId;
    private boolean forcedDown;
    private int groundedTicks;
    /** The difficulty her max health is set for; no initializer, as Ice and Fire may set it from its constructor. */
    @Nullable
    private Difficulty healthDifficulty;

    public Syndrigossa(EntityType<? extends Syndrigossa> type, Level level) {
        super(type, level);
    }

    @Override
    public String getVariant() {
        return BASE_COLOR;
    }

    /** Whom she answers to: the queen she rose under, while that queen is loaded (server side). */
    @Nullable
    @Override
    public Entity getCommander() {
        return this.queenId != null && this.level() instanceof ServerLevel level ? level.getEntity(this.queenId) : null;
    }

    @Nullable
    public UUID getQueenId() {
        return this.queenId;
    }

    /** Bound to the queen she rose under: her mount, and the other end of their {@link SoulTether}. */
    public void bondTo(DreadQueen queen) {
        this.queenId = queen.getUUID();
    }

    /**
     * Ice and Fire seats only a tame dragon's owner; anyone else on a dragon is prey in its jaws, shaken
     * and bitten ({@code updatePreyInMouth}). A Dread Queen on her back rides in the saddle, placed as
     * Ice and Fire places a rider: her feet at its rider position plus her height, on the dragon's back
     * (full-grown: 9.1 blocks up for a player, against the model's back at 8.9). That's her hitbox and
     * where she casts from (at the rider position itself, as 0.24.0 had it, she was inside the dragon's
     * body, out of reach); she's drawn on the dragon's back by {@code SaddleLayer}.
     */
    @Override
    public void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (passenger instanceof DreadQueenEntity queen && this.hasPassenger(passenger)) {
            Vec3 seat = this.getRiderPosition();
            callback.accept(passenger, seat.x, seat.y + passenger.getBbHeight(), seat.z);
            // Facing along the dragon, as a rider does (Ice and Fire's riders turn it; she doesn't).
            queen.setYRot(this.yBodyRot);
            queen.yRotO = this.yBodyRotO;
            queen.yBodyRot = this.yBodyRot;
            queen.yBodyRotO = this.yBodyRotO;
            return;
        }
        super.positionRider(passenger, callback);
    }

    /** Anyone on her back but a Dread Queen: prey in her jaws, or a rider. */
    boolean carriesOthers() {
        for (Entity passenger : this.getPassengers()) {
            if (!(passenger instanceof DreadQueenEntity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ice and Fire's own, except that a queen in the saddle doesn't keep her on the ground (Ice and Fire's
     * needs no passengers at all), and not while she's forced down or grounded ({@link #bringQueenDown}).
     */
    @Override
    public boolean isAllowedToTriggerFlight() {
        return (this.hasFlightClearance() && this.onGround() || this.isInWater())
                && !this.isOrderedToSit() && !this.carriesOthers() && !this.isBaby() && !this.isSleeping() && this.canMove()
                && !this.mustStayDown();
    }

    private boolean mustStayDown() {
        return this.forcedDown || this.groundedTicks > 0;
    }

    /** Ice and Fire lands a dragon that wants to: hovering, it sinks; near the ground, it stops flying. */
    @Override
    public boolean doesWantToLand() {
        return this.mustStayDown() || super.doesWantToLand();
    }

    /**
     * Half her health gone ({@link #QUEEN_OFF_AT}) with a queen on her back: down she goes, flying turned
     * to hovering so Ice and Fire sinks her ({@link #doesWantToLand}), and once she's on the ground (or
     * in water) her queen Barrow-Steps off and she's grounded {@link #GROUNDED_TICKS} ticks. Once: her
     * queen never remounts.
     */
    private void bringQueenDown() {
        if (this.groundedTicks > 0) {
            this.groundedTicks--;
        }
        Entity rider = null;
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof DreadQueenEntity) {
                rider = passenger;
            }
        }
        if (rider != null && !this.forcedDown && this.getHealth() <= this.getMaxHealth() * QUEEN_OFF_AT) {
            this.forcedDown = true;
        }
        if (!this.mustStayDown()) {
            return;
        }
        if (this.isFlying()) {
            this.setFlying(false);
            this.setHovering(!this.onGround());
        }
        if (this.forcedDown && (this.onGround() || this.isInWater())) {
            this.forcedDown = false;
            this.groundedTicks = GROUNDED_TICKS;
            if (rider instanceof DreadQueen queen) {
                queen.leaveSaddle();
            } else if (rider != null) {
                rider.stopRiding();
            }
        }
    }

    public boolean isAncient() {
        return this.ancient;
    }

    /**
     * Ice and Fire's stats for her age, but her health is hers by difficulty (the owner's numbers:
     * 500 Easy, 750 Normal, 1,000 Hard at full strength, {@link DragonAge#fullHealth}), grown with her
     * age as Ice and Fire grows a dragon's ({@link DragonAge#maxHealth}), in place of its
     * {@code dragon.maxHealth}.
     */
    @Override
    public void refreshDirtyAttributes() {
        super.refreshDirtyAttributes();
        this.healthDifficulty = this.level().getDifficulty();
        AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(DragonAge.maxHealth(DragonAge.fullHealth(this.healthDifficulty), this.getAgeInDays()));
        }
    }

    /** The difficulty changed: her max health follows it, and she keeps the same share of it. */
    private void followDifficulty() {
        if (this.level().getDifficulty() == this.healthDifficulty) {
            return;
        }
        float share = this.getHealth() / this.getMaxHealth();
        this.refreshDirtyAttributes();
        this.setHealth(share * this.getMaxHealth());
    }

    /** Makes her ancient, the boss: grown to {@link DragonAge#ANCIENT_DAYS} if she's younger, and whole. */
    public void makeAncient() {
        this.ancient = true;
        if (this.getAgeInDays() < DragonAge.ANCIENT_DAYS) {
            this.setAgeInDays(DragonAge.ANCIENT_DAYS);
            this.refreshDirtyAttributes();
            this.refreshDimensions();
        }
        this.setHealth(this.getMaxHealth());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (DragonAge.spawnsAncient(reason)) {
            this.makeAncient();
        }
        return result;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    /** A name tag renames her bar too (and a loaded name, which comes through here). */
    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossBar.setName(this.getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(ANCIENT_KEY, this.ancient);
        if (this.healthDifficulty != null) {
            tag.putInt(HEALTH_DIFFICULTY_KEY, this.healthDifficulty.getId());
        }
        if (this.queenId != null) {
            tag.putUUID(QUEEN_KEY, this.queenId);
        }
    }

    /**
     * A dragon made from a tag ({@code /summon} with NBT skips {@code finalizeSpawn}) reads its
     * health while its max is still a hatchling's 20 (the tag has no saved attributes, which a saved
     * dragon's does), and Ice and Fire only grows the max to its age afterwards, here: summoned
     * full-grown she had 20 of 419 and fell to one warden hit, and {@code Health:300} came out 20.
     * Ice and Fire's own ice dragons do the same. So once her max is set: the tag's health, or whole.
     * {@code Ancient:1b} on a younger one (a summon, or a spawn egg's entity data, which merges into
     * what she already is) grows her and makes her whole.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.queenId = tag.hasUUID(QUEEN_KEY) ? tag.getUUID(QUEEN_KEY) : null;
        if (tag.getBoolean(ANCIENT_KEY) && this.getAgeInDays() < DragonAge.ANCIENT_DAYS) {
            this.makeAncient();
            return;
        }
        this.ancient = tag.getBoolean(ANCIENT_KEY);
        this.setHealth(tag.contains("Health", Tag.TAG_ANY_NUMERIC) ? tag.getFloat("Health") : this.getMaxHealth());
        // Saved at another difficulty (changed while she was unloaded): the same share of her health.
        if (tag.contains(HEALTH_DIFFICULTY_KEY, Tag.TAG_ANY_NUMERIC)) {
            Difficulty saved = Difficulty.byId(tag.getInt(HEALTH_DIFFICULTY_KEY));
            double was = DragonAge.maxHealth(DragonAge.fullHealth(saved), this.getAgeInDays());
            if (saved != this.healthDifficulty && was > 0.0) {
                this.setHealth((float) (this.getHealth() / was * this.getMaxHealth()));
            }
        }
    }

    /**
     * She hunts players whatever Ice and Fire's {@code neutralToPlayer} says (the owner's pack sets
     * it, so its wild dragons leave players be until struck): she's of the Dread. Ice and Fire's own
     * wild-dragon targeting, for players only and without that setting; never a creative or spectator
     * player, and a tame one ({@link DragonAITargetNonTamedGoal} stands down) follows its owner.
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(5, new DragonAITargetNonTamedGoal<>(this, Player.class, false,
                entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator()));
    }

    /** As Ice and Fire's ice dragon picks: bones from halfway through the harvest (when she's drawn as a skeleton). */
    @Override
    public ResourceLocation getDeadLootTable() {
        return this.getDeathStage() >= this.getAgeInDays() / 5 / 2 ? BONES_LOOT : CORPSE_LOOT;
    }

    /**
     * Her corpse is harvested as Ice and Fire's dragons are (empty hand, a take each click), but she's
     * undead: a bottle draws no blood, her heart is dread ({@link #HEART_LOOT}), and she carries no ice
     * dragon's egg. Her own egg is in her heart's table, for an ancient one only (the owner's call: only
     * the boss drops it), whatever her gender.
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isModelDead()) {
            return super.mobInteract(player, hand);
        }
        ItemStack held = player.getMainHandItem().isEmpty() ? player.getItemInHand(hand) : player.getMainHandItem();
        if (held.is(Items.GLASS_BOTTLE)) {
            return InteractionResult.PASS;
        }
        this.harvesting = true;
        try {
            return super.mobInteract(player, hand);
        } finally {
            this.harvesting = false;
        }
    }

    @Nullable
    @Override
    public ItemEntity spawnAtLocation(ItemStack stack, float offsetY) {
        if (this.harvesting && this.level() instanceof ServerLevel level) {
            if (stack.getItem() instanceof DragonEggItem) {
                return null;
            }
            if (stack.is(this.getHeartItem().asItem())) {
                ItemEntity last = null;
                for (ItemStack drop : this.rollLoot(level, HEART_LOOT)) {
                    last = super.spawnAtLocation(drop, offsetY);
                }
                return last;
            }
        }
        return super.spawnAtLocation(stack, offsetY);
    }

    /** One of her loot tables, rolled as Ice and Fire rolls a dragon's corpse table. */
    private List<ItemStack> rollLoot(ServerLevel level, ResourceLocation table) {
        LootTable loot = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, table));
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                .create(LootContextParamSets.ENTITY);
        return loot.getRandomItems(params);
    }

    /** Every 30 s, grown, with her target in sight within 24 blocks: Sovereign's Eclipse round it. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            this.bossBar.setProgress(this.getMaxHealth() > 0.0F ? Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F) : 0.0F);
            this.bossBar.setVisible(this.isAlive() && !this.isModelDead() && !this.isTame());
        }
        if (this.level().isClientSide || !this.isAlive() || this.isModelDead()) {
            return;
        }
        this.followDifficulty();
        // A target handed to her (by her queen, or kept from before) isn't rechecked by a target goal;
        // Ice and Fire breathes at whatever it is: a player gone creative or spectator included.
        LivingEntity current = this.getTarget();
        if (current != null && (!current.isAlive() || !this.canAttack(current))) {
            this.setTarget(null);
        }
        this.bringQueenDown();
        LivingEntity hunted = this.getTarget();
        if (hunted != null && hunted.isAlive() && !this.isFlying() && !this.isHovering() && !this.carriesOthers() && !this.isOrderedToSit()
                && !this.isSleeping() && this.isAllowedToTriggerFlight() && this.random.nextInt(TAKEOFF_CHANCE) == 0) {
            this.takeOff();
        }
        this.shatterSpine(hunted);
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

    /**
     * Her own Shatter-Spine, every {@link #SPINE_COOLDOWN} ticks in the air with her target in sight 6-32
     * blocks off: Ice and Fire's charge attack ({@code performChargeAttack}, called each tick of the
     * charge: its animation, then {@code createCharge} at its 20th tick), whatever air attack Ice and
     * Fire has picked. Its Hover Blast came round only now and then (1-2 in 40 s, the first 14-25 s in),
     * since Ice and Fire swaps air attacks in many places; the owner never saw one.
     */
    private void shatterSpine(@Nullable LivingEntity target) {
        boolean airborne = this.isFlying() || this.isHovering();
        if (this.spineCharging > 0) {
            this.spineCharging--;
            if (target != null && target.isAlive() && airborne) {
                this.performChargeAttack(target.getX(), target.getY(), target.getZ());
            } else {
                this.spineCharging = 0;
            }
            return;
        }
        if (this.spineCooldown > 0) {
            this.spineCooldown--;
            return;
        }
        if (target != null && target.isAlive() && airborne && !this.carriesOthers()) {
            double distance = this.distanceTo(target);
            if (distance >= SPINE_MIN_RANGE && distance <= SPINE_MAX_RANGE && this.hasLineOfSight(target)) {
                this.spineCharging = SPINE_CHARGE_TICKS;
                this.spineCooldown = SPINE_COOLDOWN;
            }
        }
    }

    /**
     * Up, as Ice and Fire takes a dragon off (hovering first, then flying), opening with Shatter-Spine:
     * Ice and Fire picks again after the charge. Otherwise it rarely showed: it's her only charge and
     * only in the air, and dragons can't target players in creative or spectator.
     */
    private void takeOff() {
        this.airAttack = IafDragonAttacks.Air.HOVER_BLAST;
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
     * Ice and Fire's breathing, reaching its full length in half a second (its stream takes two).
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
        double splash = DragonBreath.breathRadius(this.getDragonStage());
        // Every particle ends where the breath reaches (DragonBreathTest runs vanilla's particle motion on these).
        for (int i = 0; i < BREATH_PARTICLES_SENT; i++) {
            Vec3 v = this.spread(axis);
            double carry = DragonBreath.sentDistance(length, splash, this.random.nextDouble());
            ParticleOptions particle = this.createBreathParticle();
            level.sendParticles(particle, head.x, head.y, head.z, 0, v.x, v.y, v.z, breathParticleSpeed(particle, carry));
        }
        for (int i = 0; i < BREATH_PARTICLES_SET; i++) {
            double along = length * (0.15 + 0.85 * this.random.nextDouble());
            Vec3 at = head.add(this.spread(axis).scale(along));
            double drift = DragonBreath.driftDistance(length, along, splash, this.random.nextDouble());
            ParticleOptions particle = this.createBreathParticle();
            level.sendParticles(particle, at.x, at.y, at.z, 0, axis.x, axis.y, axis.z, breathParticleSpeed(particle, drift));
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

    /** The speed to send one of her breath particles at to carry {@code distance}: vanilla's snowflake goes further for its speed. */
    private static double breathParticleSpeed(ParticleOptions particle, double distance) {
        return particle == ParticleTypes.SNOWFLAKE ? DragonBreath.snowSpeed(distance) : DragonBreath.particleSpeed(distance);
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

    /** Whether she's biting: Ice and Fire's bite, or shaking prey in her jaws. */
    public boolean biting() {
        Animation action = this.getAnimation();
        return action == ANIMATION_BITE || action == ANIMATION_SHAKEPREY;
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
