package io.github.hammerknife404.dreadpatch.effect;

import com.iafenvoy.iceandfire.registry.IafParticles;
import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import io.github.hammerknife404.dreadpatch.magic.RimeLeech;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The Dread Queen's Withering Rime leeches: on someone she afflicted (her bolt, her crown's
 * shrapnel, her sword, Beater's Decree, her husk), each withering tick takes a share of their max
 * health by difficulty, 1% on Easy, 1.5% on Normal, 2% on Hard, never under 1 or over 10
 * ({@link RimeLeech}), and she gains what it took while she's within {@link #RANGE} blocks of them.
 * The Rime is hers until someone else lays it on them again. Anyone else's Withering Rime withers 1 a
 * tick and feeds no one.
 */
public final class RimeSiphon {
    public static final double RANGE = 32.0;

    /** Server thread only: who laid each living thing's Withering Rime, and whether she was a queen. */
    private static final Map<LivingEntity, Layer> LAID_BY = new WeakHashMap<>();

    private record Layer(UUID id, boolean queen) {
    }

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
            LAID_BY.put(entity, new Layer(source.getUUID(), source instanceof DreadQueen));
        } else {
            LAID_BY.remove(entity);
        }
    }

    /** What a withering tick takes from {@code victim}: her leech if a queen laid it (even one now gone or far off), else 1. */
    static float witherAmount(LivingEntity victim) {
        Layer layer = LAID_BY.get(victim);
        if (layer == null || !layer.queen()) {
            return 1.0F;
        }
        return RimeLeech.perTick(victim.getMaxHealth(), share(victim.level().getDifficulty()));
    }

    static float share(Difficulty difficulty) {
        return switch (difficulty) {
            case HARD -> RimeLeech.HARD;
            case NORMAL -> RimeLeech.NORMAL;
            default -> RimeLeech.EASY;
        };
    }

    /** Its withering has just taken {@code amount} from {@code victim}: the queen who laid it, if near, takes it in. */
    static void feed(LivingEntity victim, float amount) {
        Layer layer = LAID_BY.get(victim);
        if (layer == null || !layer.queen() || amount <= 0.0F || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        if (level.getEntity(layer.id()) instanceof DreadQueen queen && queen.isAlive() && queen.distanceToSqr(victim) <= RANGE * RANGE) {
            queen.heal(amount);
            level.sendParticles(IafParticles.DREAD_TORCH.get(), victim.getX(), victim.getY(0.6), victim.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
            level.sendParticles(ParticleTypes.SNOWFLAKE, queen.getX(), queen.getY(0.6), queen.getZ(), 3, 0.3, 0.4, 0.3, 0.01);
        }
    }
}
