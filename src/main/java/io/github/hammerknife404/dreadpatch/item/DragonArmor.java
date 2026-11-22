package io.github.hammerknife404.dreadpatch.item;

import com.iafenvoy.iceandfire.item.armor.DragonScaleArmorItem;
import com.iafenvoy.iceandfire.item.armor.DragonSteelArmorItem;
import com.iafenvoy.iceandfire.registry.IafDamageTypes;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Dragon armour against dragon breath, as Ice and Fire has it (its {@code ServerEvents.onEntityDamage}):
 * against a dragon's breath each piece of dragonscale or dragonsteel takes off its share (helmet 10%,
 * chestplate 30%, leggings 20%, boots 10%: 70% for a full set). Ice and Fire does that for its own
 * armour against its own breath; we do it for ours (0.42.0) against its breath, and for its and ours
 * against a Rimefrost dragon's (its breath and its Shatter-Spine's shards).
 */
public final class DragonArmor {
    private DragonArmor() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DragonArmor::onDamage);
    }

    /** What a piece in {@code slot} takes off dragon breath. */
    public static float share(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD, FEET -> 0.1F;
            case CHEST -> 0.3F;
            case LEGS -> 0.2F;
            default -> 0.0F;
        };
    }

    private static void onDamage(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();
        boolean rimefrost = source.is(ModRegistries.WITHERING_BREATH_DAMAGE) || source.is(ModRegistries.SHATTER_SPINE_DAMAGE);
        boolean iceAndFire = source.is(IafDamageTypes.DRAGON_FIRE_TYPE) || source.is(IafDamageTypes.DRAGON_ICE_TYPE) || source.is(IafDamageTypes.DRAGON_LIGHTNING_TYPE);
        if (!rimefrost && !iceAndFire) {
            return;
        }
        LivingEntity victim = event.getEntity();
        float off = 0.0F;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            Item item = victim.getItemBySlot(slot).getItem();
            // Ice and Fire already takes its own armour's share off its own breath.
            boolean counts = item instanceof DreadArmorItem || rimefrost && (item instanceof DragonScaleArmorItem || item instanceof DragonSteelArmorItem);
            if (counts) {
                off += share(slot);
            }
        }
        if (off > 0.0F) {
            event.setNewDamage(event.getNewDamage() * (1.0F - Math.min(1.0F, off)));
        }
    }
}
