package io.github.hammerknife404.dreadpatch.registry;

import com.iafenvoy.iceandfire.data.DragonArmorMaterial;
import com.iafenvoy.iceandfire.data.DragonArmorPart;
import com.iafenvoy.iceandfire.item.DragonArmorItem;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Locale;

/**
 * Dread dragonsteel dragon armour (0.84.4, the owner: "Should also include a rimefrost dragonsteel dragon armor"): Ice
 * and Fire's {@link DragonArmorItem}s (head, neck, body, tail) of a material of ours, as its ice dragonsteel's (10, the
 * most it has; dragonsteel), made as its are, from blocks of the steel: so the dread dragonsteel block too (nine ingots,
 * as Ice and Fire's dragonsteel blocks).
 * <p>Drawn on a dragon from {@code iceandfire:textures/entity/dragon_armor/armor_<part>_dread_dragonsteel.png}, which
 * {@code client/DreadRecolors.dragonArmor} makes from its ice dragonsteel's; its sprites recoloured as the rest of the
 * steel's.
 */
public final class DreadDragonArmor {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DreadPatch.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DreadPatch.MODID);

    /** Ice and Fire's: iron 1.5, copper and gold 2, silver 3, diamond 5, netherite 7, dragonsteel 10. */
    public static final DragonArmorMaterial DREAD_DRAGONSTEEL = new DragonArmorMaterial("dread_dragonsteel", 10.0, false, true);

    /** As Ice and Fire's dragonsteel blocks: hard as netherite's, a diamond pickaxe's. */
    public static final DeferredBlock<Block> DREAD_DRAGONSTEEL_BLOCK = BLOCKS.registerSimpleBlock("dread_dragonsteel_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(10.0F, 1000.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> DREAD_DRAGONSTEEL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(DREAD_DRAGONSTEEL_BLOCK,
            new Item.Properties().rarity(Rarity.RARE));

    public static final List<DeferredItem<Armor>> ARMOR = List.of(
            armor(DragonArmorPart.HEAD), armor(DragonArmorPart.NECK), armor(DragonArmorPart.BODY), armor(DragonArmorPart.TAIL));

    private DreadDragonArmor() {
    }

    private static DeferredItem<Armor> armor(DragonArmorPart part) {
        return ITEMS.register("dragonarmor_dread_dragonsteel_" + part.getId(), () -> new Armor(part));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    /** Ice and Fire's dragon armour, of dread dragonsteel: its name ours (Ice and Fire names its own by its id). */
    public static final class Armor extends DragonArmorItem {
        Armor(DragonArmorPart part) {
            super(DREAD_DRAGONSTEEL, part);
        }

        @Override
        public String getDescriptionId() {
            return "item.dreadpatch.dragonarmor_dread_dragonsteel_" + this.dragonSlot.getId().toLowerCase(Locale.ROOT);
        }
    }
}
