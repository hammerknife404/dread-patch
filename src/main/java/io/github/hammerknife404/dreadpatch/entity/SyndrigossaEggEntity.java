package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.registry.IafSounds;
import io.github.hammerknife404.dreadpatch.block.DreadGateBlock;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Syndrigossa's egg, laid on dreadstone (0.39.0, the owner's choice of the plan's B): it hatches after
 * soaking up {@link #STORMS} full Whiteout Dominion storms cast over it (a storm that ran its whole
 * course with the egg inside it from its start, the egg still on dreadstone: {@code WhiteoutDominion}
 * counts them), into a Rimefrost dragon hatchling, white like her, tamed to whoever laid it, as Ice and
 * Fire's eggs hatch (Syndrigossa is unique: hers are Rimefrost dragons). Struck by a player, it pops
 * back into an egg (its storms are lost). Nothing else harms it. Saved: who laid it, its storms.
 */
public class SyndrigossaEggEntity extends Entity {
    public static final int STORMS = 3;
    static final String OWNER_KEY = "Owner";
    static final String SOAKED_KEY = "Soaked";

    @Nullable
    private UUID owner;
    private int soaked;

    public SyndrigossaEggEntity(EntityType<? extends SyndrigossaEggEntity> type, Level level) {
        super(type, level);
    }

    public static SyndrigossaEggEntity lay(Level level, Vec3 at, float yaw, @Nullable UUID owner) {
        SyndrigossaEggEntity egg = new SyndrigossaEggEntity(ModRegistries.SYNDRIGOSSA_EGG_ENTITY.get(), level);
        egg.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        egg.owner = owner;
        return egg;
    }

    /** Whether it lies on dreadstone (the blocks a Dread Gate's frame takes). */
    public boolean onDreadstone() {
        BlockPos below = BlockPos.containing(this.getX(), this.getY() - 0.1, this.getZ());
        return this.level().getBlockState(below).is(DreadGateBlock.FRAME);
    }

    public int getSoaked() {
        return this.soaked;
    }

    /** A full storm has passed over it: one closer to hatching, if it lies on dreadstone. */
    public void soak() {
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved() || !this.onDreadstone()) {
            return;
        }
        this.soaked++;
        level.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(0.5), this.getZ(), 30, 0.3, 0.3, 0.3, 0.05);
        level.sendParticles(ModRegistries.RIME_FLAME_SMALL.get(), this.getX(), this.getY(0.5), this.getZ(), 20, 0.25, 0.3, 0.25, 0.02);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.5F, 0.6F);
        Player layer = this.owner != null ? level.getPlayerByUUID(this.owner) : null;
        if (this.soaked >= STORMS) {
            this.hatch(level);
        } else if (layer != null) {
            layer.displayClientMessage(Component.translatable("message.dreadpatch.syndrigossa_egg.soaked", this.soaked, STORMS), true);
        }
    }

    /** Into a Rimefrost hatchling, white, tamed to whoever laid it (as Ice and Fire hatches its eggs). */
    private void hatch(ServerLevel level) {
        RimefrostDragon dragon = ModRegistries.RIMEFROST_DRAGON.get().create(level);
        if (dragon == null) {
            return;
        }
        dragon.setVariant(Syndrigossa.BASE_COLOR);
        dragon.setGender(this.random.nextBoolean());
        dragon.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
        dragon.setHunger(50);
        dragon.refreshDirtyAttributes();
        dragon.setHealth(dragon.getMaxHealth());
        if (this.hasCustomName()) {
            dragon.setCustomName(this.getCustomName());
        }
        if (this.owner != null) {
            dragon.setTame(true, true);
            dragon.setOwnerUUID(this.owner);
        }
        dragon.setPersistenceRequired();
        level.addFreshEntity(dragon);
        level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY(0.5), this.getZ(), 30, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), IafSounds.EGG_HATCH.get(), SoundSource.NEUTRAL, 2.5F, 0.8F);
        this.discard();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        // It lies where it was laid, and falls if what it lay on goes.
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 0.98, 0.6));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, -0.5, 1.0));
        }
        if (this.level().isClientSide && this.tickCount % 40 == 0) {
            this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getRandomX(0.6), this.getY(0.8), this.getRandomZ(0.6), 0.0, -0.01, 0.0);
        }
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    /** A player's strike takes it back up (as an egg, its storms lost); nothing else harms it. {@code /kill} still removes it. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.isRemoved() || !(source.getEntity() instanceof Player)) {
            return false;
        }
        ItemStack egg = new ItemStack(ModRegistries.SYNDRIGOSSA_EGG.get());
        if (this.hasCustomName()) {
            egg.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, this.getCustomName());
        }
        this.spawnAtLocation(egg, 0.2F);
        this.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 0.8F);
        this.discard();
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.owner = tag.hasUUID(OWNER_KEY) ? tag.getUUID(OWNER_KEY) : null;
        this.soaked = tag.getInt(SOAKED_KEY);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.owner != null) {
            tag.putUUID(OWNER_KEY, this.owner);
        }
        tag.putInt(SOAKED_KEY, this.soaked);
    }
}
