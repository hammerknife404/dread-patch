package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadBeastEntity;
import com.iafenvoy.iceandfire.entity.DreadScuttlerEntity;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.magic.DreadFishRules;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;

/**
 * The Dread eating what's dropped (0.83.0, the owner: "Extend the food eating behavior of the dread fish to the dread
 * beasts. Plan for a list or method that allows the dread to consume most organic dropped items, but only food heals").
 * What they eat is {@link #EATS} ({@code #dreadpatch:dread_eats}: most organic things: food, seeds and crops, eggs,
 * mushrooms, meat and fish, bones, leather and hide, feathers, string, wool, slime, ink, spider eyes, saplings, leaves,
 * flowers) or anything edible, but never {@link #REFUSES} ({@code #dreadpatch:dread_refuses}: golden apples and carrots);
 * a pack can add to either. Only food heals ({@link DreadFishRules#heal}: an eighth of its nutrition, 0.25 to 1); the rest is
 * just eaten. Dread Fish (since 0.71.0, their own list until 0.83.0), Ice and Fire's Dread Beasts and (0.84.0, the owner:
 * "extend the beast eating to scuttlers too") its Dread Scuttlers eat, with nothing to fight: the nearest such item in
 * reach of their search, one at a time.
 * <p>Fed or not (0.85.0, the owner: "Enable eating trait by default even at full health/hunger (lower AI priority if
 * uninjured)"): a beast or scuttler eats whatever its health (and since 0.86.0 dread phantoms swoop on it: their own
 * {@code DreadPhantom.FeedGoal}), but a hurt one puts eating before wandering
 * ({@link #HURT_PRIORITY}) and a whole one after it ({@link #WHOLE_PRIORITY}), so it eats only when idle.
 */
public final class DreadFeeding {
    public static final TagKey<Item> EATS = TagKey.create(Registries.ITEM, DreadPatch.id("dread_eats"));
    public static final TagKey<Item> REFUSES = TagKey.create(Registries.ITEM, DreadPatch.id("dread_refuses"));
    /** How far a beast or a scuttler looks for something to eat. */
    static final double BEAST_SEARCH = 8.0;
    /** Its eating, hurt: over Ice and Fire's wandering (5); whole, under it (and its looking about, which don't share a flag). */
    static final int HURT_PRIORITY = 4;
    static final int WHOLE_PRIORITY = 6;

    private DreadFeeding() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadFeeding::onJoin);
    }

    /** Goals aren't saved: a Dread Beast's or a scuttler's is added each time it joins a level. */
    private static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && (event.getEntity() instanceof DreadBeastEntity || event.getEntity() instanceof DreadScuttlerEntity)) {
            Mob mob = (Mob) event.getEntity();
            mob.goalSelector.addGoal(HURT_PRIORITY, new EatDroppedGoal(mob, BEAST_SEARCH, Boolean.TRUE));
            mob.goalSelector.addGoal(WHOLE_PRIORITY, new EatDroppedGoal(mob, BEAST_SEARCH, Boolean.FALSE));
        }
    }

    /** Whether the Dread eat it. */
    public static boolean eats(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(REFUSES) && (stack.is(EATS) || stack.has(DataComponents.FOOD));
    }

    /** Eats one of the item's stack: healing by it if it's food, crumbs (or bubbles, in water) and a bite's sound either way. */
    public static void eat(Mob mob, ItemEntity item) {
        // Another may have eaten it, or it merged away, this same tick: an empty stack crashed the server's tick (0.83.0's
        // smoke test: crumbs drawn from an empty stack throw).
        if (!item.isAlive() || item.getItem().isEmpty()) {
            return;
        }
        ItemStack stack = item.getItem().copy();
        ItemStack bite = stack.copyWithCount(1);
        FoodProperties food = stack.get(DataComponents.FOOD);
        stack.shrink(1);
        if (stack.isEmpty()) {
            item.discard();
        } else {
            item.setItem(stack);
        }
        if (food != null) {
            mob.heal(DreadFishRules.heal(food.nutrition()));
        }
        mob.playSound(SoundEvents.GENERIC_EAT, 0.6F, mob.getVoicePitch());
        if (mob.level() instanceof ServerLevel level) {
            if (mob.isInWater()) {
                level.sendParticles(ParticleTypes.BUBBLE, item.getX(), item.getY() + 0.1, item.getZ(), 4, 0.1, 0.1, 0.1, 0.02);
            } else {
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, bite), item.getX(), item.getY() + 0.15, item.getZ(), 6, 0.1, 0.05, 0.1, 0.05);
            }
        }
    }

    /**
     * With nothing to fight, now and then (a 1-in-5 roll: a goal's {@code canUse} runs only on alternate ticks, so a
     * tick-count step can miss every check) it makes for the nearest thing it eats within its search, not on pickup delay,
     * and eats it a mouthful at a time once in reach (within half its width and a block across, a block and a half under to
     * its height and a block and a half over).
     */
    public static final class EatDroppedGoal extends Goal {
        private final Mob mob;
        private final double search;
        /** Only while hurt (true), only while whole (false), or either (null). */
        private final @Nullable Boolean hurt;
        private @Nullable ItemEntity food;
        private int timer;

        public EatDroppedGoal(Mob mob, double search) {
            this(mob, search, null);
        }

        public EatDroppedGoal(Mob mob, double search, @Nullable Boolean hurt) {
            this.mob = mob;
            this.search = search;
            this.hurt = hurt;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean wanted(ItemEntity item) {
            return item.isAlive() && !item.hasPickUpDelay() && eats(item.getItem());
        }

        @Override
        public boolean canUse() {
            if (this.mob.getTarget() != null || this.mob.getRandom().nextInt(5) != 0
                    || this.hurt != null && this.hurt != this.mob.getHealth() < this.mob.getMaxHealth()) {
                return false;
            }
            this.food = this.mob.level().getEntitiesOfClass(ItemEntity.class, this.mob.getBoundingBox().inflate(this.search), this::wanted)
                    .stream().min(Comparator.comparingDouble(this.mob::distanceToSqr)).orElse(null);
            return this.food != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.food != null && this.wanted(this.food) && this.mob.getTarget() == null && this.timer < 300
                    && this.mob.distanceToSqr(this.food) < (this.search + 4) * (this.search + 4);
        }

        @Override
        public void start() {
            this.timer = 0;
        }

        @Override
        public void stop() {
            this.food = null;
            this.mob.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private boolean inReach(ItemEntity item) {
            double across = this.mob.getBbWidth() / 2.0 + 1.0;
            double dx = item.getX() - this.mob.getX();
            double dz = item.getZ() - this.mob.getZ();
            double dy = item.getY() - this.mob.getY();
            return dx * dx + dz * dz <= across * across && dy >= -1.5 && dy <= this.mob.getBbHeight() + 1.5;
        }

        @Override
        public void tick() {
            if (this.food == null) {
                return;
            }
            this.timer++;
            this.mob.getLookControl().setLookAt(this.food);
            if (this.inReach(this.food)) {
                this.mob.getNavigation().stop();
                if (this.timer % DreadFishRules.EAT_TICKS == 0 && this.wanted(this.food)) {
                    eat(this.mob, this.food);
                }
            } else if (this.timer % 10 == 1) {
                this.mob.getNavigation().moveTo(this.food, 1.0);
            }
        }
    }
}
