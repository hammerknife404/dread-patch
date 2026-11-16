package io.github.hammerknife404.dreadpatch.item;

import com.iafenvoy.iceandfire.registry.IafItems;
import com.iafenvoy.iceandfire.registry.IafParticles;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.entity.RimefireCrown;
import io.github.hammerknife404.dreadpatch.magic.DreadDamage;
import io.github.hammerknife404.dreadpatch.magic.RimeCombo;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Her sword, Ice and Fire's {@code iceandfire:dread_queen_sword} ("Blade of the Dreaded Queen"), a
 * plain {@code SwordItem} there (8 attack damage, 1.6 speed, 4,000 uses, nothing repairs it). The
 * chat's profile, all through its default components (every sword has them, including ones already
 * dropped) and events, for whoever wields it, her included:
 * <ul>
 * <li>11.5 attack damage at 1.6 speed, half a block more reach, unbreakable;</li>
 * <li>Thermodynamic Theft: every strike, sweeps included, leaves 4.5 s of Withering Rime (which
 * already stops all healing);</li>
 * <li>Glass-Rime Shatter ({@link RimeCombo}): every third full strike on the same chilled or frozen
 * target (each within 5 s of the last; strikes on others don't break the count) adds 10% of its max health (at most 20) past its armor, and leaves it
 * {@code Brittle} (armor -10%) for 4 s;</li>
 * <li>Fire-Drinking Guard: right-click raises it, as a shield (slowed, no swinging). Raised, it
 * stops fire and magic ({@link #GUARDED}) from the front, puts out fire on its holder and nearby,
 * and a blocked hit charges the blade ({@code dreadpatch:frost_charge}, shown by a glint): its next
 * full strike shatters at once. With the sword in the main hand, an off hand holding something
 * with its own right-click ({@link #hasOwnUse}: a shield, food, a bow, a block, a staff) keeps it.</li>
 * </ul>
 */
public final class DreadQueenSword {
    /** The chat's profile: tooltips count the player's base 1 attack damage and 4 attack speed in. */
    public static final double ATTACK_DAMAGE = 11.5;
    public static final double ATTACK_SPEED = 1.6;
    public static final double REACH = 0.5;
    /** Thermodynamic Theft: Withering Rime on every strike, as long as her bolt leaves it. */
    public static final int THEFT_TICKS = 90;
    /** Glass-Rime Shatter: the share of the target's max health it adds, and how long Brittle lasts. */
    public static final float SHATTER_SHARE = 0.10F;
    public static final int BRITTLE_TICKS = 80;
    /** A player's swing counts toward the shatter at this much of its charge or more. */
    static final float FULL_STRIKE = 0.9F;
    /** The guard: held as long as a shield can be, raised after a shield's 5 ticks, drinking fire this many blocks round. */
    static final int GUARD_DURATION = 72000;
    static final int GUARD_RAISE_TICKS = 5;
    static final int GUARD_FIRE_REACH = 2;
    /** What the guard stops: fire, magic (Iron's schools included) and Ice and Fire's dragon fire. */
    public static final TagKey<DamageType> GUARDED = TagKey.create(Registries.DAMAGE_TYPE, DreadPatch.id("fire_drinking_guard"));

    private static final ParticleOptions ICE_SHARDS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());
    /**
     * Server thread only. Combos per wielder, and each player's last swing (whom at, and when) to
     * tell it from the vanilla sweep. Better Combat swings go through {@code Player.attack} once per
     * target, so each of their targets counts as the swing's own.
     */
    private static final Map<LivingEntity, RimeCombo> COMBOS = new WeakHashMap<>();
    private static final Map<Player, Swing> SWINGS = new WeakHashMap<>();

    private record Swing(int targetId, long time) {
    }

    private DreadQueenSword() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(DreadQueenSword::onModifyDefaultComponents);
        gameBus.addListener(DreadQueenSword::onRightClick);
        gameBus.addListener(DreadQueenSword::onUseStart);
        gameBus.addListener(DreadQueenSword::onUseTick);
        gameBus.addListener(DreadQueenSword::onIncomingDamage);
        gameBus.addListener(DreadQueenSword::onAttack);
        gameBus.addListener(DreadQueenSword::onDamage);
    }

    public static boolean is(ItemStack stack) {
        return stack.is(IafItems.DREAD_QUEEN_SWORD.get());
    }

    public static boolean charged(ItemStack stack) {
        return stack.has(ModRegistries.FROST_CHARGE);
    }

    private static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        event.modify(IafItems.DREAD_QUEEN_SWORD.get(), components -> components
                .set(DataComponents.ATTRIBUTE_MODIFIERS, attributes())
                .set(DataComponents.UNBREAKABLE, new Unbreakable(true)));
    }

    /** As vanilla swords give theirs (the base ids, so tooltips show the totals), plus the reach. */
    static ItemAttributeModifiers attributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, ATTACK_DAMAGE - 1.0,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, ATTACK_SPEED - 4.0,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(DreadPatch.id("dread_queen_sword_reach"), REACH,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    // Fire-Drinking Guard

    /** Raises the guard, on both sides (as a shield's use does). */
    private static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!is(event.getItemStack()) || player.isUsingItem()
                || (event.getHand() == InteractionHand.MAIN_HAND && hasOwnUse(player.getOffhandItem()))) {
            return;
        }
        player.startUsingItem(event.getHand());
        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
    }

    /** Off-hand things that keep their own right-click behind her sword: shields, food, bows, blocks, throwables, staffs. */
    static boolean hasOwnUse(ItemStack stack) {
        return !stack.isEmpty() && (stack.getUseAnimation() != UseAnim.NONE || stack.getItem() instanceof BlockItem
                || stack.getItem() instanceof ProjectileItem || stack.getItem() instanceof EnderpearlItem
                || stack.has(ComponentRegistry.CASTING_IMPLEMENT));
    }

    /** A sword's use lasts no time at all; the guard's lasts as long as a shield's. */
    private static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (is(event.getItem())) {
            event.setDuration(GUARD_DURATION);
        }
    }

    static boolean guarding(LivingEntity entity) {
        return entity.isUsingItem() && is(entity.getUseItem()) && GUARD_DURATION - entity.getUseItemRemainingTicks() >= GUARD_RAISE_TICKS;
    }

    /** Raised, it drinks the fire on its holder and round them. */
    private static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !is(event.getItem()) || !guarding(entity) || entity.tickCount % 5 != 0) {
            return;
        }
        entity.clearFire();
        BlockPos at = entity.blockPosition();
        int r = GUARD_FIRE_REACH;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-r, -1, -r), at.offset(r, 2, r))) {
            RimefireCrown.drinkFire(entity.level(), pos);
        }
    }

    /** Raised, it stops fire and magic from the front, and each block charges the blade. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        if (!guarding(entity) || !source.is(GUARDED) || source.is(DamageTypeTags.BYPASSES_SHIELD) || !inFront(entity, source.getSourcePosition())) {
            return;
        }
        event.setCanceled(true);
        ItemStack sword = entity.getUseItem();
        if (!charged(sword)) {
            sword.set(ModRegistries.FROST_CHARGE, Unit.INSTANCE);
            sword.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, entity.getSoundSource(), 1.0F, 1.4F);
        }
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, entity.getSoundSource(), 0.6F, 1.4F);
        if (entity.level() instanceof ServerLevel level) {
            Vec3 guard = entity.getEyePosition().add(Vec3.directionFromRotation(0.0F, entity.getYHeadRot()).scale(0.6)).subtract(0.0, 0.4, 0.0);
            level.sendParticles(ICE_SHARDS, guard.x, guard.y, guard.z, 10, 0.2, 0.3, 0.2, 0.1);
            level.sendParticles(IafParticles.DREAD_TORCH.get(), guard.x, guard.y, guard.z, 6, 0.2, 0.3, 0.2, 0.02);
        }
    }

    /** Whether a hit from {@code from} comes at its front, as a shield judges it (level, by where the head faces). */
    static boolean inFront(LivingEntity entity, @Nullable Vec3 from) {
        if (from == null) {
            return false;
        }
        Vec3 facing = Vec3.directionFromRotation(0.0F, entity.getYHeadRot());
        Vec3 incoming = from.vectorTo(entity.position());
        return new Vec3(incoming.x, 0.0, incoming.z).normalize().dot(facing) < 0.0;
    }

    // Thermodynamic Theft and Glass-Rime Shatter

    /** A player's swing, before its damage: whom it's at, so the shatter can tell it from the sweep. */
    private static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide && is(player.getMainHandItem())) {
            SWINGS.put(player, new Swing(event.getTarget().getId(), player.level().getGameTime()));
        }
    }

    /**
     * After armor and resistance, before absorption: the shatter's share lands past armor. Swings
     * still have their charge here (NeoForge resets it after the attack).
     */
    private static void onDamage(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker
                || !(source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK))) {
            return;
        }
        ItemStack sword = attacker.getMainHandItem();
        LivingEntity target = event.getEntity();
        if (!is(sword) || target == attacker) {
            return;
        }
        boolean chilled = target.hasEffect(ModRegistries.WITHERING_RIME) || target.getTicksFrozen() > 0;
        target.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, THEFT_TICKS), attacker);
        boolean full = true;
        if (attacker instanceof Player player) {
            Swing swing = SWINGS.get(player);
            if (swing == null || swing.targetId() != target.getId() || swing.time() != player.level().getGameTime()) {
                return; // the sweep
            }
            full = player.getAttackStrengthScale(0.5F) >= FULL_STRIKE;
        }
        boolean charged = charged(sword);
        if (!COMBOS.computeIfAbsent(attacker, a -> new RimeCombo()).strike(target.getUUID(), attacker.level().getGameTime(), full, chilled, charged)) {
            return;
        }
        event.setNewDamage(event.getNewDamage() + DreadDamage.maxHealthShare(target.getMaxHealth(), SHATTER_SHARE));
        target.addEffect(new MobEffectInstance(ModRegistries.BRITTLE, BRITTLE_TICKS), attacker);
        if (charged) {
            sword.remove(ModRegistries.FROST_CHARGE);
            sword.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ICE_SHARDS, target.getX(), target.getY(0.5), target.getZ(), 30, 0.4, 0.5, 0.4, 0.2);
            level.sendParticles(IafParticles.DREAD_TORCH.get(), target.getX(), target.getY(0.5), target.getZ(), 12, 0.4, 0.5, 0.4, 0.05);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, attacker.getSoundSource(), 1.2F, 0.6F);
        }
    }
}
