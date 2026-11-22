package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadLichEntity;
import io.github.hammerknife404.dreadpatch.magic.PhantomRules;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Calling dread phantoms (0.45.0). The queen, any Dread Lich and a wild Syndrigossa (not other
 * Rimefrost dragons) call 1–2 while they've a target: the first {@link PhantomRules#FIRST_CALL} ticks
 * into a fight, then every {@link PhantomRules#CALL_COOLDOWN}, never past {@link PhantomRules#MAX_STANDING}
 * of theirs at once. Their phantoms escort them and take their fights. The countdown is in the
 * summoner's persistent data (the lich is Ice and Fire's), and only runs while it has a target.
 * <p>
 * The spell's phantoms rise here too, as a summoned court: marked with when they go, and kept until then.
 */
public final class PhantomCalling {
    static final String COUNTDOWN_KEY = "dreadpatch:phantom_call";
    /** How far round a summoner its phantoms are counted. */
    static final double ROSTER_RANGE = 128.0;

    private static final ParticleOptions CHARCOAL = new DustParticleOptions(new Vector3f(0.078F, 0.086F, 0.114F), 1.6F);

    private PhantomCalling() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(PhantomCalling::onTick);
    }

    /** Whether {@code mob} calls dread phantoms: the queen, a Dread Lich, or Syndrigossa while she's wild and alive. */
    public static boolean calls(Entity mob) {
        return mob instanceof DreadQueen || mob instanceof DreadLichEntity
                || mob instanceof Syndrigossa dragon && !dragon.isTame() && !dragon.isModelDead();
    }

    private static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob summoner) || !(summoner.level() instanceof ServerLevel level) || !calls(summoner)
                || !summoner.isAlive() || summoner.isNoAi()) {
            return;
        }
        LivingEntity target = summoner.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        CompoundTag data = summoner.getPersistentData();
        int countdown = data.contains(COUNTDOWN_KEY) ? data.getInt(COUNTDOWN_KEY) : PhantomRules.FIRST_CALL;
        if (--countdown <= 0) {
            int wanted = PhantomRules.toCall(standing(level, summoner), 1 + summoner.getRandom().nextInt(2));
            // A full roster spends the call; nowhere to rise, it tries again shortly.
            countdown = wanted == 0 || call(level, summoner, target, wanted) > 0 ? PhantomRules.CALL_COOLDOWN : PhantomRules.CALL_RETRY;
        }
        data.putInt(COUNTDOWN_KEY, countdown);
    }

    /** How many of {@code summoner}'s phantoms are alive round it. */
    static int standing(ServerLevel level, Entity summoner) {
        return level.getEntitiesOfClass(DreadPhantom.class, summoner.getBoundingBox().inflate(ROSTER_RANGE),
                phantom -> phantom.isAlive() && summoner.getUUID().equals(phantom.getSummonerId())).size();
    }

    /** {@code count} phantoms rise over {@code summoner}, set on {@code target}. How many rose. */
    public static int call(ServerLevel level, Mob summoner, LivingEntity target, int count) {
        int called = 0;
        for (int i = 0; i < count; i++) {
            DreadPhantom phantom = rise(level, summoner, -1L);
            if (phantom != null) {
                phantom.setTarget(target);
                called++;
            }
        }
        return called;
    }

    /**
     * One phantom rises over {@code summoner}, theirs. With {@code expires} at or past 0, it's a summoned
     * court's: kept (not despawning) until that game time, when it goes. Null if there was no room.
     */
    @Nullable
    public static DreadPhantom rise(ServerLevel level, LivingEntity summoner, long expires) {
        DreadPhantom phantom = ModRegistries.DREAD_PHANTOM.get().create(level);
        if (phantom == null) {
            return null;
        }
        Vec3 spot = findSpot(level, summoner, phantom);
        if (spot == null) {
            return null;
        }
        phantom.moveTo(spot.x, spot.y, spot.z, summoner.getYRot(), 0.0F);
        EventHooks.finalizeMobSpawn(phantom, level, level.getCurrentDifficultyAt(phantom.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        phantom.setSummoner(summoner);
        if (expires >= 0) {
            phantom.getPersistentData().putLong(SummonedCourt.EXPIRES_KEY, expires);
            phantom.setPersistenceRequired();
        }
        if (!level.addFreshEntity(phantom)) {
            return null;
        }
        MagicManager.spawnParticles(level, CHARCOAL, spot.x, spot.y + 0.2, spot.z, 16, 0.5, 0.2, 0.5, 0.02, false);
        MagicManager.spawnParticles(level, ParticleTypes.SOUL, spot.x, spot.y + 0.2, spot.z, 6, 0.4, 0.2, 0.4, 0.02, false);
        MagicManager.spawnParticles(level, ParticleTypes.SNOWFLAKE, spot.x, spot.y + 0.2, spot.z, 10, 0.5, 0.3, 0.5, 0.02, false);
        level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.PHANTOM_AMBIENT, SoundSource.HOSTILE, 1.5F, 0.8F);
        level.playSound(null, spot.x, spot.y, spot.z, ModRegistries.DREAD_PHANTOM_WAIL.get(), SoundSource.HOSTILE, 1.5F, 0.9F);
        return phantom;
    }

    /** Open air 3–6 blocks over the summoner, up to 3 to the side. */
    @Nullable
    private static Vec3 findSpot(ServerLevel level, LivingEntity summoner, DreadPhantom phantom) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double x = summoner.getX() + (summoner.getRandom().nextDouble() - 0.5) * 6.0;
            double y = summoner.getY() + summoner.getBbHeight() + 1.0 + summoner.getRandom().nextDouble() * 3.0;
            double z = summoner.getZ() + (summoner.getRandom().nextDouble() - 0.5) * 6.0;
            phantom.setPos(x, y, z);
            if (level.noCollision(phantom) && !level.containsAnyLiquid(phantom.getBoundingBox())) {
                return new Vec3(x, y, z);
            }
        }
        return null;
    }
}
