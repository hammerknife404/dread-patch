package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.shaders.FogShape;
import io.github.hammerknife404.dreadpatch.magic.StormRules;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * A Dreadlands storm as the player sees it (0.48.0): under the open sky while it snows there, the fog
 * closes in to {@link StormRules#FOG_FAR} blocks (Whiteout Dominion's to 10: this one's a warning),
 * storm-grey by day and dark by night, coming and going with the snow and over a couple of seconds as
 * you go in under cover and out again; and flakes blow past on the day's wind, three times as thick and
 * twice as fast in its whiteout weather (0.54.0; its fog and wind are {@link WhiteoutClient}'s).
 */
public final class DreadlandsClient {
    private static final float FOG_R = 0.62F;
    private static final float FOG_G = 0.66F;
    private static final float FOG_B = 0.72F;
    /** How fast the fog follows the snow and the sky over you: per tick. */
    private static final float FADE = 1.0F / 40.0F;
    private static final int FLAKES = 6;

    private static float storm;
    private static float lastStorm;
    /** How far into a whiteout the camera is, 0 to 1: Whiteout on it, in the Dreadlands. */
    private static float whiteout;

    private DreadlandsClient() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadlandsClient::onClientTick);
        gameBus.addListener(DreadlandsClient::onRenderFog);
        gameBus.addListener(DreadlandsClient::onFogColor);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Entity camera = minecraft.getCameraEntity();
        lastStorm = storm;
        if (level == null || camera == null || minecraft.isPaused()) {
            if (level == null) {
                storm = lastStorm = whiteout = 0.0F;
            }
            return;
        }
        boolean out = level.dimension() == Dreadlands.LEVEL && level.canSeeSky(BlockPos.containing(camera.getEyePosition()));
        float target = out ? level.getRainLevel(1.0F) : 0.0F;
        storm = storm < target ? Math.min(target, storm + FADE) : Math.max(target, storm - FADE);
        boolean blinded = out && camera instanceof LivingEntity living && living.hasEffect(ModRegistries.WHITEOUT);
        whiteout = Mth.clamp(whiteout + (blinded ? FADE : -FADE), 0.0F, 1.0F);
        if (storm > 0.0F) {
            blowFlakes(level, camera.getEyePosition());
        }
    }

    /** Flakes driven past on the wind, which turns with the days. */
    private static void blowFlakes(ClientLevel level, Vec3 eye) {
        RandomSource random = level.random;
        double angle = level.getDayTime() / 24000.0 * 2.4;
        int count = Math.round(FLAKES * storm * (1.0F + 2.0F * whiteout));
        for (int i = 0; i < count; i++) {
            Vec3 at = eye.add((random.nextDouble() - 0.5) * 16.0, (random.nextDouble() - 0.4) * 8.0, (random.nextDouble() - 0.5) * 16.0);
            if (level.getBlockState(BlockPos.containing(at)).isAir()) {
                double gust = (0.15 + random.nextDouble() * 0.2) * (1.0 + whiteout);
                level.addParticle(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, Math.cos(angle) * gust, -0.04 - random.nextDouble() * 0.05, Math.sin(angle) * gust);
            }
        }
    }

    /** How bad its weather is round the camera (for {@link SoundFilters}): snow, more in thunder, all in a whiteout. */
    static float severity() {
        ClientLevel level = Minecraft.getInstance().level;
        float thunder = level == null ? 0.0F : level.getThunderLevel(1.0F);
        return Math.max(storm * Mth.lerp(thunder, io.github.hammerknife404.dreadpatch.magic.SoundRules.SNOW,
                io.github.hammerknife404.dreadpatch.magic.SoundRules.THUNDER), whiteout * io.github.hammerknife404.dreadpatch.magic.SoundRules.WHITEOUT);
    }

    private static float storm(float partialTick) {
        return Mth.lerp(partialTick, lastStorm, storm);
    }

    /** Never further than it was: Whiteout's closer fog, or anything else's, still wins. */
    private static void onRenderFog(ViewportEvent.RenderFog event) {
        float amount = storm((float) event.getPartialTick());
        if (amount <= 0.0F || event.getType() != FogType.NONE || CrownSight.clearsStorms()) {
            return;
        }
        float far = event.getFarPlaneDistance();
        ClientLevel level = Minecraft.getInstance().level;
        float thunder = level == null ? 0.0F : level.getThunderLevel((float) event.getPartialTick());
        float storm = Mth.lerp(thunder, StormRules.FOG_FAR_SNOW, StormRules.FOG_FAR);
        event.setFarPlaneDistance(Math.min(far, Mth.lerp(amount, far, Math.min(storm, far))));
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), Mth.lerp(amount, event.getNearPlaneDistance(), 0.0F)));
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    private static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float amount = storm((float) event.getPartialTick());
        ClientLevel level = Minecraft.getInstance().level;
        if (CrownSight.clearsStorms()) {
            return;
        }
        if (amount <= 0.0F || level == null || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float light = level.getSkyDarken((float) event.getPartialTick());
        event.setRed(Mth.lerp(amount, event.getRed(), FOG_R * light));
        event.setGreen(Mth.lerp(amount, event.getGreen(), FOG_G * light));
        event.setBlue(Mth.lerp(amount, event.getBlue(), FOG_B * light));
    }
}
