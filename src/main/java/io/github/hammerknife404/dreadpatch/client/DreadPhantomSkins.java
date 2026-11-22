package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Function;

/**
 * The dread phantom's skin and eyes (0.45.0), made on the render thread the first time it's drawn from
 * vanilla's phantom textures ({@link DreadPhantomSkin}). Nothing of Mojang's ships with us, and a
 * resource pack's phantom carries through. Remade after a resource reload; if the textures can't be
 * read, it wears vanilla's.
 */
final class DreadPhantomSkins {
    static final ResourceLocation PHANTOM = ResourceLocation.withDefaultNamespace("textures/entity/phantom.png");
    static final ResourceLocation PHANTOM_EYES = ResourceLocation.withDefaultNamespace("textures/entity/phantom_eyes.png");

    private static ResourceLocation skin;
    private static ResourceLocation eyes;

    private DreadPhantomSkins() {
    }

    static ResourceLocation skin() {
        if (skin == null) {
            skin = make(PHANTOM, "dread_phantom", pixels -> DreadPhantomSkin.skin(pixels[0], pixels[1][0], pixels[1][1]));
        }
        return skin;
    }

    static ResourceLocation eyes() {
        if (eyes == null) {
            eyes = make(PHANTOM_EYES, "dread_phantom_eyes", pixels -> DreadPhantomSkin.eyes(pixels[0]));
        }
        return eyes;
    }

    /** After a resource reload: forget them, to be made again from whatever textures are loaded now. */
    static void clear() {
        for (ResourceLocation made : new ResourceLocation[]{skin, eyes}) {
            if (made != null && made.getNamespace().equals(DreadPatch.MODID)) {
                Minecraft.getInstance().getTextureManager().release(made);
            }
        }
        skin = null;
        eyes = null;
    }

    /** Vanilla's texture through {@code change} ({pixels}, {width, height}), registered as ours; vanilla's own if that fails. */
    private static ResourceLocation make(ResourceLocation from, String name, Function<int[][], int[]> change) {
        try (NativeImage in = read(from)) {
            int width = in.getWidth();
            int height = in.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y * width + x] = in.getPixelRGBA(x, y);
                }
            }
            int[] made = change.apply(new int[][]{pixels, {width, height}});
            NativeImage out = new NativeImage(width, height, false);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    out.setPixelRGBA(x, y, made[y * width + x]);
                }
            }
            ResourceLocation location = DreadPatch.id("dynamic/" + name);
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(out));
            return location;
        } catch (IOException | RuntimeException e) {
            DreadPatch.LOGGER.warn("Couldn't make the dread phantom's {} from {}; it'll wear vanilla's", name, from, e);
            return from;
        }
    }

    private static NativeImage read(ResourceLocation location) throws IOException {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
        if (resource.isEmpty()) {
            throw new IOException("no texture " + location);
        }
        try (InputStream in = resource.get().open()) {
            return NativeImage.read(in);
        }
    }
}
