package io.github.hammerknife404.dreadpatch.entity;

import net.minecraft.server.level.ServerLevel;
import io.github.hammerknife404.dreadpatch.block.RimefireBlock;
import com.iafenvoy.iceandfire.entity.DreadMobEntity;
import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import io.github.hammerknife404.dreadpatch.magic.DreadDamage;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Optional;

/**
 * Her signature bolt, the cold dread fire and soot of Syndrigossa's breath at a flame's size (the
 * owner's ask; until 0.25.0 charcoal, bone and indigo dust): flat damage plus a share of the target's max health
 * ({@link DreadDamage}), then Withering Rime. Shot by her and by the Withering Rime-Bolt spell; she
 * takes a bigger share than a player casting it. A queen's bolt passes through the Dread, and a
 * summoned court's scuttler's through whatever it mayn't fight ({@link SummonedCourt}). Into
 * someone a Rimefire Crown holds, it shatters the crown ({@link RimefireCrown#shatterAround}).
 */
public class WitheringRimeBolt extends AbstractMagicProjectile {

    private float maxHealthShare;
    private int rimeTicks;

    public WitheringRimeBolt(EntityType<? extends WitheringRimeBolt> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public WitheringRimeBolt(Level level, LivingEntity shooter, float damage, float maxHealthShare, int rimeTicks) {
        this(ModRegistries.WITHERING_RIME_BOLT.get(), level);
        this.setOwner(shooter);
        this.setDamage(damage);
        this.maxHealthShare = maxHealthShare;
        this.rimeTicks = rimeTicks;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(this.getOwner() instanceof IDreadMob && target instanceof IDreadMob)
                && !(target instanceof BarrowHusk husk && husk.getOwner() == this.getOwner())
                && !(this.getOwner() instanceof DreadMobEntity member && SummonedCourt.isMember(member) && !SummonedCourt.mayFight(member, target));
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        // Its embers (0.55.0): a brief block of rimefire where it struck, on the face it hit, or on top of that block.
        if (this.level() instanceof ServerLevel level && !RimefireBlock.embers(level, hit.getBlockPos().relative(hit.getDirection()))) {
            RimefireBlock.embers(level, hit.getBlockPos().above());
        }
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (this.level().isClientSide) {
            return;
        }
        Entity target = hit.getEntity();
        boolean crowned = target instanceof LivingEntity living && living.hasEffect(ModRegistries.RIMEBOUND);
        float amount = this.getDamage();
        if (target instanceof LivingEntity living) {
            amount += DreadDamage.maxHealthShare(living.getMaxHealth(), this.maxHealthShare);
        }
        boolean hurt = DamageSources.applyDamage(target, amount, ModRegistries.WITHERING_RIME_BOLT_SPELL.get().getDamageSource(this, this.getOwner()));
        if (hurt && target instanceof LivingEntity living && living.isAlive()) {
            living.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, this.rimeTicks), this.getOwner());
        }
        if (crowned) {
            RimefireCrown.shatterAround(this.level(), this, this.getOwner(), (LivingEntity) target);
        }
        this.consumeEntityImpact(hit, true);
    }

    /** Her dragon's breath in small: 60% cold flame, 30% soot, 10% snow, as {@code Syndrigossa.createBreathParticle} picks. */
    private ParticleOptions rimeParticle() {
        int pick = this.random.nextInt(10);
        return pick < 6 ? ModRegistries.RIME_FLAME_SMALL.get() : pick < 9 ? ModRegistries.RIME_SMOKE_SMALL.get() : ParticleTypes.SNOWFLAKE;
    }

    @Override
    public void trailParticles() {
        for (int i = 0; i < 4; i++) {
            double spread = 0.12;
            this.level().addParticle(this.rimeParticle(),
                    this.getX() + (this.random.nextDouble() - 0.5) * spread,
                    this.getY() + this.getBbHeight() * 0.5 + (this.random.nextDouble() - 0.5) * spread,
                    this.getZ() + (this.random.nextDouble() - 0.5) * spread,
                    (this.random.nextDouble() - 0.5) * 0.02, (this.random.nextDouble() - 0.5) * 0.02, (this.random.nextDouble() - 0.5) * 0.02);
        }
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        MagicManager.spawnParticles(this.level(), ModRegistries.RIME_FLAME_SMALL.get(), x, y, z, 18, 0.2, 0.2, 0.2, 0.08, true);
        MagicManager.spawnParticles(this.level(), ModRegistries.RIME_SMOKE_SMALL.get(), x, y, z, 9, 0.25, 0.25, 0.25, 0.04, true);
        MagicManager.spawnParticles(this.level(), ParticleTypes.SNOWFLAKE, x, y, z, 6, 0.2, 0.2, 0.2, 0.08, false);
    }

    @Override
    public float getSpeed() {
        return 1.5F;
    }

    @Override
    public Optional<Holder<SoundEvent>> getImpactSound() {
        return Optional.of(SoundRegistry.ICE_IMPACT);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("MaxHealthShare", this.maxHealthShare);
        tag.putInt("RimeTicks", this.rimeTicks);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.maxHealthShare = tag.getFloat("MaxHealthShare");
        this.rimeTicks = tag.getInt("RimeTicks");
    }
}
