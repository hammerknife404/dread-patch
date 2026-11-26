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
        if (model == null || !event.getModels().containsKey(theirs)) {
            LOGGER.warn("Couldn't put the regalia's {} in place of Ice and Fire's", item);
            return;
        }
        event.getModels().put(theirs, model);
    }
}
