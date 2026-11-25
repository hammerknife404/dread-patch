package io.github.hammerknife404.dreadpatch.effect;

import com.iafenvoy.iceandfire.registry.IafParticles;
import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import io.github.hammerknife404.dreadpatch.magic.DreadDebuffs;
import io.github.hammerknife404.dreadpatch.entity.SoulTether;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The Dread Queen's Withering Rime feeds her: each withering tick that takes health from someone she
 * afflicted (her bolt, her crown's shrapnel, her sword, Beater's Decree, her husk) heals her a flat
 * amount by difficulty, {@link #HEAL_EASY} on Easy (and Peaceful), {@link #HEAL_NORMAL} on Normal,
 * {@link #HEAL_HARD} on Hard, while she's within {@link #RANGE} blocks of them. They lose 1 a tick, as
 * under anyone's Withering Rime. The Rime is hers until someone else lays it on them again. Anyone
 * else's feeds no one. (0.13.0 leeched a share of the victim's max health, up to 10 a tick, and she
 * cut through wardens.) Bonded to a Syndrigossa, she shares it with her dragon ({@link SoulTether}). Over several
 * targets (0.73.0) every one feeds her in full, unless the config caps how many or splits her heal among them.
 */
public final class RimeSiphon {
    public static final double RANGE = 32.0;
    /** What each tick of her withering heals her, per target. */
    public static final float HEAL_EASY = 1.0F;
    public static final float HEAL_NORMAL = 2.0F;
    public static final float HEAL_HARD = 3.0F;

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

    /** Who laid {@code victim}'s Withering Rime, if they're in its level (0.51.0: her regalia strengthens its wearer's withering). */
    @Nullable
    public static Entity layer(LivingEntity victim) {
        Layer layer = LAID_BY.get(victim);
        if (layer == null || !(victim.level() instanceof ServerLevel level)) {
            return null;
        }
        Entity found = level.getPlayerByUUID(layer.id());
        return found != null ? found : level.getEntity(layer.id());
    }

    static float heal(Difficulty difficulty) {
        return switch (difficulty) {
            case HARD -> HEAL_HARD;
            case NORMAL -> HEAL_NORMAL;
            default -> HEAL_EASY;
        };
    }

    /** Her withering has just taken health from {@code victim}: the queen who laid it, if near, is fed. */
    static void feed(LivingEntity victim) {
        Layer layer = LAID_BY.get(victim);
        if (layer == null || !layer.queen() || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        if (level.getEntity(layer.id()) instanceof DreadQueen queen && queen.isAlive() && queen.distanceToSqr(victim) <= RANGE * RANGE) {
            float heal = heal(level.getDifficulty());
            int maxTargets = DreadPatchConfig.siphonTargets();
            boolean split = DreadPatchConfig.siphonSplit();
            if (maxTargets > 0 || split) {
                // Over several targets (0.73.0): the nearest ones only, or her heal shared among them.
                double own = queen.distanceToSqr(victim);
                int feeding = 0;
                int nearer = 0;
                for (Map.Entry<LivingEntity, Layer> entry : LAID_BY.entrySet()) {
                    LivingEntity other = entry.getKey();
                    if (!entry.getValue().id().equals(layer.id()) || !other.isAlive() || other.level() != level
                            || !other.hasEffect(ModRegistries.WITHERING_RIME)) {
                        continue;
                    }
                    double distance = queen.distanceToSqr(other);
                    if (distance > RANGE * RANGE) {
                        continue;
                    }
                    feeding++;
                    if (other != victim && (distance < own || distance == own && other.getId() < victim.getId())) {
                        nearer++;
                    }
                }
                heal = DreadDebuffs.siphon(heal, nearer, feeding, maxTargets, split);
                if (heal <= 0.0F) {
                    return;
                }
            }
            SoulTether.heal(queen, heal);
            level.sendParticles(IafParticles.DREAD_TORCH.get(), victim.getX(), victim.getY(0.6), victim.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
            level.sendParticles(ParticleTypes.SNOWFLAKE, queen.getX(), queen.getY(0.6), queen.getZ(), 3, 0.3, 0.4, 0.3, 0.01);
        }
    }
}
