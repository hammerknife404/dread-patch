package io.github.hammerknife404.dreadpatch.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import com.iafenvoy.iceandfire.entity.DreadKnightEntity;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.magic.KnightRules;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.lang.reflect.Field;

/**
 * Ice and Fire's dread knights, grown (0.75.0, {@link KnightRules}): half again as large (vanilla's scale
 * attribute, so its box, reach, model and gear all grow), an eighth faster, stepping a full block, and
 * crouching like a player into a 2-block passage.
 * <ul>
 * <li>The modifiers are transient, set as it joins a level (so a knight saved before this grows too).</li>
 * <li>Its navigation plans for its crouched height (a {@link WalkNodeEvaluator} that sees it 2 blocks tall),
 * so it paths into 2-block passages; a server tick crouches it ({@link Pose#CROUCHING}) whenever its standing
 * box, here or a step ahead, would reach into a block above its crouched height, and stands it when there's
 * room. Crouched, {@link EntityEvent.Size} gives it a box {@link KnightRules#CROUCH_HEIGHT} tall: honest, so
 * its own box is what fits. Vanilla's melee goal (Ice and Fire's) already needs a line of sight from its eyes,
 * which crouch with it.</li>
 * <li>Drawn crouched by {@code client/DreadKnightCrouch}.</li>
 * </ul>
 */
public final class DreadKnights {
    private static final Field NAVIGATION = ObfuscationReflectionHelper.findField(Mob.class, "navigation");

    private DreadKnights() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadKnights::onJoin);
        gameBus.addListener(DreadKnights::onSize);
        gameBus.addListener(DreadKnights::onTick);
        if (ModList.get().isLoaded("better_mob_combat_reimagined")) {
            gameBus.addListener(DreadKnights::onEquipment);
            gameBus.addListener(DreadKnights::onDrops);
        }
    }

    private static void modifier(DreadKnightEntity knight, Holder<Attribute> attribute, String name, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = knight.getAttribute(attribute);
        if (instance != null) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(DreadPatch.id(name), amount, operation));
        }
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof DreadKnightEntity knight)) {
            return;
        }
        modifier(knight, Attributes.SCALE, "knight_scale", KnightRules.SCALE_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(knight, Attributes.MOVEMENT_SPEED, "knight_speed", KnightRules.SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(knight, Attributes.STEP_HEIGHT, "knight_step", KnightRules.STEP_BONUS, AttributeModifier.Operation.ADD_VALUE);
        knight.refreshDimensions();
        if (!event.getLevel().isClientSide && ModList.get().isLoaded("better_mob_combat_reimagined")) {
            reach(knight.getMainHandItem());
        }
        if (!event.getLevel().isClientSide && !(knight.getNavigation() instanceof CrouchingNavigation)) {
            try {
                NAVIGATION.set(knight, new CrouchingNavigation(knight, event.getLevel()));
            } catch (IllegalAccessException e) {
                DreadPatch.LOGGER.warn("Couldn't give a dread knight its crouching navigation", e);
            }
        }
    }

    /**
     * Its reach, grown with it (0.85.0), under Better Mob Combat Reimagined: that mod swings a mob's weapon 2.5 blocks
     * however large the mob, unless the weapon itself carries entity interaction range, which it adds. So a knight's
     * weapon carries {@link KnightRules#reachBonus} of it, while the knight holds it ({@link #REACH}); taken off its
     * drops ({@link #onDrops}). Without that mod a knight's reach grows with its box already (vanilla's melee check).
     */
    static final ResourceLocation REACH = DreadPatch.id("knight_reach");

    static void reach(ItemStack weapon) {
        if (weapon.isEmpty()) {
            return;
        }
        ItemAttributeModifiers modifiers = weapon.getAttributeModifiers();
        if (modifiers.modifiers().stream().anyMatch(entry -> entry.modifier().id().equals(REACH))) {
            return;
        }
        weapon.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers.withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(REACH, KnightRules.reachBonus(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND));
    }

    static void unreach(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null || modifiers.modifiers().stream().noneMatch(entry -> entry.modifier().id().equals(REACH))) {
            return;
        }
        ItemAttributeModifiers.Builder kept = ItemAttributeModifiers.builder();
        modifiers.modifiers().stream().filter(entry -> !entry.modifier().id().equals(REACH))
                .forEach(entry -> kept.add(entry.attribute(), entry.modifier(), entry.slot()));
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, kept.build().withTooltip(modifiers.showInTooltip()));
    }

    private static void onEquipment(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof DreadKnightEntity knight && !knight.level().isClientSide && event.getSlot() == EquipmentSlot.MAINHAND) {
            unreach(event.getFrom());
            reach(knight.getMainHandItem());
        }
    }

    private static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof DreadKnightEntity) {
            event.getDrops().forEach(item -> unreach(item.getItem()));
        }
    }

    private static void onSize(EntityEvent.Size event) {
        if (event.getEntity() instanceof DreadKnightEntity && event.getPose() == Pose.CROUCHING) {
            EntityDimensions standing = event.getNewSize();
            float height = KnightRules.crouchedHeight(standing.height());
            event.setNewSize(EntityDimensions.scalable(standing.width(), height).withEyeHeight(height * KnightRules.CROUCH_EYES));
        }
    }

    /** Its standing box's height, whatever its pose. */
    static float standingHeight(DreadKnightEntity knight) {
        return knight.getType().getDimensions().scale(knight.getScale()).height();
    }

    /** Whether the part of its standing box above its crouched height, set at {@code at}, would be in a block. */
    static boolean lowCeiling(DreadKnightEntity knight, Vec3 at) {
        float standing = standingHeight(knight);
        float crouched = KnightRules.crouchedHeight(standing);
        double half = knight.getBbWidth() / 2.0;
        AABB upper = new AABB(at.x - half, at.y + crouched, at.z - half, at.x + half, at.y + standing, at.z + half);
        return knight.level().getBlockCollisions(knight, upper).iterator().hasNext();
    }

    private static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof DreadKnightEntity knight) || knight.level().isClientSide || !knight.isAlive()) {
            return;
        }
        Pose pose = knight.getPose();
        if (pose != Pose.STANDING && pose != Pose.CROUCHING) {
            return;
        }
        boolean low = !knight.isPassenger() && (lowCeiling(knight, knight.position()) || lowCeiling(knight, ahead(knight)));
        if (low && pose != Pose.CROUCHING) {
            knight.setPose(Pose.CROUCHING);
        } else if (!low && pose == Pose.CROUCHING) {
            knight.setPose(Pose.STANDING);
        }
    }

    /** Where it's about to be: its path's next node, else a little along its motion. */
    private static Vec3 ahead(DreadKnightEntity knight) {
        Path path = knight.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            Node next = path.getNextNode();
            return new Vec3(next.x + 0.5, next.y, next.z + 0.5);
        }
        Vec3 motion = knight.getDeltaMovement().multiply(1.0, 0.0, 1.0);
        return motion.lengthSqr() < 1e-6 ? knight.position() : knight.position().add(motion.normalize().scale(0.6));
    }

    /** A ground navigation that plans for its crouched height. */
    static final class CrouchingNavigation extends GroundPathNavigation {
        CrouchingNavigation(Mob mob, Level level) {
            super(mob, level);
        }

        @Override
        protected PathFinder createPathFinder(int maxVisitedNodes) {
            this.nodeEvaluator = new WalkNodeEvaluator() {
                @Override
                public void prepare(PathNavigationRegion region, Mob mob) {
                    super.prepare(region, mob);
                    this.entityHeight = (int) Math.floor(KnightRules.crouchedHeight(mob.getBbHeight()) + 1.0F);
                }
            };
            this.nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
        }
    }
}
