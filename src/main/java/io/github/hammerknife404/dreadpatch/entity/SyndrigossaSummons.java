package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadBeastEntity;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.iafenvoy.iceandfire.registry.IafSounds;
import io.github.hammerknife404.dreadpatch.magic.SummonRules;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Syndrigossa's Dread Fish and dread beasts (0.93.0, {@link SummonRules}): while she's wild and fighting, 1-2 of each
 * rise round her target now and then, each kind on its own countdown, spread through the fight; they're her court's for
 * 25 s ({@link SummonedCourt}, unbound by any player's terms), fighting what she fights. Fish rise in water near her
 * target if there's any, else on the ground; beasts on the ground. Her countdowns aren't saved: a fight after a reload
 * starts afresh.
 */
final class SyndrigossaSummons {
    /** How far round her her summons are counted. */
    static final double ROSTER_RANGE = 128.0;
    private static final ParticleOptions CHARCOAL = new DustParticleOptions(new Vector3f(0.078F, 0.086F, 0.114F), 1.6F);

    private final Syndrigossa dragon;
    private boolean fighting;
    private int fish;
    private int beasts;
    private int sinceLast = SummonRules.GAP;

    SyndrigossaSummons(Syndrigossa dragon) {
        this.dragon = dragon;
    }

    /** Every server tick she's alive and wild; {@code target} null when she isn't fighting. */
    void tick(ServerLevel level, @Nullable LivingEntity target) {
        this.sinceLast++;
        if (target == null || !target.isAlive() || this.dragon.distanceTo(target) > SummonRules.RANGE) {
            this.fighting = false;
            return;
        }
        if (!this.fighting) {
            // A fight begins: the fish first, then the beasts.
            this.fighting = true;
            this.fish = SummonRules.first(true, this.dragon.getRandom().nextDouble());
            this.beasts = SummonRules.first(false, this.dragon.getRandom().nextDouble());
        }
        this.fish--;
        this.beasts--;
        if (SummonRules.mayCome(this.fish, this.sinceLast)) {
            this.summon(level, target, true);
            this.fish = SummonRules.cooldown(this.dragon.getRandom().nextDouble());
        } else if (SummonRules.mayCome(this.beasts, this.sinceLast)) {
            this.summon(level, target, false);
            this.beasts = SummonRules.cooldown(this.dragon.getRandom().nextDouble());
        }
    }

    /** 1-2 of a kind round {@code target}, as many as leave her at most 2 of it. A full roster or no room still spends the turn. */
    private void summon(ServerLevel level, LivingEntity target, boolean fish) {
        int count = SummonRules.toSummon(this.standing(level, fish ? DreadFish.class : DreadBeastEntity.class), this.dragon.getRandom().nextBoolean());
        if (count == 0) {
            return;
        }
        long expires = level.getGameTime() + SummonRules.LIFETIME;
        int raised = fish ? SummonedCourt.raiseFish(level, this.dragon, target.position(), count, expires, true, target)
                : this.raiseBeasts(level, target, count, expires);
        if (raised > 0) {
            this.sinceLast = 0;
            this.dragon.playSound(this.dragon.getRoarSound(), 3.0F, 0.85F);
        }
    }

    private int raiseBeasts(ServerLevel level, LivingEntity target, int count, long expires) {
        int raised = 0;
        for (int i = 0; i < count; i++) {
            DreadBeastEntity beast = IafEntities.DREAD_BEAST.get().create(level);
            if (beast == null) {
                continue;
            }
            Vec3 spot = DreadCourt.findSpot(level, target.position(), this.dragon.getRandom(), beast);
            if (spot == null) {
                continue;
            }
            beast.moveTo(spot.x, spot.y, spot.z, this.dragon.getRandom().nextFloat() * 360.0F, 0.0F);
            // Through NeoForge, as her court rises: its rise and its size.
            EventHooks.finalizeMobSpawn(beast, level, level.getCurrentDifficultyAt(beast.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            beast.setCommanderId(this.dragon.getUUID());
            beast.getPersistentData().putLong(SummonedCourt.EXPIRES_KEY, expires);
            beast.getPersistentData().putBoolean(SummonedCourt.UNBOUND_KEY, true);
            beast.setPersistenceRequired();
            beast.setTarget(target);
            if (level.addFreshEntity(beast)) {
                MagicManager.spawnParticles(level, CHARCOAL, spot.x, spot.y + 0.2, spot.z, 20, 0.4, 0.1, 0.4, 0.02, false);
                MagicManager.spawnParticles(level, ParticleTypes.SOUL, spot.x, spot.y + 0.4, spot.z, 6, 0.3, 0.3, 0.3, 0.02, false);
                level.playSound(null, spot.x, spot.y, spot.z, IafSounds.DREAD_LICH_SUMMON.get(), SoundSource.HOSTILE, 1.0F, 0.9F);
                raised++;
            }
        }
        return raised;
    }

    /** How many of hers of {@code kind} are about. */
    private int standing(ServerLevel level, Class<? extends Mob> kind) {
        return level.getEntitiesOfClass(kind, this.dragon.getBoundingBox().inflate(ROSTER_RANGE),
                mob -> mob.isAlive() && SummonedCourt.serves(mob, this.dragon)).size();
    }
}
