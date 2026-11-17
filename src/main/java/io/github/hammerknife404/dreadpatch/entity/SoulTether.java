package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.registry.IafParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The bond between a Dread Queen and the Syndrigossa she rose with (the owner's notes: a 50/50
 * healing split, broken by pulling the two more than 28 blocks apart). What either one's siphon
 * heals (her Withering Rime, the dragon's breath) is shared evenly while the other is alive within
 * {@link #RANGE} blocks; apart, the one who fed keeps it all. The total healed doesn't change.
 */
public final class SoulTether {
    public static final double RANGE = 28.0;

    private SoulTether() {
    }

    /** Heals {@code healer} by {@code amount}, or each of the pair by half while they're within reach. */
    public static void heal(LivingEntity healer, float amount) {
        LivingEntity partner = partner(healer);
        if (partner == null) {
            healer.heal(amount);
            return;
        }
        healer.heal(amount / 2.0F);
        partner.heal(amount / 2.0F);
        if (partner.level() instanceof ServerLevel level) {
            level.sendParticles(IafParticles.DREAD_TORCH.get(), partner.getX(), partner.getY(0.6), partner.getZ(), 2, 0.3, 0.4, 0.3, 0.01);
        }
    }

    /** The other of a bonded pair, if alive and within {@link #RANGE} blocks; server side only. */
    @Nullable
    public static LivingEntity partner(LivingEntity one) {
        if (!(one.level() instanceof ServerLevel level)) {
            return null;
        }
        UUID id = one instanceof DreadQueen queen ? queen.getDragonId() : one instanceof Syndrigossa dragon ? dragon.getQueenId() : null;
        if (id == null) {
            return null;
        }
        Entity other = level.getEntity(id);
        return other instanceof LivingEntity living && living.isAlive() && living.distanceToSqr(one) <= RANGE * RANGE ? living : null;
    }
}
