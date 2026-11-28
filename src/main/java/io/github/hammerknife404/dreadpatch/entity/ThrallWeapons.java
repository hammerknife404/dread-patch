package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadThrallEntity;
import com.iafenvoy.iceandfire.registry.IafItems;
import io.github.hammerknife404.dreadpatch.magic.ThrallArms;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.EntityHitResult;

import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A dread thrall's weapon (0.84.4, {@link ThrallArms}; the owner: "make sure dread thralls can spawn with a bow or cross
 * bow, and never without a weapon").
 * <ul>
 * <li>Never empty-handed: one that joins a level with nothing in its main hand (Ice and Fire leaves one in four so, and
 * the storm raises the unarmed dead in their own gear) is given a bow, a crossbow or a dread sword.</li>
 * <li>Ice and Fire's thrall has only its melee goal, so one holding a bow or crossbow (given, or a skeleton's raised in
 * her storm) gets {@link ShootGoal} over it: it draws or charges, and looses an arrow as a skeleton does. Goals aren't
 * saved, so it's added on every join; it stands aside whenever the thrall holds anything else, and the melee goal
 * runs.</li>
 * <li>Unarmed now and then (0.85.0, {@link ThrallArms}): its arming is rolled once, on its first join, and remembered
 * ({@link #ARMED}), so one left unarmed stays so. Unarmed, its hits are a ghoul's claws ({@link #onIncoming},
 * {@link #onHit}, {@link #onTick}): half a ghoul's damage, twice an attack, the second {@link ThrallArms#SECOND_SLASH_TICKS}
 * on if its target is still in reach; what they leave is rolled in {@code effect/DreadHits}.</li>
 * <li>No friendly fire (0.85.0, the owner: "Disable friendly-fire projectile damage from bows/crossbows against Dread and
 * Rimefrost mobs"): an arrow any of the Dread shot flies through the Dread ({@link #onImpact}).</li>
 * </ul>
 */
public final class ThrallWeapons {
    private ThrallWeapons() {
    }

    /** Persistent data: its arming is rolled (0.85.0), so an unarmed one isn't armed when it's loaded again. */
    static final String ARMED = "dreadpatch:armed";
    /** Server thread only: an unarmed thrall's second slash due, its target and the tick. */
    private static final Map<DreadThrallEntity, Slash> SECOND = new WeakHashMap<>();
    /** Set while a second slash lands, so it isn't followed by another. */
    private static boolean slashing;

    private record Slash(int target, long due) {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ThrallWeapons::onJoin);
        gameBus.addListener(ThrallWeapons::onIncoming);
        gameBus.addListener(ThrallWeapons::onHit);
        gameBus.addListener(ThrallWeapons::onTick);
        gameBus.addListener(ThrallWeapons::keepBow);
        gameBus.addListener(ThrallWeapons::giveBowBack);
        gameBus.addListener(ThrallWeapons::onImpact);
    }

    /** Whether it fights with a ghoul's claws: a thrall with nothing in its main hand. */
    public static boolean unarmed(Entity entity) {
        return entity instanceof DreadThrallEntity thrall && thrall.getMainHandItem().isEmpty();
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof DreadThrallEntity thrall)) {
            return;
        }
        if (!thrall.getPersistentData().getBoolean(ARMED)) {
            thrall.getPersistentData().putBoolean(ARMED, true);
            if (thrall.getMainHandItem().isEmpty()) {
                ItemStack weapon = switch (ThrallArms.weapon(thrall.getRandom().nextDouble())) {
                    case NONE -> ItemStack.EMPTY;
                    case BOW -> new ItemStack(Items.BOW);
                    case CROSSBOW -> new ItemStack(Items.CROSSBOW);
                    case DREAD_SWORD -> new ItemStack(IafItems.DREAD_SWORD.get());
                };
                thrall.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            }
        }
        // Over its melee goal (2): the same flags, so only one of them runs.
        thrall.goalSelector.addGoal(1, new ShootGoal(thrall));
    }

    /** An unarmed thrall's hit: a claw, half a ghoul's damage (its own scaled, so difficulty still counts). */
    private static void onIncoming(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (source.getDirectEntity() == source.getEntity() && unarmed(source.getEntity()) && source.is(DamageTypes.MOB_ATTACK)) {
            event.setAmount((float) (event.getAmount() * ThrallArms.clawScale()));
        }
    }

    /** A claw that landed: the second follows, unless this is it. */
    private static void onHit(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!slashing && source.getDirectEntity() == source.getEntity() && source.is(DamageTypes.MOB_ATTACK)
                && source.getEntity() instanceof DreadThrallEntity thrall && unarmed(thrall) && !thrall.level().isClientSide) {
            SECOND.put(thrall, new Slash(event.getEntity().getId(), thrall.level().getGameTime() + ThrallArms.SECOND_SLASH_TICKS));
        }
    }

    /**
     * Its bow kept (0.95.0): Ice and Fire's thrall puts a bone in its hand in place of a bow every tick
     * ({@code DreadThrallEntity.aiStep}), so the bows 0.84.4 gave thralls never shot (only crossbows did). Its bow is noted
     * before its tick and handed back after: its goals (in {@code aiStep}, before the swap) see the bow, and its equipment
     * is sent at the start of a tick, so the bone is never seen or saved.
     */
    private static void keepBow(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof DreadThrallEntity thrall && !thrall.level().isClientSide && thrall.getMainHandItem().is(Items.BOW)) {
            BOWS.put(thrall, thrall.getMainHandItem().copy());
        }
    }

    private static void giveBowBack(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof DreadThrallEntity thrall && !thrall.level().isClientSide) {
            ItemStack bow = BOWS.remove(thrall);
            if (bow != null && thrall.getMainHandItem().is(Items.BONE)) {
                thrall.setItemSlot(EquipmentSlot.MAINHAND, bow);
            }
        }
    }

    private static final Map<DreadThrallEntity, ItemStack> BOWS = new WeakHashMap<>();

    private static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof DreadThrallEntity thrall) || thrall.level().isClientSide) {
            return;
        }
        Slash slash = SECOND.get(thrall);
        if (slash == null || thrall.level().getGameTime() < slash.due()) {
            return;
        }
        SECOND.remove(thrall);
        if (thrall.isAlive() && unarmed(thrall) && thrall.level().getEntity(slash.target()) instanceof LivingEntity target && target.isAlive()
                && thrall.isWithinMeleeAttackRange(target) && thrall.getSensing().hasLineOfSight(target)) {
            thrall.swing(InteractionHand.MAIN_HAND);
            slashing = true;
            try {
                target.hurt(thrall.damageSources().mobAttack(thrall), (float) thrall.getAttributeValue(Attributes.ATTACK_DAMAGE));
            } finally {
                slashing = false;
            }
        }
    }

    /** An arrow of the Dread's flies through the Dread (a dragon's parts count as their dragon). */
    private static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !RisenDread.dread(arrow.getOwner())) {
            return;
        }
        Entity struck = hit.getEntity() instanceof PartEntity<?> part ? part.getParent() : hit.getEntity();
        if (RisenDread.dread(struck)) {
            event.setCanceled(true);
        }
    }

    /** Whether it holds a bow or a crossbow in its main hand. */
    public static boolean ranged(ItemStack stack) {
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
    }

    /** A thrall's bow or crossbow: keep in range and sight, ready it, loose. */
    static final class ShootGoal extends Goal {
        private final DreadThrallEntity thrall;
        private int cooldown;

        ShootGoal(DreadThrallEntity thrall) {
            this.thrall = thrall;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.thrall.getTarget();
            return target != null && target.isAlive() && ranged(this.thrall.getMainHandItem());
        }

        @Override
        public void stop() {
            this.thrall.stopUsingItem();
            this.cooldown = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.thrall.getTarget();
            if (target == null) {
                return;
            }
            this.thrall.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = this.thrall.distanceTo(target);
            boolean seen = this.thrall.getSensing().hasLineOfSight(target);
            if (!seen || distance > ThrallArms.CLOSE_TO) {
                this.thrall.getNavigation().moveTo(target, 1.0);
            } else {
                this.thrall.getNavigation().stop();
            }
            if (this.cooldown > 0) {
                this.cooldown--;
                return;
            }
            ItemStack weapon = this.thrall.getMainHandItem();
            boolean crossbow = weapon.getItem() instanceof CrossbowItem;
            if (!this.thrall.isUsingItem()) {
                if (seen && distance <= ThrallArms.RANGE) {
                    this.thrall.startUsingItem(InteractionHand.MAIN_HAND);
                }
                return;
            }
            if (!seen || distance > ThrallArms.RANGE) {
                this.thrall.stopUsingItem();
                return;
            }
            if (this.thrall.getTicksUsingItem() >= ThrallArms.readyTicks(crossbow)) {
                this.thrall.stopUsingItem();
                this.shoot(target, weapon, crossbow);
                this.cooldown = crossbow ? ThrallArms.CROSSBOW_COOLDOWN : ThrallArms.BOW_COOLDOWN;
            }
        }

        /** As a skeleton shoots ({@code AbstractSkeleton.performRangedAttack}). */
        private void shoot(LivingEntity target, ItemStack weapon, boolean crossbow) {
            float velocity = crossbow ? ThrallArms.CROSSBOW_VELOCITY : ThrallArms.BOW_VELOCITY;
            ItemStack arrowStack = new ItemStack(Items.ARROW);
            if (this.thrall.getRandom().nextFloat() < ThrallArms.TIPPED_CHANCE) {
                arrowStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.TIPPED_ARROW,
                        this.thrall.getRandom().nextBoolean() ? io.github.hammerknife404.dreadpatch.registry.DreadPotions.RIMEFROST
                                : io.github.hammerknife404.dreadpatch.registry.DreadPotions.WITHERING_RIME);
            }
            AbstractArrow arrow = ProjectileUtil.getMobArrow(this.thrall, arrowStack, 1.0F, weapon);
            if (weapon.getItem() instanceof BowItem bow) {
                arrow = bow.customArrow(arrow, arrowStack, weapon);
            }
            double dx = target.getX() - this.thrall.getX();
            double dy = target.getY(1.0 / 3.0) - arrow.getY();
            double dz = target.getZ() - this.thrall.getZ();
            double flat = Math.sqrt(dx * dx + dz * dz);
            arrow.shoot(dx, dy + flat * (crossbow ? 0.1 : 0.2), dz, velocity,
                    ThrallArms.inaccuracy(this.thrall.level().getDifficulty().getId()));
            this.thrall.playSound(crossbow ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.SKELETON_SHOOT, 1.0F,
                    1.0F / (this.thrall.getRandom().nextFloat() * 0.4F + 0.8F));
            this.thrall.level().addFreshEntity(arrow);
        }
    }
}
