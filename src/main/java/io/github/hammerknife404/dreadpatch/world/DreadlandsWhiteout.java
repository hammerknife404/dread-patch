package io.github.hammerknife404.dreadpatch.world;

import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import io.github.hammerknife404.dreadpatch.magic.WeatherRules;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The Dreadlands' Whiteout Dominion weather (0.54.0): as a storm starts there (the overworld's rain), about
 * one in six ({@link WeatherRules}; the server config's {@code whiteout_storms}) is a whiteout, never two in
 * a row and never while a Dread Queen is alive there. While it lasts, everything under the open sky but the
 * Dread gets Whiteout (fog to 10, slowed, as in her storm; the storm's Rimefrost still settles too,
 * {@link DreadlandsStorms}) and a point of freezing damage every 3 s, unless it can't freeze. It doesn't
 * raise the dead or lay a snow cover: those are her storm's. A queen coming into the Dreadlands (her fight
 * starting) breaks it into a thunderstorm, the overworld's, since the two share their weather.
 */
public final class DreadlandsWhiteout {
    private DreadlandsWhiteout() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadlandsWhiteout::onLevelTick);
        gameBus.addListener(DreadlandsWhiteout::onJoin);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Dreadlands.is(level) || level.getGameTime() % WeatherRules.EFFECT_EVERY != 0) {
            return;
        }
        DreadlandsData data = DreadlandsData.get(level.getServer());
        WeatherRules.Storms storms = data.storms();
        boolean raining = level.getLevelData().isRaining();
        if (storms.storming != raining) {
            boolean queen = raining && queenAbout(level);
            storms.update(raining, queen, DreadPatchConfig.whiteoutOneIn(), n -> level.getRandom().nextInt(n));
            data.setDirty();
        }
        if (storms.whiteout && level.isRaining()) {
            blind(level, level.getGameTime() % WeatherRules.FREEZE_EVERY == 0);
        }
    }

    /** Whiteout on everything under the open sky but the Dread, and, if {@code freeze}, its point of freezing damage. */
    private static void blind(ServerLevel level, boolean freeze) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living && living.isAlive() && !(living instanceof IDreadMob) && !living.isSpectator()
                    && DreadlandsStorms.exposed(level, living)) {
                living.addEffect(new MobEffectInstance(ModRegistries.WHITEOUT, WeatherRules.EFFECT_TICKS, 0, true, true));
                if (freeze && living.canFreeze()) {
                    living.hurt(level.damageSources().freeze(), WeatherRules.FREEZE_DAMAGE);
                }
            }
        }
    }

    /** Whether a Dread Queen is alive in the Dreadlands. */
    private static boolean queenAbout(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof DreadQueen queen && queen.isAlive()) {
                return true;
            }
        }
        return false;
    }

    /** A queen coming into the Dreadlands during a whiteout: it breaks, into a heavy thunderstorm. */
    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && Dreadlands.is(level) && event.getEntity() instanceof DreadQueen queen && queen.isAlive()) {
            DreadlandsData data = DreadlandsData.get(level.getServer());
            if (data.storms().breakWhiteout()) {
                data.setDirty();
                thunder(level.getServer());
            }
        }
    }

    /** Thunder, for the rest of the storm and at least {@link WeatherRules#THUNDER_AT_LEAST}. */
    static void thunder(MinecraftServer server) {
        int left = server.getWorldData().overworldData().getRainTime();
        server.overworld().setWeatherParameters(0, Math.max(left, WeatherRules.THUNDER_AT_LEAST), true, true);
    }
}
