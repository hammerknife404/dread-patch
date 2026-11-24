package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.world.Dreadlands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Optional;

/**
 * The Dreadlands' sky (0.67.0, the owner: "Dim the sunlight to never brighter than 6, and the suns texture/brightness
 * map reflects this"): its dimension type's effects ({@code dreadpatch:dreadlands}, the overworld's otherwise).
 * <ul>
 * <li>The lightmap: the sky's share of every light level over {@link #MAX_SKY} is cut to what level 6 gives, so by
 * day the open ground looks no brighter than sky light 6 (block light, torches, still shows as itself). How things
 * are drawn only: mobs, spawning and melting still see vanilla's sky light.</li>
 * <li>The sun: while the camera's in the Dreadlands, vanilla's sun texture is swapped for a dim pale cyan copy of
 * itself ({@link #SUN_BRIGHTNESS} of its light; the sun is drawn additively, so darker is dimmer), and put back on
 * leaving or a resource reload.</li>
 * </ul>
 * Shader packs (Iris) draw their own sky and light, and likely ignore both.
 */
public final class DreadlandsSky extends DimensionSpecialEffects.OverworldEffects {
    public static final int MAX_SKY = 6;
    static final float SUN_BRIGHTNESS = 0.42F;
    /** Pale cyan, as her dread fire is cyan. */
    private static final float[] SUN_TINT = {0.62F, 0.93F, 1.0F};
    private static final ResourceLocation SUN = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean swapped;

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener((RegisterDimensionSpecialEffectsEvent event) -> event.register(DreadPatch.id("dreadlands"), new DreadlandsSky()));
        modBus.addListener((RegisterClientReloadListenersEvent event) -> event.registerReloadListener((ResourceManagerReloadListener) manager -> swapped = false));
        gameBus.addListener(DreadlandsSky::onClientTick);
    }

    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker, float skyLight,
                                     int pixelX, int pixelY, Vector3f colors) {
        if (pixelY <= MAX_SKY) {
            return;
        }
        // Vanilla's sky colour for this level of sky light, less the part over level 6's.
        float capped = skyLight * LightTexture.getBrightness(level.dimensionType(), MAX_SKY) / LightTexture.getBrightness(level.dimensionType(), pixelY);
        Vector3f sky = new Vector3f(skyDarken, skyDarken, 1.0F).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F).mul(skyLight - capped);
        colors.sub(sky).max(new Vector3f(0.0F));
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean here = minecraft.level != null && Dreadlands.is(minecraft.level);
        if (here && !swapped) {
            swapped = true;
            dimSun(minecraft);
        } else if (!here && swapped) {
            swapped = false;
            minecraft.getTextureManager().release(SUN);
        }
    }

    /** Vanilla's sun (or a pack's), each pixel tinted pale cyan and dimmed; registered in its place. */
    private static void dimSun(Minecraft minecraft) {
        Optional<Resource> resource = minecraft.getResourceManager().getResource(SUN);
        if (resource.isEmpty()) {
            return;
        }
        try (InputStream in = resource.get().open()) {
            NativeImage image = NativeImage.read(in);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    int a = abgr >>> 24;
                    float light = Math.max(abgr & 0xFF, Math.max(abgr >> 8 & 0xFF, abgr >> 16 & 0xFF)) / 255.0F * SUN_BRIGHTNESS;
                    int r = Math.round(255 * light * SUN_TINT[0]);
                    int g = Math.round(255 * light * SUN_TINT[1]);
                    int b = Math.round(255 * light * SUN_TINT[2]);
                    image.setPixelRGBA(x, y, a << 24 | b << 16 | g << 8 | r);
                }
            }
            minecraft.getTextureManager().register(SUN, new DynamicTexture(image));
        } catch (Exception e) {
            LOGGER.warn("Couldn't dim the Dreadlands' sun: {}", e.toString());
        }
    }
}
