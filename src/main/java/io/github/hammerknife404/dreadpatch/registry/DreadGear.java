package io.github.hammerknife404.dreadpatch.registry;

import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.item.tool.ActivePostHitAxeItem;
import com.iafenvoy.iceandfire.item.tool.ActivePostHitHoeItem;
import com.iafenvoy.iceandfire.item.tool.ActivePostHitPickaxeItem;
import com.iafenvoy.iceandfire.item.tool.ActivePostHitShovelItem;
import com.iafenvoy.iceandfire.item.tool.ActivePostHitSwordItem;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.item.DreadArmorItem;
import io.github.hammerknife404.dreadpatch.item.DreadDragonsteel;
import io.github.hammerknife404.dreadpatch.loot.RimefrostScalesFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The Rimefrost dragon scale and dread dragonsteel (0.42.0): the scale (a Rimefrost corpse's, in place
 * of Ice and Fire's coloured scales), its blood (scales and a bottle in a dragonforge; Ice and Fire's ice
 * one until 0.59.0, the Rimefrost dragonforge since, {@code DreadForge}), the ingot (blood and iron there),
 * and their armour and tools, Ice and Fire's ice dragonscale and ice
 * dragonsteel sets made Dread: their numbers are Ice and Fire's (the scale set's fixed, dragonsteel's
 * from its config, as its own are), their abilities ours ({@link DreadDragonsteel}, {@code DragonArmor}).
 */
public final class DreadGear {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DreadPatch.MODID);
    private static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, DreadPatch.MODID);
    private static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, DreadPatch.MODID);

    public static final DeferredItem<Item> RIMEFROST_DRAGON_SCALE = ITEMS.registerSimpleItem("rimefrost_dragon_scale", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> RIMEFROST_DRAGON_BLOOD = ITEMS.registerSimpleItem("rimefrost_dragon_blood", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> DREAD_DRAGONSTEEL_INGOT = ITEMS.registerSimpleItem("dread_dragonsteel_ingot", new Item.Properties().rarity(Rarity.RARE));

    /** Ice and Fire's dragonscale armour material (its {@code DragonColor.initArmors}): 5/7/9/5, enchantability 15, chain, toughness 2. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> RIMEFROST_DRAGONSCALE_MATERIAL = ARMOR_MATERIALS.register("rimefrost_dragonscale",
            () -> material(5, 7, 9, 5, 15, SoundEvents.ARMOR_EQUIP_CHAIN, 2.0F, 0.0F, () -> Ingredient.of(RIMEFROST_DRAGON_SCALE.get()), "rimefrost_dragonscale"));
    /** Ice and Fire's ice dragonsteel armour material, read from its config as its own is. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DREAD_DRAGONSTEEL_MATERIAL = ARMOR_MATERIALS.register("dread_dragonsteel", () -> {
        IafCommonConfig.ArmorsConfig c = IafCommonConfig.INSTANCE.armors;
        return material(c.dragonsteelBootsArmor.getValue(), c.dragonsteelLeggingsArmor.getValue(), c.dragonsteelChestplateArmor.getValue(),
                c.dragonsteelHelmetArmor.getValue(), c.dragonsteelArmorEnchantability.getValue(), SoundEvents.ARMOR_EQUIP_DIAMOND,
                c.dragonsteelArmorToughness.getValue().floatValue(), c.dragonsteelArmorKnockbackResistance.getValue().floatValue(),
                () -> Ingredient.of(DREAD_DRAGONSTEEL_INGOT.get()), "dread_dragonsteel");
    });

    /**
     * The Dread Queen's regalia (0.43.0): her outfit to wear, "in line with" the dread sets (the owner): dread
     * dragonsteel's numbers (Ice and Fire's config), dragon armour against breath, repaired with dread dragonsteel.
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DREAD_QUEEN_MATERIAL = ARMOR_MATERIALS.register("dread_queen", () -> {
        IafCommonConfig.ArmorsConfig c = IafCommonConfig.INSTANCE.armors;
        return material(c.dragonsteelBootsArmor.getValue(), c.dragonsteelLeggingsArmor.getValue(), c.dragonsteelChestplateArmor.getValue(),
                c.dragonsteelHelmetArmor.getValue(), c.dragonsteelArmorEnchantability.getValue(), SoundEvents.ARMOR_EQUIP_NETHERITE,
                c.dragonsteelArmorToughness.getValue().floatValue(), c.dragonsteelArmorKnockbackResistance.getValue().floatValue(),
                () -> Ingredient.of(DREAD_DRAGONSTEEL_INGOT.get()), "dread_queen");
    });
    public static final DeferredItem<DreadArmorItem> DREAD_QUEEN_CROWN = queenArmor("dread_queen_crown", ArmorItem.Type.HELMET);
    public static final DeferredItem<DreadArmorItem> DREAD_QUEEN_BODICE = queenArmor("dread_queen_bodice", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<DreadArmorItem> DREAD_QUEEN_ROBE = queenArmor("dread_queen_robe", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<DreadArmorItem> DREAD_QUEEN_GREAVES = queenArmor("dread_queen_greaves", ArmorItem.Type.BOOTS);

    public static final DeferredItem<DreadArmorItem> RIMEFROST_DRAGONSCALE_HELMET = scaleArmor("rimefrost_dragonscale_helmet", ArmorItem.Type.HELMET, 397);
    public static final DeferredItem<DreadArmorItem> RIMEFROST_DRAGONSCALE_CHESTPLATE = scaleArmor("rimefrost_dragonscale_chestplate", ArmorItem.Type.CHESTPLATE, 577);
    public static final DeferredItem<DreadArmorItem> RIMEFROST_DRAGONSCALE_LEGGINGS = scaleArmor("rimefrost_dragonscale_leggings", ArmorItem.Type.LEGGINGS, 541);
    public static final DeferredItem<DreadArmorItem> RIMEFROST_DRAGONSCALE_BOOTS = scaleArmor("rimefrost_dragonscale_boots", ArmorItem.Type.BOOTS, 469);

    public static final DeferredItem<DreadArmorItem> DREAD_DRAGONSTEEL_HELMET = steelArmor("dread_dragonsteel_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<DreadArmorItem> DREAD_DRAGONSTEEL_CHESTPLATE = steelArmor("dread_dragonsteel_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<DreadArmorItem> DREAD_DRAGONSTEEL_LEGGINGS = steelArmor("dread_dragonsteel_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<DreadArmorItem> DREAD_DRAGONSTEEL_BOOTS = steelArmor("dread_dragonsteel_boots", ArmorItem.Type.BOOTS);

    /** Ice and Fire's ice dragonsteel tools' attack damage and speed (its {@code IafItems}). */
    public static final DeferredItem<Item> DREAD_DRAGONSTEEL_SWORD = ITEMS.register("dread_dragonsteel_sword",
            () -> new ActivePostHitSwordItem(DreadDragonsteel.TIER, tool(3.0F, -2.4F), DreadDragonsteel.RIME));
    public static final DeferredItem<Item> DREAD_DRAGONSTEEL_PICKAXE = ITEMS.register("dread_dragonsteel_pickaxe",
            () -> new ActivePostHitPickaxeItem(DreadDragonsteel.TIER, tool(1.0F, -2.8F), DreadDragonsteel.RIME));
    public static final DeferredItem<Item> DREAD_DRAGONSTEEL_AXE = ITEMS.register("dread_dragonsteel_axe",
            () -> new ActivePostHitAxeItem(DreadDragonsteel.TIER, tool(5.0F, -3.0F), DreadDragonsteel.RIME));
    public static final DeferredItem<Item> DREAD_DRAGONSTEEL_SHOVEL = ITEMS.register("dread_dragonsteel_shovel",
            () -> new ActivePostHitShovelItem(DreadDragonsteel.TIER, tool(1.5F, -3.0F), DreadDragonsteel.RIME));
    public static final DeferredItem<Item> DREAD_DRAGONSTEEL_HOE = ITEMS.register("dread_dragonsteel_hoe",
            () -> new ActivePostHitHoeItem(DreadDragonsteel.TIER, tool(-4.0F, 0.0F), DreadDragonsteel.RIME));

    /** Scales for a dragon's age, as Ice and Fire's {@code dragon_loot} sizes its coloured ones. */
    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<RimefrostScalesFunction>> RIMEFROST_SCALES_FUNCTION =
            LOOT_FUNCTIONS.register("rimefrost_scales", () -> new LootItemFunctionType<>(RimefrostScalesFunction.CODEC));

    private DreadGear() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ARMOR_MATERIALS.register(modBus);
        LOOT_FUNCTIONS.register(modBus);
    }

    /** Everything, in the order the creative tab shows it. */
    public static List<DeferredItem<? extends Item>> all() {
        return List.of(RIMEFROST_DRAGON_SCALE, RIMEFROST_DRAGON_BLOOD, DREAD_DRAGONSTEEL_INGOT,
                RIMEFROST_DRAGONSCALE_HELMET, RIMEFROST_DRAGONSCALE_CHESTPLATE, RIMEFROST_DRAGONSCALE_LEGGINGS, RIMEFROST_DRAGONSCALE_BOOTS,
                DREAD_DRAGONSTEEL_HELMET, DREAD_DRAGONSTEEL_CHESTPLATE, DREAD_DRAGONSTEEL_LEGGINGS, DREAD_DRAGONSTEEL_BOOTS,
                DREAD_DRAGONSTEEL_SWORD, DREAD_DRAGONSTEEL_PICKAXE, DREAD_DRAGONSTEEL_AXE, DREAD_DRAGONSTEEL_SHOVEL, DREAD_DRAGONSTEEL_HOE);
    }

    public static boolean isDreadDragonsteelTool(net.minecraft.world.item.ItemStack stack) {
        return stack.is(DREAD_DRAGONSTEEL_SWORD.get()) || stack.is(DREAD_DRAGONSTEEL_AXE.get()) || stack.is(DREAD_DRAGONSTEEL_PICKAXE.get())
                || stack.is(DREAD_DRAGONSTEEL_SHOVEL.get()) || stack.is(DREAD_DRAGONSTEEL_HOE.get());
    }

    /** Her regalia, helmet to boots (the order {@code RegaliaDrops} counts pieces in). */
    public static List<DeferredItem<DreadArmorItem>> queenArmor() {
        return List.of(DREAD_QUEEN_CROWN, DREAD_QUEEN_BODICE, DREAD_QUEEN_ROBE, DREAD_QUEEN_GREAVES);
    }

    public static List<DeferredItem<DreadArmorItem>> armor() {
        return List.of(RIMEFROST_DRAGONSCALE_HELMET, RIMEFROST_DRAGONSCALE_CHESTPLATE, RIMEFROST_DRAGONSCALE_LEGGINGS, RIMEFROST_DRAGONSCALE_BOOTS,
                DREAD_DRAGONSTEEL_HELMET, DREAD_DRAGONSTEEL_CHESTPLATE, DREAD_DRAGONSTEEL_LEGGINGS, DREAD_DRAGONSTEEL_BOOTS);
    }

    private static Item.Properties tool(float damage, float speed) {
        return new Item.Properties().attributes(DiggerItem.createAttributes(DreadDragonsteel.TIER, damage, speed)).rarity(Rarity.RARE);
    }

    private static DeferredItem<DreadArmorItem> scaleArmor(String name, ArmorItem.Type type, int durability) {
        return ITEMS.register(name, () -> new DreadArmorItem(RIMEFROST_DRAGONSCALE_MATERIAL, type, DreadArmorItem.Kind.SCALE,
                new Item.Properties().durability(durability).rarity(Rarity.UNCOMMON)));
    }

    private static DeferredItem<DreadArmorItem> queenArmor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new DreadArmorItem(DREAD_QUEEN_MATERIAL, type, DreadArmorItem.Kind.QUEEN,
                new Item.Properties().durability(steelDurability(type)).rarity(Rarity.EPIC).fireResistant()));
    }

    private static int steelDurability(ArmorItem.Type type) {
        IafCommonConfig.ArmorsConfig c = IafCommonConfig.INSTANCE.armors;
        return switch (type) {
            case HELMET -> c.dragonsteelHelmetDurability.getValue();
            case CHESTPLATE -> c.dragonsteelChestplateDurability.getValue();
            case LEGGINGS -> c.dragonsteelLeggingsDurability.getValue();
            default -> c.dragonsteelBootsDurability.getValue();
        };
    }

    private static DeferredItem<DreadArmorItem> steelArmor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> {
            IafCommonConfig.ArmorsConfig c = IafCommonConfig.INSTANCE.armors;
            int durability = switch (type) {
                case HELMET -> c.dragonsteelHelmetDurability.getValue();
                case CHESTPLATE -> c.dragonsteelChestplateDurability.getValue();
                case LEGGINGS -> c.dragonsteelLeggingsDurability.getValue();
                default -> c.dragonsteelBootsDurability.getValue();
            };
            return new DreadArmorItem(DREAD_DRAGONSTEEL_MATERIAL, type, DreadArmorItem.Kind.STEEL, new Item.Properties().durability(durability).rarity(Rarity.RARE));
        });
    }

    private static ArmorMaterial material(int boots, int leggings, int chestplate, int helmet, int enchantability,
                                          Holder<net.minecraft.sounds.SoundEvent> sound, float toughness, float knockback, Supplier<Ingredient> repair, String layer) {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, boots);
        defense.put(ArmorItem.Type.LEGGINGS, leggings);
        defense.put(ArmorItem.Type.CHESTPLATE, chestplate);
        defense.put(ArmorItem.Type.HELMET, helmet);
        defense.put(ArmorItem.Type.BODY, chestplate);
        return new ArmorMaterial(defense, enchantability, sound, repair, List.of(new ArmorMaterial.Layer(DreadPatch.id(layer))), toughness, knockback);
    }
}
