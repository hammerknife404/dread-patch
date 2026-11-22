package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Rimefrost dragons' skins (Syndrigossa's among them), made on the render thread the first time each
 * is drawn: Ice and Fire's ice dragon texture in the dragon's colour for its stage (awake or asleep),
 * rotted with its ice dragon skeleton ({@link UndeadSkin}). Nothing of Ice and Fire's is copied into
 * this mod; resource packs that retexture its ice dragons retexture them too. Remade after a resource
 * reload.
 */
final class UndeadDragonSkins {
    private static final Map<String, ResourceLocation> MADE = new HashMap<>();

    private UndeadDragonSkins() {
    }

    static ResourceLocation get(String color, int stage, boolean sleeping) {
        int s = Math.max(1, Math.min(5, stage));
        String key = "rimefrost_" + color + "_" + s + (sleeping ? "_sleeping" : "");
        ResourceLocation made = MADE.get(key);
        if (made != null) {
            return made;
        }
        ResourceLocation body = iceDragon(color + "_" + s + (sleeping ? "_sleeping" : "") + ".png");
        ResourceLocation bones = iceDragon("ice_skeleton_" + s + ".png");
        try (NativeImage bodyImage = read(body); NativeImage bonesImage = read(bones)) {
            int width = bodyImage.getWidth();
            int height = bodyImage.getHeight();
            if (bonesImage.getWidth() != width || bonesImage.getHeight() != height) {
                throw new IOException("skeleton " + bones + " isn't the size of " + body);
            }
            int[] skin = UndeadSkin.compose(pixels(bodyImage), pixels(bonesImage), width, height);
            NativeImage out = new NativeImage(width, height, false);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    out.setPixelRGBA(x, y, skin[y * width + x]);
                }
            }
            made = DreadPatch.id("dynamic/" + key);
            Minecraft.getInstance().getTextureManager().register(made, new DynamicTexture(out));
        } catch (IOException | RuntimeException e) {
            DreadPatch.LOGGER.warn("Couldn't make a Rimefrost dragon's skin from {} and {}; it'll wear Ice and Fire's", body, bones, e);
            made = body;
        }
        MADE.put(key, made);
        return made;
    }

    /** After a resource reload: forget the skins, to be made again from whatever textures are loaded now. */
    static void clear() {
        for (ResourceLocation made : MADE.values()) {
            if (made.getNamespace().equals(DreadPatch.MODID)) {
                Minecraft.getInstance().getTextureManager().release(made);
            }
        }
        MADE.clear();
    }

    private static ResourceLocation iceDragon(String file) {
        return ResourceLocation.fromNamespaceAndPath("iceandfire", "textures/entity/icedragon/" + file);
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

    private static int[] pixels(NativeImage image) {
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                pixels[y * image.getWidth() + x] = image.getPixelRGBA(x, y);
            }
        }
        return pixels;
    }
}
