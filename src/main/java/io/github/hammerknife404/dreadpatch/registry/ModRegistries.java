package io.github.hammerknife404.dreadpatch.registry;

import com.iafenvoy.iceandfire.entity.DreadQueenEntity;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistries {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, DreadPatch.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DreadPatch.MODID);

    /**
     * Ice and Fire's own Dread Queen class, which Ice and Fire never registers. A full-size humanoid
     * (the zombie's box); tracked from as far as the Wither, so her boss bar shows from afar.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<DreadQueenEntity>> DREAD_QUEEN = ENTITY_TYPES.register("dread_queen",
            () -> EntityType.Builder.<DreadQueenEntity>of(DreadQueenEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(DreadPatch.id("dread_queen").toString()));

    /** Ice and Fire's dread egg base, spotted with the queen's eye glow. */
    public static final DeferredItem<DeferredSpawnEggItem> DREAD_QUEEN_SPAWN_EGG = ITEMS.register("dread_queen_spawn_egg",
            () -> new DeferredSpawnEggItem(DREAD_QUEEN, 0xE0E6E6, 0x00CBFF, new Item.Properties()));

    private ModRegistries() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
    }
}
