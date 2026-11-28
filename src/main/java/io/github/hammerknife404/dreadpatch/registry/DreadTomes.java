package io.github.hammerknife404.dreadpatch.registry;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.magic.TomeRules;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * The Rimefrost spellbooks (0.98.3, the owner's handoff; {@link TomeRules}): Iron's {@link SpellBook}s, worn in its
 * spellbook curio slot.
 * <ul>
 * <li>The <b>Rimebound Tome</b> ("crafted by infusing an Ice spellbook with Dreadlands materials; reduces generic Ice
 * bonuses in favor of stronger Rimefrost school bonuses"): Iron's Ice Spell Book at a smithing table, a dread shard its
 * template and a dreadwood log its addition, so its spells and inscriptions come with it.</li>
 * <li>The <b>Unforged Sovereign's Tome</b>, ours: the Rimebound Tome at a smithing table, a dread dragonsteel ingot its
 * template and a Rimefrost Dragon Scale its addition (the handoff's ingots and scales: our forge has only its input and
 * its blood).</li>
 * <li>The <b>Dread Sovereign's Tome</b> ("forged by placing the Rimebound Tome inside a Rimefrost Dragonforge with Rimefrost
 * Dragon Scales, Rimefrost Dragon Blood, and Dread Dragonsteel Ingots; heavily focused on Rimefrost school bonuses"): the
 * unforged tome in the Rimefrost dragonforge with Rimefrost Dragon Blood, which keeps its spells
 * ({@code RimefrostForgeBlockEntity.make}).</li>
 * </ul>
 */
public final class DreadTomes {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DreadPatch.MODID);

    public static final DeferredItem<Item> RIMEBOUND_TOME = ITEMS.register("rimebound_tome", () -> new SpellBook(TomeRules.SLOTS,
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()).withSpellbookAttributes(
            new AttributeContainer(AttributeRegistry.ICE_SPELL_POWER, TomeRules.RIMEBOUND_ICE_POWER, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new AttributeContainer(ModRegistries.RIMEFROST_SPELL_POWER, TomeRules.RIMEBOUND_RIMEFROST_POWER, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new AttributeContainer(AttributeRegistry.MAX_MANA, TomeRules.RIMEBOUND_MANA, AttributeModifier.Operation.ADD_VALUE)));
    public static final DeferredItem<Item> UNFORGED_SOVEREIGNS_TOME = ITEMS.registerSimpleItem("unforged_sovereigns_tome",
            new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<Item> DREAD_SOVEREIGNS_TOME = ITEMS.register("dread_sovereigns_tome", () -> new SpellBook(TomeRules.SLOTS,
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()).withSpellbookAttributes(
            new AttributeContainer(ModRegistries.RIMEFROST_SPELL_POWER, TomeRules.SOVEREIGN_RIMEFROST_POWER, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new AttributeContainer(ModRegistries.RIMEFROST_MAGIC_RESIST, TomeRules.SOVEREIGN_RIMEFROST_RESIST, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new AttributeContainer(AttributeRegistry.MAX_MANA, TomeRules.SOVEREIGN_MANA, AttributeModifier.Operation.ADD_VALUE)));

    private DreadTomes() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static List<DeferredItem<Item>> items() {
        return List.of(RIMEBOUND_TOME, UNFORGED_SOVEREIGNS_TOME, DREAD_SOVEREIGNS_TOME);
    }
}
