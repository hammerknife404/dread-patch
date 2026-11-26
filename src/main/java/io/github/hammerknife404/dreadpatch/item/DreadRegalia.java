package io.github.hammerknife404.dreadpatch.item;

import com.iafenvoy.iceandfire.entity.DreadMobEntity;
import com.iafenvoy.iceandfire.entity.DreadQueenEntity;
import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.effect.RimeSiphon;
import io.github.hammerknife404.dreadpatch.entity.DreadPhantom;
import io.github.hammerknife404.dreadpatch.entity.Syndrigossa;
import io.github.hammerknife404.dreadpatch.magic.RegaliaBonuses;
import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.github.hammerknife404.dreadpatch.spell.RimefrostSchool;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Her regalia's powers (0.51.0, the owner's second drop; {@link RegaliaBonuses}):
 * <ul>
 * <li>indestructible (the {@code unbreakable} component, on every stack: a default component);</li>
 * <li>each piece worn: +150 max mana (Iron's), 5% off what an Ice or Rimefrost spell costs, 5% more
 * Rimefrost damage dealt and 5% less taken (the owner: it's "to add rimefrost damage", the wearer taking
 * less is "ok too", and "both ways on what counts as rimefrost"): {@code #dreadpatch:rimefrost} (her
 * school's magic, her breath, Shatter-Spine, Whiteout's cold) and Withering Rime's withering, which has
 * no attacker of its own, so it counts as dealt by whoever laid the rime ({@link RimeSiphon#layer});</li>
 * <li>the full set: no freezing (powder snow, Withering Rime's chill, any freezing damage), and the Dread
 * leave the wearer be. Ours: not the queen, nor Syndrigossa, nor anything a queen or Syndrigossa
 * commands (her fight can't be walked through in her clothes), and not one the wearer has hurt in the
 * last {@link #PROVOKED_TICKS}.</li>
 * </ul>
 * Any living thing wearing the set (the smoke test dresses a zombie), not only players; the mana is
 * Iron's, so players'.
 */
public final class DreadRegalia {
    /** Rimefrost damage: what the regalia strengthens, dealt, and guards against, taken. */
    public static final TagKey<DamageType> RIMEFROST = TagKey.create(Registries.DAMAGE_TYPE, DreadPatch.id("rimefrost"));
    /** On a dread mob the wearer hurt: who, and until when it may fight them. */
    static final String PROVOKED_BY = "dreadpatch:provoked_by";
    static final String PROVOKED_UNTIL = "dreadpatch:provoked_until";
    static final int PROVOKED_TICKS = 600;

    private DreadRegalia() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(DreadRegalia::onModifyDefaultComponents);
        gameBus.addListener(DreadRegalia::onAttributes);
        gameBus.addListener(DreadRegalia::onSpellCast);
        gameBus.addListener(DreadRegalia::onIncomingDamage);
        gameBus.addListener(DreadRegalia::onDamage);
        gameBus.addListener(DreadRegalia::onChangeTarget);
        gameBus.addListener(DreadRegalia::onTick);
    }

    /** How many pieces of it {@code entity} wears. */
    public static int pieces(LivingEntity entity) {
        int pieces = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (isRegalia(entity.getItemBySlot(slot))) {
                pieces++;
            }
        }
        return pieces;
    }

    public static boolean isRegalia(ItemStack stack) {
        return stack.getItem() instanceof DreadArmorItem armor && armor.kind() == DreadArmorItem.Kind.QUEEN;
    }

    private static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        DreadGear.regalia().forEach(piece -> event.modify(piece.get(), components -> components.set(DataComponents.UNBREAKABLE, new Unbreakable(true))));
    }

    /** +150 max mana, in the piece's own slot. */
    private static void onAttributes(ItemAttributeModifierEvent event) {
        if (event.getItemStack().getItem() instanceof DreadArmorItem armor && armor.kind() == DreadArmorItem.Kind.QUEEN) {
            ArmorItem.Type type = armor.getType();
            event.addModifier(AttributeRegistry.MAX_MANA, new AttributeModifier(DreadPatch.id("dread_queen_" + type.getName() + "_mana"),
                    RegaliaBonuses.MANA_PER_PIECE, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.bySlot(type.getSlot()));
        }
    }

    /** 5% a piece off an Ice or Rimefrost spell's mana. Iron's fires this as a player's spell goes off, before it takes the mana. */
    private static void onSpellCast(SpellOnCastEvent event) {
        int pieces = pieces(event.getEntity());
        if (pieces > 0 && RimefrostSchool.isIce(event.getSchoolType())) {
            event.setManaCost(RegaliaBonuses.manaCost(event.getManaCost(), pieces));
        }
    }

    /** 5% a piece off Rimefrost damage taken, after armour. */
    private static void onDamage(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        int pieces = pieces(victim);
        if (pieces > 0 && rimefrost(victim, event.getSource())) {
            event.setNewDamage(RegaliaBonuses.rimefrostDamageTaken(event.getNewDamage(), pieces));
        }
    }

    /** Rimefrost damage: tagged, or Withering Rime's withering (vanilla's wither damage, on someone under it and not under vanilla's Wither). */
    static boolean rimefrost(LivingEntity victim, DamageSource source) {
        return source.is(RIMEFROST) || withering(victim, source);
    }

    private static boolean withering(LivingEntity victim, DamageSource source) {
        return source.is(DamageTypes.WITHER) && victim.hasEffect(ModRegistries.WITHERING_RIME) && !victim.hasEffect(MobEffects.WITHER);
    }

    /** Who dealt Rimefrost damage: its causing entity, or for Withering Rime's withering, who laid the rime. */
    @Nullable
    private static Entity dealer(LivingEntity victim, DamageSource source) {
        if (source.getEntity() != null) {
            return source.getEntity();
        }
        return withering(victim, source) ? RimeSiphon.layer(victim) : null;
    }

    /**
     * The full set: no freezing damage. Each piece the attacker wears: 5% more Rimefrost damage (before
     * armour, as an attack's own strength is). And the Dread remember who hurt them.
     */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.getSource().is(DamageTypeTags.IS_FREEZING) && RegaliaBonuses.fullSet(pieces(victim))) {
            event.setCanceled(true);
            return;
        }
        if (rimefrost(victim, event.getSource()) && dealer(victim, event.getSource()) instanceof LivingEntity dealer) {
            int pieces = pieces(dealer);
            if (pieces > 0) {
                event.setAmount(RegaliaBonuses.rimefrostDamageDealt(event.getAmount(), pieces));
            }
        }
        if (victim instanceof IDreadMob && event.getSource().getEntity() instanceof LivingEntity attacker && RegaliaBonuses.fullSet(pieces(attacker))) {
            CompoundTag data = victim.getPersistentData();
            data.putUUID(PROVOKED_BY, attacker.getUUID());
            data.putLong(PROVOKED_UNTIL, victim.level().getGameTime() + PROVOKED_TICKS);
        }
    }

    /** The Dread don't take the set's wearer for a target, unless they may ({@link #mayFight}). */
    private static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target != null && !event.getEntity().level().isClientSide && !mayFight(event.getEntity(), target)) {
            event.setCanceled(true);
        }
    }

    /**
     * Each second: a dread mob holding the wearer as its target lets go (one it had before the set went on,
     * or handed to it outside the target goals). Each tick: the full set's wearer doesn't freeze.
     */
    private static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide) {
            return;
        }
        if (living.getTicksFrozen() > 0 && RegaliaBonuses.fullSet(pieces(living))) {
            living.setTicksFrozen(0);
        }
        if (living.tickCount % 20 == 0 && living instanceof Mob mob && mob.getTarget() != null && !mayFight(mob, mob.getTarget())) {
            mob.setTarget(null);
        }
    }

    /** Whether {@code mob} may fight {@code target}: anything may, but one of the Dread, the set's wearer, only as above. */
    static boolean mayFight(LivingEntity mob, LivingEntity target) {
        if (!(mob instanceof IDreadMob) || !RegaliaBonuses.fullSet(pieces(target))) {
            return true;
        }
        if (mob instanceof DreadQueenEntity || mob instanceof Syndrigossa || queensOwn(commander(mob))) {
            return true;
        }
        CompoundTag data = mob.getPersistentData();
        return data.hasUUID(PROVOKED_BY) && target.getUUID().equals(data.getUUID(PROVOKED_BY))
                && mob.level().getGameTime() < data.getLong(PROVOKED_UNTIL);
    }

    private static boolean queensOwn(@Nullable Entity commander) {
        return commander instanceof DreadQueenEntity || commander instanceof Syndrigossa;
    }

    /** Who a dread mob answers to: its {@code CommanderUUID} (Ice and Fire's own lookup misses for its scuttler and lich), a phantom's summoner. */
    @Nullable
    private static Entity commander(LivingEntity mob) {
        if (mob instanceof DreadPhantom phantom) {
            return phantom.summoner();
        }
        if (mob instanceof DreadMobEntity dread) {
            UUID id = dread.getCommanderId();
            return id != null && mob.level() instanceof ServerLevel level ? level.getEntity(id) : null;
        }
        return ((IDreadMob) mob).getCommander();
    }
}
