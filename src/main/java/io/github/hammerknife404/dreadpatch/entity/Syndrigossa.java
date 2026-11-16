package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.util.IDreadMob;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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

    /** Ice and Fire's ice breath multiplier ({@code dragon.attackDamageIce}; 2.1 in the owner's pack). */
    public static double attackDamageIce() {
        return IafCommonConfig.INSTANCE.dragon.attackDamageIce.getValue();
    }

    /**
     * Her breath where Ice and Fire's stream lands (it calls this instead of freezing the spot):
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
            if (victim.hurt(source, damage) && victim.isAlive()) {
                victim.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, DragonBreath.RIME_TICKS), this);
            }
        }
        BlockPos center = BlockPos.containing(at);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
            RimefireCrown.drinkFire(level, pos);
        }
    }

    /** Soot, cold dread fire and snow, in place of the ice dragon's frost. */
    @Override
    public ParticleOptions createBreathParticle() {
        int pick = this.random.nextInt(4);
        return pick < 2 ? ParticleTypes.SOUL_FIRE_FLAME : pick == 2 ? ParticleTypes.SNOWFLAKE : ParticleTypes.LARGE_SMOKE;
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
