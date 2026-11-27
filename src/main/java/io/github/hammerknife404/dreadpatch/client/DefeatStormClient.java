package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import io.github.hammerknife404.dreadpatch.world.Escalation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The defeat storm on a client (0.62.0, {@link Escalation}): while it lasts, out under the sky in a cold
 * biome (or anywhere, by the server's setting), an ash-grey fog closes in to {@link #FOG_FAR} (a warning,
 * not the Dreadlands' storms' 24, nor Whiteout's 10), rime is blown along the ground on the day's wind,
 * and {@link DreadlandsAmbience} plays the Dreadlands' wind and ice cracking (not its rumbles, whispers or
 * roars, and not over a Whiteout Dominion's own wind). Her crown keeps its fog off, as the Dreadlands'.
 */
public final class DefeatStormClient {
    static final float FOG_FAR = 48.0F;
    /** Ash grey. */
    private static final float FOG_R = 0.45F;
    private static final float FOG_G = 0.44F;
    private static final float FOG_B = 0.43F;
    private static final float FADE = 1.0F / 40.0F;
    private static final int RIME = 5;

    /** Its end, in game time (0: none). */
    private static long until;
    /** The overworld's dread weather (0.89.0): on, and whether it reaches the hot lands; drawn as the Dreadlands' snow. */
    private static boolean dreadWeather;
    private static boolean dreadWeatherHot;
    private static boolean dreadExposed;
    private static boolean exposed;
    private static float strength;
    private static float lastStrength;

    private DefeatStormClient() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DefeatStormClient::onClientTick);
        gameBus.addListener(DefeatStormClient::onRenderFog);
        gameBus.addListener(DefeatStormClient::onFogColor);
        gameBus.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            until = 0L;
            dreadWeather = false;
        });
    }

    /** From the server: it lasts until {@code end} (game time). */
    public static void set(long end) {
        until = end;
    }

    /** From the server: the overworld's dread weather on or off (0.89.0). */
    public static void setDreadWeather(boolean on, boolean hot) {
        dreadWeather = on;
        dreadWeatherHot = hot;
    }

    /** Whether it reaches the camera now (no easing). */
    static boolean exposed() {
        return exposed;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Entity camera = minecraft.getCameraEntity();
        lastStrength = strength;
        if (level == null || camera == null) {
            exposed = false;
            strength = lastStrength = 0.0F;
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        BlockPos eye = BlockPos.containing(camera.getEyePosition());
        boolean sky = level.dimension() == Level.OVERWORLD && level.canSeeSky(eye);
        dreadExposed = sky && dreadWeather && level.isRaining()
                && io.github.hammerknife404.dreadpatch.world.DreadWeather.reaches(level, camera.blockPosition(), dreadWeatherHot);
        exposed = dreadExposed || sky && level.getGameTime() < until
                && (!DreadPatchConfig.defeatStormColdOnly() || Escalation.cold(level, camera.blockPosition()));
        strength = Mth.clamp(strength + (exposed ? FADE : -FADE), 0.0F, 1.0F);
        if (strength > 0.0F) {
            blowRime(level, camera.getEyePosition());
        }
    }

    /** Rime driven along the ground on the wind, which turns with the days (as the Dreadlands' snow). */
    private static void blowRime(ClientLevel level, Vec3 eye) {
        RandomSource random = level.random;
        double angle = level.getDayTime() / 24000.0 * 2.4;
        int count = Math.round(RIME * strength);
        for (int i = 0; i < count; i++) {
            int x = Mth.floor(eye.x + (random.nextDouble() - 0.5) * 28.0);
            int z = Mth.floor(eye.z + (random.nextDouble() - 0.5) * 28.0);
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (Math.abs(ground - eye.y) > 10.0) {
                continue;
            }
            double gust = 0.25 + random.nextDouble() * 0.3;
            level.addParticle(ParticleTypes.SNOWFLAKE, x + random.nextDouble(), ground + 0.1 + random.nextDouble() * 1.2, z + random.nextDouble(),
                    Math.cos(angle) * gust, 0.0, Math.sin(angle) * gust);
        }
    }

    /** How bad it is round the camera (for {@link SoundFilters}). */
    static float severity() {
        return strength * io.github.hammerknife404.dreadpatch.magic.SoundRules.DEFEAT_STORM;
    }

    private static float strength(float partialTick) {
        return Mth.lerp(partialTick, lastStrength, strength);
    }

    /** Never further than it was: the Dreadlands', Whiteout's, or anything else's closer fog still wins. */
    private static void onRenderFog(ViewportEvent.RenderFog event) {
        float amount = strength((float) event.getPartialTick());
        if (amount <= 0.0F || event.getType() != FogType.NONE || CrownSight.clearsStorms()) {
            return;
        }
        float far = event.getFarPlaneDistance();
        // The dread weather closes in as the Dreadlands' snow does (thicker in thunder); her defeat's to 48.
        ClientLevel level = Minecraft.getInstance().level;
        float thunder = level == null ? 0.0F : level.getThunderLevel((float) event.getPartialTick());
        float to = dreadExposed ? Mth.lerp(thunder, io.github.hammerknife404.dreadpatch.magic.StormRules.FOG_FAR_SNOW,
                io.github.hammerknife404.dreadpatch.magic.StormRules.FOG_FAR) : FOG_FAR;
        event.setFarPlaneDistance(Math.min(far, Mth.lerp(amount, far, Math.min(to, far))));
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), Mth.lerp(amount, event.getNearPlaneDistance(), 0.0F)));
        event.setCanceled(true);
    }

    private static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float amount = strength((float) event.getPartialTick());
        ClientLevel level = Minecraft.getInstance().level;
        if (amount <= 0.0F || level == null || CrownSight.clearsStorms() || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float light = level.getSkyDarken((float) event.getPartialTick());
        event.setRed(Mth.lerp(amount, event.getRed(), FOG_R * light));
        event.setGreen(Mth.lerp(amount, event.getGreen(), FOG_G * light));
        event.setBlue(Mth.lerp(amount, event.getBlue(), FOG_B * light));
    }
}
