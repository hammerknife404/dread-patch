package io.github.hammerknife404.dreadpatch.item;

import com.iafenvoy.iceandfire.registry.IafItems;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Ice and Fire's lich staff ({@code iceandfire:lich_staff}, a Dread Lich's: right-click looses a lich skull), given a
 * little of the Dread's magic (0.87.0, the owner: "Grant modest related spell buffs plus Level 1 Whiteout and Level 1
 * Dread Phantom spells/summons"; asked, Ice and Fire's own staff). Through its default components, as her staff:
 * <ul>
 * <li>Whiteout Dominion and Summon Dread Phantoms locked into it at level {@link #SPELL_LEVEL}, on the spell wheel while
 * it's in either hand, cast with Iron's cast key.</li>
 * <li>Its own skull a spell (0.98.5, the owner: "Turn the innate spell ability of the dread lich staff into its own
 * spell"): Lich Skull, first among them at level {@link #SPELL_LEVEL} (one skull, as its right-click loosed). The staff
 * a casting implement as her staff is, so right-click casts the selected spell (Iron's cancels its own right-click, the
 * old skull, before it runs).</li>
 * <li>In the main hand: +{@link #SPELL_POWER} Ice Spell Power, Rimefrost Spell Power and Summon Damage (her staff gives
 * three times that Ice power).</li>
 * </ul>
 */
public final class DreadLichStaff {
    public static final int SPELL_LEVEL = 1;
    public static final double SPELL_POWER = 0.05;

    private DreadLichStaff() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(DreadLichStaff::onModifyDefaultComponents);
    }

    private static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        event.modify(IafItems.LICH_STAFF.get(), components -> components
                .set(ComponentRegistry.SPELL_CONTAINER.get(), spells())
                .set(ComponentRegistry.CASTING_IMPLEMENT.get(), net.minecraft.util.Unit.INSTANCE)
                .set(DataComponents.ATTRIBUTE_MODIFIERS, attributes()));
    }

    static ISpellContainer spells() {
        ISpellContainerMutable spells = ISpellContainer.create(3, true, false).mutableCopy();
        spells.addSpell(ModRegistries.LICH_SKULL_SPELL.get(), SPELL_LEVEL, true);
        spells.addSpell(ModRegistries.WHITEOUT_DOMINION_SPELL.get(), SPELL_LEVEL, true);
        spells.addSpell(ModRegistries.SUMMON_DREAD_PHANTOMS_SPELL.get(), SPELL_LEVEL, true);
        return spells.toImmutable();
    }

    static ItemAttributeModifiers attributes() {
        return ItemAttributeModifiers.builder()
                .add(AttributeRegistry.ICE_SPELL_POWER, new AttributeModifier(DreadPatch.id("lich_staff_ice_spell_power"),
                        SPELL_POWER, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
                .add(ModRegistries.RIMEFROST_SPELL_POWER, new AttributeModifier(DreadPatch.id("lich_staff_rimefrost_spell_power"),
                        SPELL_POWER, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
                .add(AttributeRegistry.SUMMON_DAMAGE, new AttributeModifier(DreadPatch.id("lich_staff_summon_damage"),
                        SPELL_POWER, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
                .build();
    }
}
