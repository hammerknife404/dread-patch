package io.github.hammerknife404.dreadpatch.client;


import com.mojang.logging.LogUtils;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.slf4j.Logger;

/**
 * Her sword and staff redrawn (0.81.0, the owner's pictures): our own voxel models
 * ({@code models/item/dread_queen_sword_regalia.json}, {@code _staff_regalia}, from {@code tools/models/make_regalia.py})
 * put in place of Ice and Fire's for its two items once models are baked, in hand (hers too), on the ground and as icons.
 * Ice and Fire's files are left as they are: nothing of theirs is shipped or overwritten, and its items stay its own.
 * Since 0.98.7 its own are the classic look, by default ({@link DreadPatchClientConfig}): both are kept, under a
 * {@link Switch} that is whichever is chosen at every call.
 */
public final class RegaliaWeapons {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ModelResourceLocation SWORD = ModelResourceLocation.standalone(DreadPatch.id("item/dread_queen_sword_regalia"));
    private static final ModelResourceLocation STAFF = ModelResourceLocation.standalone(DreadPatch.id("item/dread_queen_staff_regalia"));

    private RegaliaWeapons() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((ModelEvent.RegisterAdditional event) -> {
            event.register(SWORD);
            event.register(STAFF);
            for (String piece : PIECES) {
                event.register(classicIcon(piece));
            }
        });
        modBus.addListener(RegaliaWeapons::onBaked);
    }

    /** Her regalia's pieces, whose icons switch too (0.98.10). */
    private static final String[] PIECES = {"crown", "bodice", "winged_bodice", "robe", "greaves"};

    /**
     * Whether a baked model is the missing model (0.98.13, a review): each model that fails bakes as its own copy of it, so
     * comparing with the missing model's instance never matched; its particle sprite gives it away.
     */
    private static boolean missing(BakedModel model) {
        return model.getParticleIcon().contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
    }

    private static ModelResourceLocation classicIcon(String piece) {
        return ModelResourceLocation.standalone(DreadPatch.id("item/dread_queen_" + piece + "_classic"));
    }

    private static void onBaked(ModelEvent.ModifyBakingResult event) {
        swap(event, "dread_queen_sword", SWORD);
        swap(event, "dread_queen_staff", STAFF);
        // The regalia's icons (0.98.10, the owner: "classic icons not applying to inventory icons"): its classic ones
        // (as drawn until 0.81.0, from make_icons.py) while the classic armour is on; the redrawn ones, with their trims, off.
        for (String piece : PIECES) {
            ModelResourceLocation item = ModelResourceLocation.inventory(DreadPatch.id("dread_queen_" + piece));
            BakedModel redrawn = event.getModels().get(item);
            BakedModel classic = event.getModels().get(classicIcon(piece));
            if (redrawn == null || classic == null || missing(classic)) {
                LOGGER.warn("Couldn't give the regalia's {} its classic icon", piece);
                continue;
            }
            event.getModels().put(item, new Switch(classic, redrawn, DreadPatchClientConfig::classicArmor));
        }
    }

    private static void swap(ModelEvent.ModifyBakingResult event, String item, ModelResourceLocation ours) {
        BakedModel model = event.getModels().get(ours);
        ModelResourceLocation theirs = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath("iceandfire", item));
        // A model that failed to load bakes as the missing model: keep Ice and Fire's then (0.81.0's staff was drawn
        // as the purple-and-black missing model).
        if (model == null || missing(model) || !event.getModels().containsKey(theirs)) {
            LOGGER.warn("Couldn't put the regalia's {} in place of Ice and Fire's", item);
            return;
        }
        BakedModel classic = event.getModels().get(theirs);
        java.util.function.BooleanSupplier useClassic = item.equals("dread_queen_sword") ? DreadPatchClientConfig::classicSword : DreadPatchClientConfig::classicStaff;
        // Both kept (0.98.7): which is drawn is the client config's, chosen at every call, so a change shows at once.
        event.getModels().put(theirs, new Switch(classic, model, useClassic));
    }

    /**
     * A model that is whichever of two the client config chooses (0.98.7), asked each time. Until 0.98.8's dev client this
     * was an {@code ItemOverrides} with no baker, which NeoForge's patched constructor can't take: model baking failed and
     * the game wouldn't load.
     */
    static final class Switch extends net.neoforged.neoforge.client.model.BakedModelWrapper<BakedModel> {
        private final BakedModel redrawn;
        private final java.util.function.BooleanSupplier useClassic;

        Switch(BakedModel classic, BakedModel redrawn, java.util.function.BooleanSupplier useClassic) {
            super(classic);
            this.redrawn = redrawn;
            this.useClassic = useClassic;
        }

        private BakedModel shown() {
            return this.useClassic.getAsBoolean() ? this.originalModel : this.redrawn;
        }

        @Override
        public java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(@org.jetbrains.annotations.Nullable net.minecraft.world.level.block.state.BlockState state,
                @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side, net.minecraft.util.RandomSource rand) {
            return this.shown().getQuads(state, side, rand);
        }

        @Override
        public java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(@org.jetbrains.annotations.Nullable net.minecraft.world.level.block.state.BlockState state,
                @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side, net.minecraft.util.RandomSource rand,
                net.neoforged.neoforge.client.model.data.ModelData data, @org.jetbrains.annotations.Nullable net.minecraft.client.renderer.RenderType renderType) {
            return this.shown().getQuads(state, side, rand, data, renderType);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.shown().useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return this.shown().isGui3d();
        }

        @Override
        public boolean usesBlockLight() {
            return this.shown().usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer() {
            return this.shown().isCustomRenderer();
        }

        @Override
        public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() {
            return this.shown().getParticleIcon();
        }

        @Override
        public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() {
            return this.shown().getTransforms();
        }

        @Override
        public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() {
            return this.shown().getOverrides();
        }

        @Override
        public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext context, com.mojang.blaze3d.vertex.PoseStack poseStack, boolean leftHand) {
            return this.shown().applyTransform(context, poseStack, leftHand);
        }

        @Override
        public java.util.List<net.minecraft.client.renderer.RenderType> getRenderTypes(net.minecraft.world.item.ItemStack stack, boolean fabulous) {
            return this.shown().getRenderTypes(stack, fabulous);
        }

        @Override
        public java.util.List<BakedModel> getRenderPasses(net.minecraft.world.item.ItemStack stack, boolean fabulous) {
            return this.shown().getRenderPasses(stack, fabulous);
        }
    }
}
