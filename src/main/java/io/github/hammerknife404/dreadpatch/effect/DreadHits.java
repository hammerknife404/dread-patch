package io.github.hammerknife404.dreadpatch.effect;

import com.iafenvoy.iceandfire.entity.DreadKnightEntity;
import com.iafenvoy.iceandfire.entity.DreadScuttlerEntity;
import io.github.hammerknife404.dreadpatch.entity.DreadPhantom;
import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import io.github.hammerknife404.dreadpatch.entity.RimeEel;
import io.github.hammerknife404.dreadpatch.entity.RimefrostDragon;
import io.github.hammerknife404.dreadpatch.entity.RisenDread;
import io.github.hammerknife404.dreadpatch.magic.DreadDebuffs;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The Dread's hits and the one-source cap (0.73.0, the owner's Feature_Dump_11-24; the rules in
 * {@link DreadDebuffs}).
 * <ul>
 * <li>A Dread mob's melee hit that harms rolls what it leaves by its kind: scuttler, knight, or the
 * rest (Ice and Fire's thralls, ghouls, beasts, liches, and the risen). The queen, the Rimefrost dragons,
 * the phantoms and the Dread Fish keep their own, but for the phantoms, which lay Rimefrost a third of the time, their
 * melee hits roll {@link DreadDebuffs#RIMEFROST_FLOOR} for it (0.85.0). An unarmed thrall's claws roll their own (0.85.0).</li>
 * <li>On a player, Withering Rime and Rimefrost are each one source's at a time: another's laying is
 * refused while it lasts (so a pack can't pile it on), the same source may renew it, and the queen and
 * Syndrigossa always may. Laid with no source (the weather), it's anyone's.</li>
 * </ul>
 */
public final class DreadHits {
    /** Server thread only: who laid each player's capped effects. */
    private static final Map<LivingEntity, Map<Holder<MobEffect>, UUID>> LAID_BY = new WeakHashMap<>();

    private DreadHits() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(DreadHits::onDamaged);
        // After the Dread's own refusal (DreadEvents), so only what would land is counted.
        gameBus.addListener(EventPriority.LOW, DreadHits::onApplicable);
        gameBus.addListener(DreadHits::onAdded);
        gameBus.addListener(DreadHits::onRemoved);
        gameBus.addListener(DreadHits::onExpired);
    }

    static boolean capped(Holder<MobEffect> effect) {
        return effect.is(ModRegistries.WITHERING_RIME) || effect.is(ModRegistries.RIMEFROST);
    }

    /** Whose laying always lands. */
    static boolean overriding(Entity source) {
        return source instanceof DreadQueen || source instanceof io.github.hammerknife404.dreadpatch.entity.Syndrigossa;
    }

    /** Which of the Dread's rules this attacker's melee hits follow, or null if its own. */
    static DreadDebuffs.Kind kind(Entity attacker) {
        if (!RisenDread.dread(attacker) || attacker instanceof DreadQueen || attacker instanceof RimefrostDragon
                || attacker instanceof DreadPhantom || attacker instanceof RimeEel) {
            return null;
        }
        if (attacker instanceof com.iafenvoy.iceandfire.entity.DreadThrallEntity thrall && thrall.getMainHandItem().isEmpty()) {
            return DreadDebuffs.Kind.UNARMED_THRALL;
        }
        if (attacker instanceof DreadScuttlerEntity) {
            return DreadDebuffs.Kind.SCUTTLER;
        }
        return attacker instanceof DreadKnightEntity ? DreadDebuffs.Kind.KNIGHT : DreadDebuffs.Kind.OTHER;
    }

    private static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (victim.level().isClientSide || event.getNewDamage() <= 0.0F || attacker == null || source.getDirectEntity() != attacker
                || !(source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)) || source.is(DamageTypeTags.IS_PROJECTILE)
                || !(attacker instanceof LivingEntity dread)) {
            return;
        }
        DreadDebuffs.Kind kind = kind(dread);
        if (kind == null) {
            // The queen, a Rimefrost dragon, a Dread Fish: their own, and the floor's Rimefrost (a phantom's own is more).
            int floor = DreadDebuffs.floor(dread.getRandom().nextFloat());
            if (RisenDread.dread(dread) && !(dread instanceof DreadPhantom) && floor > 0) {
                victim.addEffect(new MobEffectInstance(ModRegistries.RIMEFROST, floor, RimefrostEffect.HEAVY), dread);
            }
            return;
        }
        DreadDebuffs.Hit hit = DreadDebuffs.roll(kind, dread.getRandom().nextFloat(), dread.getRandom().nextFloat());
        if (hit.witheringTicks() > 0) {
            victim.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, hit.witheringTicks(), hit.witheringAmplifier()), dread);
        }
        if (hit.rimefrostTicks() > 0) {
            victim.addEffect(new MobEffectInstance(ModRegistries.RIMEFROST, hit.rimefrostTicks(), RimefrostEffect.HEAVY), dread);
        }
    }

    private static void onApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        Holder<MobEffect> effect = event.getEffectInstance().getEffect();
        if (!(entity instanceof Player) || entity.level().isClientSide || !capped(effect) || !entity.hasEffect(effect)
                || event.getResult() == MobEffectEvent.Applicable.Result.DO_NOT_APPLY) {
            return;
        }
        Entity source = event.getEffectSource();
        Map<Holder<MobEffect>, UUID> laid = LAID_BY.get(entity);
        UUID current = laid == null ? null : laid.get(effect);
        if (!DreadDebuffs.lands(current, source == null ? null : source.getUUID(), overriding(source))) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    private static void onAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        Holder<MobEffect> effect = event.getEffectInstance().getEffect();
        if (!(entity instanceof Player) || entity.level().isClientSide || !capped(effect)) {
            return;
        }
        Entity source = event.getEffectSource();
        if (source != null) {
            LAID_BY.computeIfAbsent(entity, e -> new HashMap<>()).put(effect, source.getUUID());
        } else if (event.getOldEffectInstance() == null) {
            forget(entity, effect);
        }
    }

    private static void onRemoved(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() != null) {
            forget(event.getEntity(), event.getEffectInstance().getEffect());
        }
    }

    private static void onExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null) {
            forget(event.getEntity(), event.getEffectInstance().getEffect());
        }
    }

    private static void forget(LivingEntity entity, Holder<MobEffect> effect) {
        Map<Holder<MobEffect>, UUID> laid = LAID_BY.get(entity);
        if (laid != null) {
            laid.remove(effect);
        }
    }
}
