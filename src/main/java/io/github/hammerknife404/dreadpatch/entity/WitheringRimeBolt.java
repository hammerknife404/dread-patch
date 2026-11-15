package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import io.github.hammerknife404.dreadpatch.magic.DreadDamage;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.neoforged.neoforge.common.Tags;
import org.joml.Vector3f;

import java.util.Optional;

/**
 * Her signature bolt, soot-black and bone-white: flat damage plus a share of the target's max health
 * ({@link DreadDamage}), then Withering Rime. Shot by her and by the Withering Rime-Bolt spell; she
 * takes a bigger share than a player casting it. A queen's bolt passes through the Dread. Into
 * someone a Barrow-Glass Tomb holds, it shatters the tomb ({@link BarrowGlassTomb#shatterAround}).
 */
public class WitheringRimeBolt extends AbstractMagicProjectile {
    // Her colours from the owner's notes: charcoal, bone-ash and abyssal indigo.
    private static final ParticleOptions CHARCOAL = new DustParticleOptions(new Vector3f(0.078F, 0.086F, 0.114F), 1.2F);
    private static final ParticleOptions BONE = new DustParticleOptions(new Vector3f(0.847F, 0.871F, 0.914F), 0.9F);
    private static final ParticleOptions INDIGO = new DustParticleOptions(new Vector3f(0.118F, 0.165F, 0.290F), 1.1F);

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
        return super.canHitEntity(target) && !(this.getOwner() instanceof IDreadMob && target instanceof IDreadMob);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (this.level().isClientSide) {
            return;
        }
        Entity target = hit.getEntity();
        boolean entombed = target instanceof LivingEntity living && living.hasEffect(ModRegistries.BARROW_GLASS);
        float amount = this.getDamage();
        if (target instanceof LivingEntity living) {
            amount += DreadDamage.maxHealthShare(living.getMaxHealth(), this.maxHealthShare, living.getType().is(Tags.EntityTypes.BOSSES));
        }
        boolean hurt = DamageSources.applyDamage(target, amount, ModRegistries.WITHERING_RIME_BOLT_SPELL.get().getDamageSource(this, this.getOwner()));
        if (hurt && target instanceof LivingEntity living && living.isAlive()) {
            living.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, this.rimeTicks), this.getOwner());
        }
        if (entombed) {
            BarrowGlassTomb.shatterAround(this.level(), this, this.getOwner(), (LivingEntity) target);
        }
        this.consumeEntityImpact(hit, true);
    }

    @Override
    public void trailParticles() {
        for (int i = 0; i < 3; i++) {
            ParticleOptions particle = switch (this.random.nextInt(3)) {
                case 0 -> CHARCOAL;
                case 1 -> BONE;
                default -> INDIGO;
            };
            double spread = 0.12;
            this.level().addParticle(particle,
                    this.getX() + (this.random.nextDouble() - 0.5) * spread,
                    this.getY() + this.getBbHeight() * 0.5 + (this.random.nextDouble() - 0.5) * spread,
                    this.getZ() + (this.random.nextDouble() - 0.5) * spread, 0, 0, 0);
        }
        if (this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(), 0, 0, 0);
        }
    }

    @Override
    public void impactParticles(double x, double y, double z) {
        MagicManager.spawnParticles(this.level(), CHARCOAL, x, y, z, 18, 0.25, 0.25, 0.25, 0.05, true);
        MagicManager.spawnParticles(this.level(), BONE, x, y, z, 12, 0.2, 0.2, 0.2, 0.05, true);
        MagicManager.spawnParticles(this.level(), ParticleTypes.SNOWFLAKE, x, y, z, 10, 0.2, 0.2, 0.2, 0.08, false);
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
