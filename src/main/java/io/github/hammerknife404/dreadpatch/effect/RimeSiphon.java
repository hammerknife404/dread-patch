package io.github.hammerknife404.dreadpatch.effect;

import com.iafenvoy.iceandfire.registry.IafParticles;
import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Withering Rime feeds the Dread Queen who laid it: whatever its withering takes from someone she
 * afflicted (her bolt, her crown's shrapnel, her sword, Beater's Decree, her husk), she gains, while
 * she's within {@link #RANGE} blocks of them. About 1 health a second for each one withering. The
 * Rime is hers until someone else lays it on them again. Only her: players' Withering Rime feeds no one.
 */
public final class RimeSiphon {
    public static final double RANGE = 32.0;

    /** Server thread only: who laid each living thing's Withering Rime. */
    private static final Map<LivingEntity, UUID> LAID_BY = new WeakHashMap<>();

    private RimeSiphon() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(RimeSiphon::onEffectAdded);
    }

    private static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !event.getEffectInstance().is(ModRegistries.WITHERING_RIME)) {
            return;
        }
        Entity source = event.getEffectSource();
        if (source != null) {
            LAID_BY.put(entity, source.getUUID());
        } else {
            LAID_BY.remove(entity);
        }
    }

    /** Its withering has just taken {@code amount} from {@code victim}: the queen who laid it, if near, takes it in. */
    static void feed(LivingEntity victim, float amount) {
        UUID layer = LAID_BY.get(victim);
        if (layer == null || amount <= 0.0F || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        if (level.getEntity(layer) instanceof DreadQueen queen && queen.isAlive() && queen.distanceToSqr(victim) <= RANGE * RANGE) {
            queen.heal(amount);
            level.sendParticles(IafParticles.DREAD_TORCH.get(), victim.getX(), victim.getY(0.6), victim.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
            level.sendParticles(ParticleTypes.SNOWFLAKE, queen.getX(), queen.getY(0.6), queen.getZ(), 3, 0.3, 0.4, 0.3, 0.01);
        }
    }
}
