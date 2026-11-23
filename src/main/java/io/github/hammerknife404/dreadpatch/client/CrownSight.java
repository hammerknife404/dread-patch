package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Dread Queen's crown's sight (0.58.0, the owner: "near perfect visibility in snow storms and heavy snow
 * storms", "sight out another 75% in whiteout weather or in whiteout dominions from any source"): read by the
 * fog of the Dreadlands' storms ({@link DreadlandsClient}) and of any whiteout ({@link WhiteoutClient}), on the
 * server's config (synced to clients).
 */
final class CrownSight {
    private CrownSight() {
    }

    static boolean crowned() {
        return Minecraft.getInstance().getCameraEntity() instanceof LivingEntity living
                && living.getItemBySlot(EquipmentSlot.HEAD).is(DreadGear.DREAD_QUEEN_CROWN.get());
    }

    /** Whether the Dreadlands' storm fog is kept off. */
    static boolean clearsStorms() {
        return crowned() && DreadPatchConfig.crownStormSight();
    }

    /** What a whiteout's fog distance is multiplied by. */
    static float whiteoutReach() {
        return crowned() ? 1.0F + (float) DreadPatchConfig.crownWhiteoutSight() : 1.0F;
    }
}
