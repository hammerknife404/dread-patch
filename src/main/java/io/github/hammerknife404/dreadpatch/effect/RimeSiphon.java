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
 * <p>Since 0.85.0 (the owner: "make the withering rime heal all dread and rimefrost type mobs") anyone of the Dread who
 * lays it (a Rimefrost dragon's bite, a phantom's, an unarmed thrall's claws, any Dread mob's hit) is fed by it too:
 * by difficulty (the owner: "scale it down to .25 .5 1hp per difficulty on withering rime that is not tagged otherwise"):
 * {@link #dreadHeal} each tick it withers, while within {@link #RANGE}. Only the queen's is more, and only hers the
 * config shares out. Only the one who laid it is fed (the owner: "only the mob that applies it is healed from its own
 * application"), never a partner it's bonded to, and (the owner: "And can only apply 1 stack per target") a Dread mob's
 * laying on someone already withering only renews it, never deepening it ({@link #onApplicable}; the queen's and
 * Syndrigossa's own are hers as before).
 */
public final class RimeSiphon {
    public static final double RANGE = 32.0;
    /** What each tick of her withering heals her, per target. */
    public static final float HEAL_EASY = 1.0F;
    public static final float HEAL_NORMAL = 2.0F;
    public static final float HEAL_HARD = 3.0F;
    /** What each tick of another Dread mob's withering heals it, by difficulty. */
    public static final float DREAD_HEAL_EASY = 0.25F;
    public static final float DREAD_HEAL_NORMAL = 0.5F;
    public static final float DREAD_HEAL_HARD = 1.0F;

    /** Server thread only: who laid each living thing's Withering Rime, and whether she was a queen. */
    private static final Map<LivingEntity, Layer> LAID_BY = new WeakHashMap<>();

    private record Layer(UUID id, boolean queen, boolean dread) {
    }

    private RimeSiphon() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(RimeSiphon::onEffectAdded);
        gameBus.addListener(net.neoforged.bus.api.EventPriority.LOW, RimeSiphon::onApplicable);
    }

    /** One stack a target: a Dread mob's Withering Rime over one already there renews it at most, never deeper. */
    private static void onApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        Entity source = event.getEffectSource();
        net.minecraft.world.effect.MobEffectInstance existing = entity.getEffect(ModRegistries.WITHERING_RIME);
        if (entity.level().isClientSide || existing == null || !event.getEffectInstance().is(ModRegistries.WITHERING_RIME)
                || source == null || !io.github.hammerknife404.dreadpatch.entity.RisenDread.dread(source) || DreadHits.overriding(source)
                || event.getResult() == MobEffectEvent.Applicable.Result.DO_NOT_APPLY) {
            return;
        }
        if (event.getEffectInstance().getAmplifier() > existing.getAmplifier()) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    private static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !event.getEffectInstance().is(ModRegistries.WITHERING_RIME)) {
            return;
        }
        Entity source = event.getEffectSource();
        if (source != null) {
            LAID_BY.put(entity, new Layer(source.getUUID(), source instanceof DreadQueen,
                    io.github.hammerknife404.dreadpatch.entity.RisenDread.dread(source)));
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

    static float dreadHeal(Difficulty difficulty) {
        return switch (difficulty) {
            case HARD -> DREAD_HEAL_HARD;
            case NORMAL -> DREAD_HEAL_NORMAL;
            default -> DREAD_HEAL_EASY;
        };
    }

    /** Her withering has just taken health from {@code victim}: the queen who laid it, if near, is fed. */
    static void feed(LivingEntity victim) {
        Layer layer = LAID_BY.get(victim);
        if (layer == null || !(layer.queen() || layer.dread()) || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        if (!layer.queen()) {
            // Any other of the Dread's: it's fed what its victim loses.
            if (level.getEntity(layer.id()) instanceof LivingEntity dread && dread.isAlive() && dread.distanceToSqr(victim) <= RANGE * RANGE
                    && dread.getHealth() < dread.getMaxHealth()) {
                dread.heal(dreadHeal(level.getDifficulty()));
                level.sendParticles(ParticleTypes.SNOWFLAKE, dread.getX(), dread.getY(0.6), dread.getZ(), 2, 0.3, 0.4, 0.3, 0.01);
            }
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
