package io.github.hammerknife404.dreadpatch.effect;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.item.DreadArmorItem;
import io.github.hammerknife404.dreadpatch.magic.WebRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.lang.reflect.Field;

/**
 * Dread armour eases webs (0.75.0, {@link WebRules}), only webs: each piece worn (Rimefrost dragonscale, dread
 * dragonsteel, her regalia) takes an eighth of any web's hold off, half for a full set, so webs still slow.
 * A web sets the entity's {@code stuckSpeedMultiplier} as it's touched, and the next move uses it: it's
 * eased before each tick, on both sides (a player moves on its own client).
 */
public final class WebEase {
    private static final Field STUCK = ObfuscationReflectionHelper.findField(Entity.class, "stuckSpeedMultiplier");
    private static boolean failed;

    private WebEase() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(WebEase::onTick);
    }

    static int pieces(LivingEntity entity) {
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (entity.getItemBySlot(slot).getItem() instanceof DreadArmorItem) {
                count++;
            }
        }
        return count;
    }

    /** Only webs (a cobweb, a scuttler's): powder snow and berry bushes hold as ever. */
    static boolean inWeb(LivingEntity entity) {
        net.minecraft.world.phys.AABB box = entity.getBoundingBox().deflate(1.0E-7);
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(
                net.minecraft.util.Mth.floor(box.minX), net.minecraft.util.Mth.floor(box.minY), net.minecraft.util.Mth.floor(box.minZ),
                net.minecraft.util.Mth.floor(box.maxX), net.minecraft.util.Mth.floor(box.maxY), net.minecraft.util.Mth.floor(box.maxZ))) {
            net.minecraft.world.level.block.state.BlockState state = entity.level().getBlockState(pos);
            if (state.getBlock() instanceof net.minecraft.world.level.block.WebBlock
                    || state.getBlock() instanceof io.github.hammerknife404.dreadpatch.block.DreadWebBlock) {
                return true;
            }
        }
        return false;
    }

    private static void onTick(EntityTickEvent.Pre event) {
        if (failed || !(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        try {
            Vec3 stuck = (Vec3) STUCK.get(entity);
            if (stuck.lengthSqr() <= 1.0E-7) {
                return;
            }
            double ease = WebRules.ease(pieces(entity));
            if (ease > 0.0 && inWeb(entity)) {
                STUCK.set(entity, new Vec3(WebRules.eased(stuck.x, ease), WebRules.eased(stuck.y, ease), WebRules.eased(stuck.z, ease)));
            }
        } catch (IllegalAccessException e) {
            failed = true;
            DreadPatch.LOGGER.warn("Couldn't ease webs for dread armour", e);
        }
    }
}
