package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.shaders.FogShape;
import io.github.hammerknife404.dreadpatch.entity.WhiteoutDominion;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.github.hammerknife404.dreadpatch.world.Dreadlands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Whiteout Dominion as its victims see it. Under Whiteout (its effect, on everyone it may harm) the
 * fog closes to {@link #FOG_FAR} blocks, snow-grey by day and dark by night; it comes and goes over a
 * second and a half. With the camera inside any storm, the wind howls (vanilla's soul sand valley
 * loop) and flakes blow past. Its snow and its wall are {@link WhiteoutRenderer}'s.
 */
public final class WhiteoutClient {
    /** How far a player under Whiteout sees. */
    static final float FOG_FAR = 10.0F;
    /** Its fog's colour in full daylight. */
    private static final float FOG_R = 0.78F;
    private static final float FOG_G = 0.81F;
    private static final float FOG_B = 0.85F;
    /** How fast the fog, and the wind, come and go: per tick. */
    private static final float FADE = 1.0F / 30.0F;
    /** Flakes a tick round the camera inside a storm. */
    private static final int FLAKES = 10;

    private static float fog;
    private static float lastFog;
    private static float inside;
    private static float lastInside;
    @Nullable
    private static WhiteoutWindSound wind;

    private WhiteoutClient() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(WhiteoutClient::onClientTick);
        gameBus.addListener(WhiteoutClient::onRenderFog);
        gameBus.addListener(WhiteoutClient::onFogColor);
        gameBus.addListener(WhiteoutRenderer::drawStorms);
    }

    /** Each storm's wind: a direction along the ground, fixed for the storm (from its UUID, so every client agrees). */
    static Vec3 wind(WhiteoutDominion storm) {
        double angle = (storm.getUUID().getLeastSignificantBits() & 0xFFFF) / 65536.0 * Math.PI * 2;
        return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
    }

    /** How loud the wind is now, 0 to 1. */
    static float windStrength() {
        return inside;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Entity camera = minecraft.getCameraEntity();
        lastFog = fog;
        lastInside = inside;
        if (level == null || camera == null || minecraft.isPaused()) {
            if (level == null) {
                fog = lastFog = inside = lastInside = 0.0F;
            }
            return;
        }
        boolean blinded = camera instanceof LivingEntity living && living.hasEffect(ModRegistries.WHITEOUT);
        fog = Mth.clamp(fog + (blinded ? FADE : -FADE), 0.0F, 1.0F);
        WhiteoutDominion storm = stormAround(level, camera.getEyePosition());
        float target = storm == null ? 0.0F : storm.strength(0.0F);
        if (blinded && level.dimension() == Dreadlands.LEVEL) {
            // The Dreadlands' whiteout weather (0.54.0): its wind wherever it blinds you there.
            target = Math.max(target, level.getRainLevel(1.0F));
        }
        inside = inside < target ? Math.min(target, inside + FADE) : Math.max(target, inside - FADE);
        if (storm != null) {
            blowFlakes(level, storm, camera.getEyePosition());
        }
        if (inside > 0.0F && (wind == null || wind.isStopped())) {
            wind = new WhiteoutWindSound();
            minecraft.getSoundManager().play(wind);
        }
    }

    /** The storm the point is in, if any. */
    @Nullable
    private static WhiteoutDominion stormAround(ClientLevel level, Vec3 point) {
        for (WhiteoutDominion storm : level.getEntitiesOfClass(WhiteoutDominion.class, new AABB(point, point).inflate(72.0))) {
            if (storm.holds(point)) {
                return storm;
            }
        }
        return null;
    }

    /** Flakes driven past the camera on the storm's wind, only where the storm is. */
    private static void blowFlakes(ClientLevel level, WhiteoutDominion storm, Vec3 eye) {
        RandomSource random = level.random;
        Vec3 wind = wind(storm);
        int count = Math.round(FLAKES * storm.strength(0.0F));
        for (int i = 0; i < count; i++) {
            Vec3 at = eye.add((random.nextDouble() - 0.5) * 16.0, (random.nextDouble() - 0.4) * 8.0, (random.nextDouble() - 0.5) * 16.0);
            if (storm.holds(at) && level.getBlockState(BlockPos.containing(at)).isAir()) {
                double gust = 0.2 + random.nextDouble() * 0.25;
                level.addParticle(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, wind.x * gust, -0.04 - random.nextDouble() * 0.06, wind.z * gust);
            }
        }
    }

    private static float fog(float partialTick) {
        return Mth.lerp(partialTick, lastFog, fog);
    }

    /** Under Whiteout, in the open air: the fog closes in round the camera, every way. */
    private static void onRenderFog(ViewportEvent.RenderFog event) {
        float amount = fog((float) event.getPartialTick());
        if (amount <= 0.0F || event.getType() != FogType.NONE) {
            return;
        }
        event.setFarPlaneDistance(Mth.lerp(amount, event.getFarPlaneDistance(), Math.min(FOG_FAR, event.getFarPlaneDistance())));
        event.setNearPlaneDistance(Mth.lerp(amount, event.getNearPlaneDistance(), 0.0F));
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    /** Its fog is snow-grey, as bright as the sky would be: pale by day, dark by night. */
    private static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float amount = fog((float) event.getPartialTick());
        ClientLevel level = Minecraft.getInstance().level;
        if (amount <= 0.0F || level == null || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float light = level.getSkyDarken((float) event.getPartialTick());
        event.setRed(Mth.lerp(amount, event.getRed(), FOG_R * light));
        event.setGreen(Mth.lerp(amount, event.getGreen(), FOG_G * light));
        event.setBlue(Mth.lerp(amount, event.getBlue(), FOG_B * light));
    }
}
