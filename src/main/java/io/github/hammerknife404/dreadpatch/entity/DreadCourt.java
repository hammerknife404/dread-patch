package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadMobEntity;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.iafenvoy.iceandfire.registry.IafSounds;
import io.github.hammerknife404.dreadpatch.magic.CourtRules;
import io.github.hammerknife404.dreadpatch.magic.DreadDamage;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Court of the Dread: the queen raises Ice and Fire's own dread mobs around her (they rise from the
 * ground with their own spawn animations), marked as hers the way the Dread Lich marks its minions,
 * and they fight what she fights. Beater's Decree: when one of them presses an enemy, she burns its
 * back. It's consumed in a burst of rime that hits everyone round it, and the rest of her court
 * frenzies.
 */
final class DreadCourt {
    /** The most of her court standing at once, and how many rise per call. */
    static final int MAX = 6;
    static final int WAVE = 3;
    static final int SUMMON_COOLDOWN = 400;
    static final int DECREE_COOLDOWN = 300;
    /** How far her court can be and still count as hers. */
    static final double ROSTER_RANGE = 48.0;
    /** A court member this close to an enemy is pressing it, and can be burnt for the decree. */
    static final double DECREE_REACH = 2.5;
    static final double DECREE_RADIUS = 3.0;
    /** The decree's rime: flat damage plus a share of max health (capped against bosses), and Withering Rime. */
    static final float DECREE_DAMAGE = 2.0F;
    static final float DECREE_MAX_HEALTH_SHARE = 0.12F;
    static final int DECREE_RIME_TICKS = 90;
    /** Her court near the burnt one frenzies: Speed I and Strength I. */
    static final double FRENZY_RANGE = 8.0;
    static final int FRENZY_TICKS = 120;

    private static final ParticleOptions CHARCOAL = new DustParticleOptions(new Vector3f(0.078F, 0.086F, 0.114F), 1.6F);
    private static final ParticleOptions BONE = new DustParticleOptions(new Vector3f(0.847F, 0.871F, 0.914F), 1.1F);

    private final DreadQueen queen;
    private int summonCooldown = 60;
    private int decreeCooldown = 200;

    DreadCourt(DreadQueen queen) {
        this.queen = queen;
    }

    /** Called every server tick she can act; {@code target} is null when she has nothing to fight. */
    void tick(@Nullable LivingEntity target) {
        if (this.summonCooldown > 0) {
            this.summonCooldown--;
        }
        if (this.decreeCooldown > 0) {
            this.decreeCooldown--;
        }
        if (target == null || !(this.queen.level() instanceof ServerLevel level)) {
            return;
        }
        boolean summonReady = this.summonCooldown == 0;
        boolean decreeReady = this.decreeCooldown == 0 && this.queen.tickCount % 10 == 0;
        boolean rally = this.queen.tickCount % 20 == 0;
        if (!summonReady && !decreeReady && !rally) {
            return;
        }
        List<DreadMobEntity> court = this.members(level);
        if (rally) {
            for (DreadMobEntity member : court) {
                if (member.getTarget() != target) {
                    member.setTarget(target);
                }
            }
        }
        if (summonReady) {
            this.raise(level, CourtRules.toRaise(court.size(), MAX, WAVE), target);
            this.summonCooldown = SUMMON_COOLDOWN;
        }
        if (decreeReady && this.decree(level, court)) {
            this.decreeCooldown = DECREE_COOLDOWN;
        }
    }

    /** Her court: living dread mobs within range that answer to her. */
    List<DreadMobEntity> members(ServerLevel level) {
        return level.getEntitiesOfClass(DreadMobEntity.class, this.queen.getBoundingBox().inflate(ROSTER_RANGE),
                m -> m.isAlive() && this.queen.getUUID().equals(m.getCommanderId()));
    }

    private void raise(ServerLevel level, int count, LivingEntity target) {
        int raised = 0;
        for (int i = 0; i < count; i++) {
            DreadMobEntity member = create(level, CourtRules.pick(this.queen.getRandom().nextInt(CourtRules.TOTAL_WEIGHT)));
            if (member == null) {
                continue;
            }
            Vec3 spot = this.findSpot(level, member);
            if (spot == null) {
                continue;
            }
            member.moveTo(spot.x, spot.y, spot.z, this.queen.getRandom().nextFloat() * 360.0F, 0.0F);
            // Through NeoForge, so other mods see the spawn (FinalizeSpawnEvent); it sets their rise animation and gear.
            EventHooks.finalizeMobSpawn(member, level, level.getCurrentDifficultyAt(member.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            member.setCommanderId(this.queen.getUUID());
            member.setTarget(target);
            level.addFreshEntity(member);
            MagicManager.spawnParticles(level, CHARCOAL, spot.x, spot.y + 0.2, spot.z, 20, 0.4, 0.1, 0.4, 0.02, false);
            raised++;
        }
        if (raised > 0) {
            this.queen.swing(InteractionHand.OFF_HAND);
            level.playSound(null, this.queen.getX(), this.queen.getY(), this.queen.getZ(), IafSounds.DREAD_LICH_SUMMON.get(), SoundSource.HOSTILE, 1.5F, 0.7F);
        }
    }

    @Nullable
    private static DreadMobEntity create(ServerLevel level, CourtRules.Kind kind) {
        EntityType<? extends DreadMobEntity> type = switch (kind) {
            case THRALL -> IafEntities.DREAD_THRALL.get();
            case GHOUL -> IafEntities.DREAD_GHOUL.get();
            case KNIGHT -> IafEntities.DREAD_KNIGHT.get();
            case SCUTTLER -> IafEntities.DREAD_SCUTTLER.get();
        };
        return type.create(level);
    }

    /** Solid ground 2-5 blocks from her with room to rise, or null if a few tries find none. */
    @Nullable
    private Vec3 findSpot(ServerLevel level, DreadMobEntity member) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = this.queen.getRandom().nextDouble() * Math.PI * 2;
            double distance = 2.0 + this.queen.getRandom().nextDouble() * 3.0;
            Vec3 guess = new Vec3(this.queen.getX() + Math.cos(angle) * distance, this.queen.getY(), this.queen.getZ() + Math.sin(angle) * distance);
            // It lands a hair under the surface (199.996 on a block top at 200); Iron's own spells lift it too.
            Vec3 ground = Utils.moveToRelativeGroundLevel(level, guess, 4).add(0, 0.05, 0);
            BlockPos below = BlockPos.containing(ground).below();
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                    && level.noCollision(member, member.getType().getSpawnAABB(ground.x, ground.y, ground.z))) {
                return ground;
            }
        }
        return null;
    }

    /** Beater's Decree: burn the court member pressing an enemy hardest. False if none is. */
    private boolean decree(ServerLevel level, List<DreadMobEntity> court) {
        DreadMobEntity burnt = null;
        double closest = DECREE_REACH;
        for (DreadMobEntity member : court) {
            for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, member.getBoundingBox().inflate(DECREE_REACH),
                    e -> e.isAlive() && RimefireCrown.canHarm(this.queen, e))) {
                double d = member.distanceTo(enemy);
                if (d <= closest) {
                    closest = d;
                    burnt = member;
                }
            }
        }
        if (burnt == null) {
            return false;
        }
        Vec3 at = burnt.position();
        burnt.discard();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, burnt.getBoundingBox().inflate(DECREE_RADIUS),
                e -> e.isAlive() && RimefireCrown.canHarm(this.queen, e))) {
            float amount = DECREE_DAMAGE + DreadDamage.maxHealthShare(victim.getMaxHealth(), DECREE_MAX_HEALTH_SHARE, victim.getType().is(Tags.EntityTypes.BOSSES));
            if (DamageSources.applyDamage(victim, amount, ModRegistries.WITHERING_RIME_BOLT_SPELL.get().getDamageSource(this.queen, this.queen)) && victim.isAlive()) {
                victim.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, DECREE_RIME_TICKS), this.queen);
            }
        }
        for (DreadMobEntity member : court) {
            if (member != burnt && member.distanceToSqr(at) <= FRENZY_RANGE * FRENZY_RANGE) {
                member.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, FRENZY_TICKS, 0), this.queen);
                member.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, FRENZY_TICKS, 0), this.queen);
            }
        }
        MagicManager.spawnParticles(level, BONE, at.x, at.y + 1.0, at.z, 40, 1.0, 0.8, 1.0, 0.1, false);
        MagicManager.spawnParticles(level, CHARCOAL, at.x, at.y + 1.0, at.z, 40, 1.0, 0.8, 1.0, 0.1, false);
        MagicManager.spawnParticles(level, ParticleTypes.SOUL, at.x, at.y + 1.0, at.z, 12, 0.5, 0.5, 0.5, 0.05, false);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.5F, 0.6F);
        return true;
    }
}
