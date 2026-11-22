package io.github.hammerknife404.dreadpatch.client;

import com.iafenvoy.iceandfire.render.model.armor.DragonSteelIceArmorModel;
import com.iafenvoy.iceandfire.render.model.armor.IceDragonScaleArmorModel;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.item.DreadArmorItem;
import io.github.hammerknife404.dreadpatch.magic.DreadRecolor;
import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Rimefrost scale's and dread dragonsteel's looks (0.42.0), made from Ice and Fire's ice art as it
 * loads ({@link DreadRecolor}), so nothing of Ice and Fire's ships with us and resource packs that
 * retexture its ice gear retexture ours:
 * <ul>
 * <li>item icons by a sprite source ({@code dreadpatch:dread_recolor}, in {@code atlases/blocks.json}),
 * each an Ice and Fire item sprite recoloured as the atlas is stitched;</li>
 * <li>armour layers as dynamic textures, made the first time each is drawn (as Syndrigossa's skins are),
 * worn on Ice and Fire's own ice dragonscale and ice dragonsteel armour models.</li>
 * </ul>
 */
public final class DreadRecolors {
    private static final Map<String, ResourceLocation> MADE = new HashMap<>();
    private static final Map<DreadArmorItem.Kind, HumanoidModel<LivingEntity>> OUTER = new EnumMap<>(DreadArmorItem.Kind.class);
    private static final Map<DreadArmorItem.Kind, HumanoidModel<LivingEntity>> INNER = new EnumMap<>(DreadArmorItem.Kind.class);
    static SpriteSourceType type;

    private DreadRecolors() {
    }

    /** One recolouring: an Ice and Fire sprite in, ours out. */
    record Entry(ResourceLocation from, ResourceLocation to) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("from").forGetter(Entry::from),
                ResourceLocation.CODEC.fieldOf("to").forGetter(Entry::to)).apply(i, Entry::new));
    }

    /** {@code dreadpatch:dread_recolor}: each entry's sprite, recoloured, under its new name. */
    record Source(List<Entry> entries) implements SpriteSource {
        static final MapCodec<Source> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Entry.CODEC.listOf().fieldOf("entries").forGetter(Source::entries)).apply(i, Source::new));

        @Override
        public void run(ResourceManager resources, Output output) {
            for (Entry entry : this.entries) {
                Optional<Resource> resource = resources.getResource(TEXTURE_ID_CONVERTER.idToFile(entry.from()));
                if (resource.isEmpty()) {
                    DreadPatch.LOGGER.warn("No sprite {} to make {} from", entry.from(), entry.to());
                    continue;
                }
                output.add(entry.to(), loader -> loader.loadSprite(entry.to(), resource.get(), (id, size, image, metadata) -> {
                    recolor(image);
                    return new SpriteContents(id, size, image, metadata);
                }));
            }
        }

        @Override
        public SpriteSourceType type() {
            return type;
        }
    }

    static void onRegisterSpriteSources(RegisterSpriteSourceTypesEvent event) {
        type = new SpriteSourceType(Source.CODEC);
        event.register(DreadPatch.id("dread_recolor"), type);
    }

    /** Ice and Fire's ice armour models for ours (Ice and Fire hands its own to Uranus' renderer, which ours aren't in). */
    static void onClientExtensions(RegisterClientExtensionsEvent event) {
        for (var item : DreadGear.armor()) {
            DreadArmorItem.Kind kind = item.get().kind();
            event.registerItem(new IClientItemExtensions() {
                @Override
                public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                    return model(kind, slot == EquipmentSlot.LEGS);
                }
            }, item.get());
        }
        DreadArmorItem.textures = DreadRecolors::armorTexture;
    }

    private static HumanoidModel<LivingEntity> model(DreadArmorItem.Kind kind, boolean inner) {
        return (inner ? INNER : OUTER).computeIfAbsent(kind, k -> k == DreadArmorItem.Kind.SCALE ? new IceDragonScaleArmorModel(inner) : new DragonSteelIceArmorModel(inner));
    }

    /** An armour layer, recoloured from Ice and Fire's: made the first time it's drawn; Ice and Fire's own if it can't be. */
    static ResourceLocation armorTexture(DreadArmorItem.Kind kind, boolean inner) {
        String key = kind.name().toLowerCase() + "_layer_" + (inner ? 2 : 1);
        ResourceLocation made = MADE.get(key);
        if (made != null) {
            return made;
        }
        ResourceLocation source = ResourceLocation.fromNamespaceAndPath("iceandfire", "textures/models/armor/" + kind.iceAndFireTexture + "_layer_" + (inner ? 2 : 1) + ".png");
        try (NativeImage image = read(source)) {
            recolor(image);
            NativeImage copy = new NativeImage(image.getWidth(), image.getHeight(), false);
            copy.copyFrom(image);
            made = DreadPatch.id("dynamic/armor/" + key);
            Minecraft.getInstance().getTextureManager().register(made, new DynamicTexture(copy));
        } catch (IOException | RuntimeException e) {
            DreadPatch.LOGGER.warn("Couldn't make dread armour from {}; it'll wear Ice and Fire's", source, e);
            made = source;
        }
        MADE.put(key, made);
        return made;
    }

    /** After a resource reload: forget the armour textures, to be made again from what's loaded now. */
    static void clear() {
        for (ResourceLocation made : MADE.values()) {
            if (made.getNamespace().equals(DreadPatch.MODID)) {
                Minecraft.getInstance().getTextureManager().release(made);
            }
        }
        MADE.clear();
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

    /** {@link DreadRecolor} over a {@link NativeImage}, which keeps its pixels ABGR. */
    static void recolor(NativeImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getPixelRGBA(x, y);
                int argb = abgr & 0xFF00FF00 | (abgr & 0xFF) << 16 | abgr >> 16 & 0xFF;
                int out = DreadRecolor.recolor(argb);
                image.setPixelRGBA(x, y, out & 0xFF00FF00 | (out & 0xFF) << 16 | out >> 16 & 0xFF);
            }
        }
    }
}
