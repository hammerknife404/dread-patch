package io.github.hammerknife404.dreadpatch.item;

import com.iafenvoy.iceandfire.registry.IafItems;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.magic.PermafrostConduit;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Her staff, Ice and Fire's {@code iceandfire:dread_queen_staff} ("Staff of the Dreaded Queen"),
 * which does nothing there, made into an Iron's staff. Its class is a bare {@code Item}, so it's all
 * done through its default components (every staff has them, including ones already dropped) and
 * events:
 * <ul>
 * <li>three of her spells (her bolt, her crown, Summon Court) locked into it at {@link #SPELL_LEVEL}, on the spell wheel while it's in either
 * hand. Iron's only takes spells out at the Inscription Table, which takes only spellbooks;</li>
 * <li>a casting implement like Iron's staffs: right-click casts the selected spell;</li>
 * <li>in the main hand, as Iron's own staffs: +15% Ice Spell Power and +10% Cooldown Reduction;</li>
 * <li>Permafrost Conduit: while it's in the main hand, Ice spells cast standing on
 * {@link #PERMAFROST} cost a fifth less mana ({@link PermafrostConduit}). Iron's still checks the
 * full cost before the cast, and takes the lower one after.</li>
 * </ul>
 */
public final class DreadQueenStaff {
    /** Her spells' level on the staff: mid-range, as Iron's Boreal Blade and Hellrazor carry theirs. Her scrolls (3-5) can beat it. */
    public static final int SPELL_LEVEL = 3;
    public static final double ICE_SPELL_POWER = 0.15;
    public static final double COOLDOWN_REDUCTION = 0.10;
    /** Snow, ice and Ice and Fire's dragon-frozen ground. */
    public static final TagKey<Block> PERMAFROST = TagKey.create(Registries.BLOCK, DreadPatch.id("permafrost"));

    private DreadQueenStaff() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(DreadQueenStaff::onModifyDefaultComponents);
        gameBus.addListener(DreadQueenStaff::onSpellCast);
    }

    public static boolean is(ItemStack stack) {
        return stack.is(IafItems.DREAD_QUEEN_STAFF.get());
    }

    private static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        event.modify(IafItems.DREAD_QUEEN_STAFF.get(), components -> components
                .set(ComponentRegistry.SPELL_CONTAINER.get(), spells())
                .set(ComponentRegistry.CASTING_IMPLEMENT.get(), Unit.INSTANCE)
                .set(DataComponents.ATTRIBUTE_MODIFIERS, attributes()));
    }

    /** Spell-wheel container (not "must equip", which would keep it off the wheel in hand), all three spells locked. */
    static ISpellContainer spells() {
        ISpellContainerMutable spells = ISpellContainer.create(3, true, false).mutableCopy();
        spells.addSpell(ModRegistries.WITHERING_RIME_BOLT_SPELL.get(), SPELL_LEVEL, true);
        spells.addSpell(ModRegistries.RIMEFIRE_CROWN_SPELL.get(), SPELL_LEVEL, true);
        spells.addSpell(ModRegistries.SUMMON_COURT_SPELL.get(), SPELL_LEVEL, true);
        return spells.toImmutable();
    }

    /** As Iron's staffs give theirs ({@code StaffTier}): multiplying the base, in the main hand. */
    static ItemAttributeModifiers attributes() {
        return ItemAttributeModifiers.builder()
                .add(AttributeRegistry.ICE_SPELL_POWER, new AttributeModifier(DreadPatch.id("dread_queen_staff_ice_spell_power"),
                        ICE_SPELL_POWER, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
                .add(AttributeRegistry.COOLDOWN_REDUCTION, new AttributeModifier(DreadPatch.id("dread_queen_staff_cooldown_reduction"),
                        COOLDOWN_REDUCTION, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** Permafrost Conduit. Iron's fires this as a player's spell goes off, before it takes the mana. */
    private static void onSpellCast(SpellOnCastEvent event) {
        Player player = event.getEntity();
        if (event.getSchoolType() == SchoolRegistry.ICE.get() && is(player.getMainHandItem()) && onPermafrost(player)) {
            event.setManaCost(PermafrostConduit.manaCost(event.getManaCost()));
        }
    }

    /** What they stand on, or in (a snow layer, powder snow). */
    static boolean onPermafrost(Entity entity) {
        return entity.getBlockStateOn().is(PERMAFROST) || entity.level().getBlockState(entity.blockPosition()).is(PERMAFROST);
    }
}
