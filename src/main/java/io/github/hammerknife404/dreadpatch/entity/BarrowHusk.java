package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.registry.IafParticles;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Barrow Husk: the rime-glass shell Barrow-Step leaves in its caster's shape where they stood. Whatever
 * was hunting the caster nearby turns on it. Struck by anyone but its caster, or after
 * {@link #LIFETIME} ticks, it shatters: everyone within {@link #SHATTER_RADIUS} blocks its caster may
 * harm is rooted (Rimebound) and touched by Withering Rime. Nothing else hurts it, and it takes no
 * effects. Never saved: it lasts seconds.
 */
public class BarrowHusk extends LivingEntity implements TraceableEntity {
    public static final int LIFETIME = 100;
    public static final double SHATTER_RADIUS = 3.0;
    /** Rimebound and Withering Rime on whoever's caught in the shatter. */
    public static final int ROOT_TICKS = 40;
    public static final int RIME_TICKS = 40;
    /** How far off the mobs hunting its caster turn on it. */
    static final double TAUNT_RADIUS = 16.0;

    private static final ParticleOptions ICE_SHARDS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());
    private static final NonNullList<ItemStack> NO_ARMOR = NonNullList.withSize(4, ItemStack.EMPTY);

    @Nullable
    private UUID ownerId;
    @Nullable
    private Entity cachedOwner;

    public BarrowHusk(EntityType<? extends BarrowHusk> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /** Leaves {@code caster}'s husk at {@code at}, facing {@code yRot}, and turns their hunters near it on it. */
    public static BarrowHusk leaveBehind(LivingEntity caster, Vec3 at, float yRot) {
        BarrowHusk husk = new BarrowHusk(ModRegistries.BARROW_HUSK.get(), caster.level());
        husk.moveTo(at.x, at.y, at.z, yRot, 0.0F);
        husk.setYHeadRot(yRot);
        husk.setYBodyRot(yRot);
        husk.ownerId = caster.getUUID();
        husk.cachedOwner = caster;
        caster.level().addFreshEntity(husk);
        for (Mob hunter : caster.level().getEntitiesOfClass(Mob.class, husk.getBoundingBox().inflate(TAUNT_RADIUS), m -> m.getTarget() == caster)) {
            hunter.setTarget(husk);
        }
        if (caster.level() instanceof ServerLevel level) {
            level.sendParticles(ICE_SHARDS, husk.getX(), husk.getY(1.0), husk.getZ(), 20, 0.3, 0.6, 0.3, 0.1);
            level.playSound(null, husk.getX(), husk.getY(), husk.getZ(), SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 1.2F, 0.5F);
        }
        return husk;
    }

    @Nullable
    @Override
    public Entity getOwner() {
        if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
            return this.cachedOwner;
        }
        if (this.ownerId != null && this.level() instanceof ServerLevel level) {
            this.cachedOwner = level.getEntity(this.ownerId);
        }
        return this.cachedOwner;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.tickCount % 3 == 0) {
                this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, -0.02, 0.0);
            }
            if (this.tickCount % 7 == 0) {
                this.level().addParticle(IafParticles.DREAD_TORCH.get(), this.getRandomX(0.5), this.getY(0.3), this.getRandomZ(0.5), 0.0, 0.02, 0.0);
            }
        } else if (this.tickCount >= LIFETIME) {
            this.shatter();
        }
    }

    /** Struck by anyone but its caster, it shatters. Unattributed harm (fire, falling) doesn't touch it; /kill removes it quietly. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.isRemoved()) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.discard();
            return true;
        }
        Entity attacker = source.getEntity();
        if (attacker == null || attacker == this.getOwner()) {
            return false;
        }
        this.shatter();
        return true;
    }

    /** Roots and rimes everyone near it its caster may harm, and breaks. */
    public void shatter() {
        if (this.level() instanceof ServerLevel level) {
            Entity owner = this.getOwner();
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(SHATTER_RADIUS),
                    e -> e != this && e.isAlive() && e.distanceTo(this) <= SHATTER_RADIUS && RimefireCrown.canHarm(owner, e))) {
                victim.addEffect(new MobEffectInstance(ModRegistries.RIMEBOUND, ROOT_TICKS), owner);
                victim.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, RIME_TICKS), owner);
            }
            level.sendParticles(ICE_SHARDS, this.getX(), this.getY(0.5), this.getZ(), 50, 0.4, 0.7, 0.4, 0.25);
            level.sendParticles(IafParticles.DREAD_TORCH.get(), this.getX(), this.getY(0.5), this.getZ(), 20, 0.5, 0.7, 0.5, 0.05);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5F, 0.7F);
        }
        this.discard();
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return NO_ARMOR;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.ownerId = tag.getUUID("Owner");
        }
    }
}
