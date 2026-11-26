package io.github.hammerknife404.dreadpatch.registry;

import com.iafenvoy.iceandfire.data.DragonArmorMaterial;
import com.iafenvoy.iceandfire.data.DragonArmorPart;
import com.iafenvoy.iceandfire.item.DragonArmorItem;
import com.iafenvoy.iceandfire.item.block.PileBlock;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Locale;

/**
 * A Rimefrost dragon's hoard (0.84.0): the hoard piles Ice and Fire lacks, iron and blacksteel (its {@link PileBlock}s,
 * as its gold, silver and copper piles: eight layers, two nuggets a layer, a nugget in hand adds a layer), and blacksteel
 * dragon armour (Ice and Fire's {@link DragonArmorItem}s of a material of ours: between diamond's 5 and netherite's 7).
 * <p>Blacksteel is L_Ender's Cataclysm's (the owner: "the same stuff Maledictus undead use"; asked, Cataclysm's, optional):
 * nothing of ours names its items but tags ({@link #BLACKSTEEL_NUGGETS}, {@link #BLACKSTEEL_BLOCKS}, its items optional
 * in them), so without Cataclysm the blacksteel pile drops nothing, no nest has one, and the armour's recipes are off.
 * More blacksteel things are left for later (the owner: "Shelf future blacksteel items for later").
 */
public final class Hoard {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DreadPatch.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DreadPatch.MODID);

    public static final TagKey<Item> BLACKSTEEL_NUGGETS = TagKey.create(Registries.ITEM, DreadPatch.id("blacksteel_nuggets"));
    public static final TagKey<Item> BLACKSTEEL_BLOCKS = TagKey.create(Registries.ITEM, DreadPatch.id("blacksteel_blocks"));
    /** Ice and Fire's armour: iron 1.5, copper and gold 2, silver 3, diamond 5, netherite 7, dragonsteel 10. */
    public static final DragonArmorMaterial BLACKSTEEL = new DragonArmorMaterial("blacksteel", 6.0, false, false);

    public static final DeferredBlock<PileBlock> IRON_PILE = BLOCKS.register("iron_pile", PileBlock::new);
    public static final DeferredBlock<PileBlock> BLACKSTEEL_PILE = BLOCKS.register("blacksteel_pile", PileBlock::new);
    public static final DeferredItem<BlockItem> IRON_PILE_ITEM = ITEMS.registerSimpleBlockItem(IRON_PILE);
    public static final DeferredItem<BlockItem> BLACKSTEEL_PILE_ITEM = ITEMS.registerSimpleBlockItem(BLACKSTEEL_PILE);
    public static final List<DeferredItem<BlacksteelDragonArmor>> BLACKSTEEL_DRAGON_ARMOR = List.of(
            armor(DragonArmorPart.HEAD), armor(DragonArmorPart.NECK), armor(DragonArmorPart.BODY), armor(DragonArmorPart.TAIL));

    private Hoard() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    private static DeferredItem<BlacksteelDragonArmor> armor(DragonArmorPart part) {
        return ITEMS.register("dragonarmor_blacksteel_" + part.getId(), () -> new BlacksteelDragonArmor(part));
    }

    /** Ice and Fire's dragon armour, of blacksteel: its name ours (Ice and Fire's names its own by its id). */
    public static final class BlacksteelDragonArmor extends DragonArmorItem {
        BlacksteelDragonArmor(DragonArmorPart part) {
            super(BLACKSTEEL, part);
        }

        @Override
        public String getDescriptionId() {
            return "item.dreadpatch.dragonarmor_blacksteel_" + this.dragonSlot.getId().toLowerCase(Locale.ROOT);
        }
    }
}
