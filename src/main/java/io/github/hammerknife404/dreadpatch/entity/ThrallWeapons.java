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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.EnumSet;

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
 * </ul>
 */
public final class ThrallWeapons {
    private ThrallWeapons() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ThrallWeapons::onJoin);
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof DreadThrallEntity thrall)) {
            return;
        }
        if (thrall.getMainHandItem().isEmpty()) {
            thrall.setItemSlot(EquipmentSlot.MAINHAND, switch (ThrallArms.weapon(thrall.getRandom().nextDouble())) {
                case BOW -> new ItemStack(Items.BOW);
                case CROSSBOW -> new ItemStack(Items.CROSSBOW);
                case DREAD_SWORD -> new ItemStack(IafItems.DREAD_SWORD.get());
            });
        }
        // Over its melee goal (2): the same flags, so only one of them runs.
        thrall.goalSelector.addGoal(1, new ShootGoal(thrall));
    }

    /** Whether it holds a bow or a crossbow in its main hand. */
    static boolean ranged(ItemStack stack) {
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
