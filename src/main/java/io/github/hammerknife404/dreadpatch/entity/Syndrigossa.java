package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadQueenEntity;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.magic.DragonAge;
import io.redspace.ironsspellbooks.api.util.Utils;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Syndrigossa, the Dread Queen's undead frost wyrm: in the owner's taxonomy a unique, named dragon of
 * the Rimefrost, a subtype of frost dragon. A {@link RimefrostDragon} (its breath, bite, Shatter-Spine,
 * undeath, Dread corpse and look come with it), and hers alone: she's always white, every 30 s she
 * raises Sovereign's Eclipse round her target, her health is set by difficulty, she rises ancient, at
 * full strength, as the boss ({@link DragonAge}), with her own boss bar, her queen rides her
 * ({@link SoulTether}), and only the boss leaves her egg.
 */
public class Syndrigossa extends RimefrostDragon {
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
    protected ResourceLocation corpseLoot() {
        return CORPSE_LOOT;
    }

    @Override
    protected ResourceLocation bonesLoot() {
        return BONES_LOOT;
    }

    @Override
    protected ResourceLocation heartLoot() {
        return HEART_LOOT;
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
    @Override
    protected boolean carriesOthers() {
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
     * As every Rimefrost dragon reads its tag ({@link RimefrostDragon#readAdditionalSaveData}: the tag's
     * health, or whole), then hers: {@code Ancient:1b} on a younger one (a summon, or a spawn egg's
     * entity data, which merges into what she already is) grows her and makes her whole, and one saved
     * at another difficulty keeps the same share of her health.
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
        // Saved at another difficulty (changed while she was unloaded): the same share of her health.
        if (tag.contains(HEALTH_DIFFICULTY_KEY, Tag.TAG_ANY_NUMERIC)) {
            Difficulty saved = Difficulty.byId(tag.getInt(HEALTH_DIFFICULTY_KEY));
            double was = DragonAge.maxHealth(DragonAge.fullHealth(saved), this.getAgeInDays());
            if (saved != this.healthDifficulty && was > 0.0) {
                this.setHealth((float) (this.getHealth() / was * this.getMaxHealth()));
            }
        }
    }

    /** Her health follows the difficulty, and her queen comes down at half her health, before any attack of a Rimefrost dragon's. */
    @Override
    protected void beforeAttacks() {
        this.followDifficulty();
        this.bringQueenDown();
    }

    /** Her boss bar; and every 30 s, grown, with her target in sight within 24 blocks: Sovereign's Eclipse round it. */
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
}
