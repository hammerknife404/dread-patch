package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadLichEntity;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Court of the Dread: the queen raises Ice and Fire's own dread mobs around her (they rise from the
 * ground with their own spawn animations), marked as hers the way the Dread Lich marks its minions,
 * and they fight what she fights. Beater's Decree: when one of them presses an enemy, she burns its
 * back. It's consumed in a burst of rime that hits everyone round it, and the rest of her court
 * frenzies. And now and then a Dread Lich ({@link #raiseLich}), one at a time, which raises minions
 * of its own.
 */
final class DreadCourt {
    /** How many rise per call (the most standing at once is {@link CourtRules#courtLimit}, by difficulty). */
    static final int WAVE = 3;
    static final int SUMMON_COOLDOWN = 400;
    static final int DECREE_COOLDOWN = 300;
    /** How far her court can be and still count as hers. */
    static final double ROSTER_RANGE = 48.0;
    /** A court member this close to an enemy is pressing it, and can be burnt for the decree. */
    static final double DECREE_REACH = 2.5;
    static final double DECREE_RADIUS = 3.0;
    /** The decree's rime: flat damage plus a share of max health (at most 20), and Withering Rime. */
    static final float DECREE_DAMAGE = 2.0F;
    static final float DECREE_MAX_HEALTH_SHARE = 0.12F;
    static final int DECREE_RIME_TICKS = 90;
    /** Her court near the burnt one frenzies: Speed I and Strength I. */
    static final double FRENZY_RANGE = 8.0;
    static final int FRENZY_TICKS = 120;
    /**
     * Her Dread Lich: the first 30 s in, then at most every 90 s (the owner: at least a minute, for
     * balance), and never while one of hers still stands; with nowhere to raise it, she tries again a
     * second later.
     */
    static final int FIRST_LICH = 600;
    static final int LICH_COOLDOWN = 1800;
    static final int LICH_RETRY = 20;

    private static final ParticleOptions CHARCOAL = new DustParticleOptions(new Vector3f(0.078F, 0.086F, 0.114F), 1.6F);
    private static final ParticleOptions BONE = new DustParticleOptions(new Vector3f(0.847F, 0.871F, 0.914F), 1.1F);

    private final DreadQueen queen;
    private int summonCooldown = 60;
    private int decreeCooldown = 200;
    private int lichCooldown = FIRST_LICH;

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
        if (this.lichCooldown > 0) {
            this.lichCooldown--;
        }
        if (!(this.queen.level() instanceof ServerLevel level)) {
            return;
        }
        boolean rally = this.queen.tickCount % 20 == 0;
        if (target == null) {
            // Nothing for her to fight: her court still spreads over whatever it found itself, and follows her lazily.
            if (rally) {
                List<DreadMobEntity> all = this.members(level);
                this.spread(level, all.stream().filter(m -> !(m instanceof DreadLichEntity)).toList(), null);
                this.follow(level, all);
            }
            return;
        }
        boolean summonReady = this.summonCooldown == 0;
        boolean decreeReady = this.decreeCooldown == 0 && this.queen.tickCount % 10 == 0;
        boolean lichReady = this.lichCooldown == 0;
        if (!summonReady && !decreeReady && !rally && !lichReady) {
            return;
        }
        List<DreadMobEntity> all = this.members(level);
        // Her lich stands apart: it takes none of her court's places, and isn't burnt for her decree.
        List<DreadMobEntity> court = all.stream().filter(m -> !(m instanceof DreadLichEntity)).toList();
        if (rally) {
            // Her lich takes her target; her court shares out what's near (0.91.0), her target first.
            for (DreadMobEntity member : all) {
                if (member instanceof DreadLichEntity && member.getTarget() != target) {
                    member.setTarget(target);
                }
            }
            this.spread(level, court, target);
            this.follow(level, all);
        }
        if (summonReady) {
            this.raise(level, CourtRules.toRaise(court.size(), CourtRules.courtLimit(level.getDifficulty().getId()), WAVE), target);
            this.summonCooldown = SUMMON_COOLDOWN;
        }
        if (decreeReady && this.decree(level, court)) {
            this.decreeCooldown = DECREE_COOLDOWN;
        }
        if (lichReady) {
            boolean standing = all.size() > court.size();
            this.lichCooldown = !standing && this.raiseLich(level, target) ? LICH_COOLDOWN : LICH_RETRY;
        }
    }

    /**
     * A Dread Lich rises beside her (Ice and Fire's, with its own rise and gear), hers: it fights what
     * she fights, and raises its own minions as a lich does (up to 5 in its life, one every 5 s; they
     * answer to it). False if there's nowhere for it to rise.
     */
    private boolean raiseLich(ServerLevel level, LivingEntity target) {
        DreadLichEntity lich = IafEntities.DREAD_LICH.get().create(level);
        if (lich == null) {
            return false;
        }
        Vec3 spot = this.findSpot(level, lich);
        if (spot == null) {
            return false;
        }
        lich.moveTo(spot.x, spot.y, spot.z, this.queen.getYRot(), 0.0F);
        EventHooks.finalizeMobSpawn(lich, level, level.getCurrentDifficultyAt(lich.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        lich.setCommanderId(this.queen.getUUID());
        lich.setTarget(target);
        if (!level.addFreshEntity(lich)) {
            return false;
        }
        MagicManager.spawnParticles(level, CHARCOAL, spot.x, spot.y + 0.2, spot.z, 30, 0.5, 0.1, 0.5, 0.02, false);
        MagicManager.spawnParticles(level, ParticleTypes.SOUL, spot.x, spot.y + 0.5, spot.z, 12, 0.4, 0.4, 0.4, 0.03, false);
        this.queen.swing(InteractionHand.OFF_HAND);
        level.playSound(null, this.queen.getX(), this.queen.getY(), this.queen.getZ(), IafSounds.DREAD_LICH_SUMMON.get(), SoundSource.HOSTILE, 2.0F, 0.5F);
        return true;
    }

    /**
     * Shares targets out among her court (0.91.0, {@link CourtRules#spread}): no more of them on one mob than
     * {@link CourtRules#pursuerCap} (any number on a player), her own target first, the rest on whatever else is near.
     */
    private void spread(ServerLevel level, List<DreadMobEntity> court, @Nullable LivingEntity preferred) {
        if (court.isEmpty()) {
            return;
        }
        List<LivingEntity> targets = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, this.queen.getBoundingBox().inflate(ROSTER_RANGE),
                this::prey));
        for (DreadMobEntity member : court) {
            LivingEntity own = member.getTarget();
            if (own != null && !targets.contains(own) && own.isAlive() && member.canAttack(own)) {
                targets.add(own);
            }
        }
        if (preferred != null && !targets.contains(preferred)) {
            targets.add(preferred);
        }
        if (targets.isEmpty()) {
            return;
        }
        double[][] distance = new double[court.size()][targets.size()];
        int[] current = new int[court.size()];
        int[] caps = new int[targets.size()];
        for (int t = 0; t < targets.size(); t++) {
            LivingEntity target = targets.get(t);
            caps[t] = CourtRules.pursuerCap(target.getMaxHealth(), target instanceof Player);
        }
        for (int m = 0; m < court.size(); m++) {
            DreadMobEntity member = court.get(m);
            current[m] = targets.indexOf(member.getTarget());
            for (int t = 0; t < targets.size(); t++) {
                LivingEntity target = targets.get(t);
                // A fresh target it must see; one it already has, or hers, it may chase out of sight.
                boolean known = t == current[m] || target == preferred;
                distance[m][t] = known || member.hasLineOfSight(target) ? member.distanceTo(target) : Double.POSITIVE_INFINITY;
            }
        }
        int[] chosen = CourtRules.spread(distance, current, caps, preferred == null ? -1 : targets.indexOf(preferred));
        for (int m = 0; m < court.size(); m++) {
            LivingEntity pick = chosen[m] >= 0 ? targets.get(chosen[m]) : null;
            if (pick != null && court.get(m).getTarget() != pick) {
                court.get(m).setTarget(pick);
            }
        }
    }

    /** What her court hunts: the living not of the Dread, nor allied to her, that can be fought. */
    private boolean prey(LivingEntity entity) {
        return entity.isAlive() && entity.attackable() && !(entity instanceof ArmorStand) && !RisenDread.dread(entity)
                && !this.queen.isAlliedTo(entity) && this.queen.canAttack(entity);
    }

    /**
     * Her court lazily follows her (0.91.0): one with nothing to fight further than {@link CourtRules#FOLLOW_FROM} walks back
     * toward her (the ground under her when she rides), unhurried, stopping {@link CourtRules#FOLLOW_TO} off.
     */
    private void follow(ServerLevel level, List<DreadMobEntity> all) {
        Vec3 her = this.queen.isPassenger() ? Utils.moveToRelativeGroundLevel(level, this.queen.position(), 64) : this.queen.position();
        for (DreadMobEntity member : all) {
            if (member.getTarget() != null || member.isPassenger() || member.position().distanceTo(her) <= CourtRules.FOLLOW_FROM
                    || !member.getNavigation().isDone()) {
                continue;
            }
            Path path = member.getNavigation().createPath(BlockPos.containing(her), (int) CourtRules.FOLLOW_TO);
            if (path != null) {
                member.getNavigation().moveTo(path, CourtRules.FOLLOW_SPEED);
            }
        }
    }

    /** Her court: living dread mobs within range that answer to her (her lich among them). */
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
    static DreadMobEntity create(ServerLevel level, CourtRules.Kind kind) {
        EntityType<? extends DreadMobEntity> type = switch (kind) {
            case THRALL -> IafEntities.DREAD_THRALL.get();
            case GHOUL -> IafEntities.DREAD_GHOUL.get();
            case KNIGHT -> IafEntities.DREAD_KNIGHT.get();
            case SCUTTLER -> IafEntities.DREAD_SCUTTLER.get();
        };
        return type.create(level);
    }

    /**
     * Solid ground 2-5 blocks from her with room to rise, or null if a few tries find none. Riding her
     * dragon, round the ground under her (up to 64 blocks down).
     */
    @Nullable
    private Vec3 findSpot(ServerLevel level, DreadMobEntity member) {
        Vec3 center = this.queen.isPassenger() ? Utils.moveToRelativeGroundLevel(level, this.queen.position(), 64) : this.queen.position();
        return findSpot(level, center, this.queen.getRandom(), member);
    }

    /** Solid ground 2-5 blocks from {@code center} with room for {@code member} to rise, or null. A caster's court rises the same way. */
    @Nullable
    static Vec3 findSpot(ServerLevel level, Vec3 center, RandomSource random, Mob member) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 2.0 + random.nextDouble() * 3.0;
            Vec3 guess = new Vec3(center.x + Math.cos(angle) * distance, center.y, center.z + Math.sin(angle) * distance);
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
            float amount = DECREE_DAMAGE + DreadDamage.maxHealthShare(victim.getMaxHealth(), DECREE_MAX_HEALTH_SHARE);
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
