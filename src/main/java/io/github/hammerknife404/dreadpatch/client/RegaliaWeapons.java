package io.github.hammerknife404.dreadpatch.client;

import net.minecraft.client.renderer.block.model.ItemOverrides;

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
 * Since 0.98.7 its own are the classic look, by default ({@link DreadPatchClientConfig}): both are kept, and an item
 * override picks one as each stack is drawn.
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
        });
        modBus.addListener(RegaliaWeapons::onBaked);
    }

    private static void onBaked(ModelEvent.ModifyBakingResult event) {
        swap(event, "dread_queen_sword", SWORD);
        swap(event, "dread_queen_staff", STAFF);
    }

    private static void swap(ModelEvent.ModifyBakingResult event, String item, ModelResourceLocation ours) {
        BakedModel model = event.getModels().get(ours);
        ModelResourceLocation theirs = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath("iceandfire", item));
        // A model that failed to load bakes as the missing model: keep Ice and Fire's then (0.81.0's staff was drawn
        // as the purple-and-black missing model).
        if (model == null || model == event.getModels().get(net.minecraft.client.resources.model.ModelBakery.MISSING_MODEL_VARIANT)
                || !event.getModels().containsKey(theirs)) {
            LOGGER.warn("Couldn't put the regalia's {} in place of Ice and Fire's", item);
            return;
        }
        BakedModel classic = event.getModels().get(theirs);
        java.util.function.BooleanSupplier useClassic = item.equals("dread_queen_sword") ? DreadPatchClientConfig::classicSword : DreadPatchClientConfig::classicStaff;
        // Both kept (0.98.7): which is drawn is the client config's, chosen as each stack is drawn, so a change shows at once.
        ItemOverrides choose = new ItemOverrides(null, null, java.util.List.of()) {
            @Override
            public BakedModel resolve(BakedModel self, net.minecraft.world.item.ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.client.multiplayer.ClientLevel level,
                                      @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity entity, int seed) {
                BakedModel shown = useClassic.getAsBoolean() ? classic : model;
                BakedModel resolved = shown.getOverrides().resolve(shown, stack, level, entity, seed);
                return resolved != null ? resolved : shown;
            }
        };
        event.getModels().put(theirs, new net.neoforged.neoforge.client.model.BakedModelWrapper<>(classic) {
            @Override
            public ItemOverrides getOverrides() {
                return choose;
            }
        });
    }
}
