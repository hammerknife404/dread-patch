package io.github.hammerknife404.dreadpatch.item;

import com.iafenvoy.iceandfire.item.ability.PostHitAbility;
import com.iafenvoy.iceandfire.registry.IafTiers;
import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.List;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Dread dragonsteel's tools (0.42.0): Ice and Fire's own tool classes ({@code ActivePostHit*Item}) on a
 * tier that is its ice dragonsteel's ({@link #TIER}: uses, speed, damage, enchantability, what it
 * mines, all from Ice and Fire's config as its own reads them) but repaired with dread dragonsteel, and
 * where ice dragonsteel freezes what it hits, dread dragonsteel leaves Withering Rime ({@link #RIME}).
 */
public final class DreadDragonsteel {
    /** Withering Rime from a dread dragonsteel tool's hit: 3 s (the queen's own sword leaves 4.5). */
    public static final int RIME_TICKS = 60;

    /** Ice and Fire's ice dragonsteel tier, repaired with dread dragonsteel. */
    public static final Tier TIER = new Tier() {
        @Override
        public int getUses() {
            return IafTiers.DRAGONSTEEL_ICE.getUses();
        }

        @Override
        public float getSpeed() {
            return IafTiers.DRAGONSTEEL_ICE.getSpeed();
        }

        @Override
        public float getAttackDamageBonus() {
            return IafTiers.DRAGONSTEEL_ICE.getAttackDamageBonus();
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return IafTiers.DRAGONSTEEL_ICE.getIncorrectBlocksForDrops();
        }

        @Override
        public int getEnchantmentValue() {
            return IafTiers.DRAGONSTEEL_ICE.getEnchantmentValue();
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(DreadGear.DREAD_DRAGONSTEEL_INGOT.get());
        }
    };

    /** Withering Rime on what it hits, as Ice and Fire's ice dragonsteel freezes it. */
    public static final PostHitAbility RIME = new PostHitAbility() {
        @Override
        public void active(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            target.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, RIME_TICKS), attacker);
        }

        @Override
        public boolean isEnable() {
            return true;
        }

        @Override
        public void addDescription(List<Component> tooltip) {
            tooltip.add(Component.translatable("tooltip.dreadpatch.dread_dragonsteel.rime", RIME_TICKS / 20).withStyle(ChatFormatting.AQUA));
        }
    };

    private DreadDragonsteel() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadDragonsteel::onMobHit);
    }

    /**
     * A mob's hit with one: vanilla's {@code Mob.doHurtTarget} never calls the item's {@code hurtEnemy},
     * where Ice and Fire's tools (and so ours) run their ability, so only a player's hits would rime.
     */
    private static void onMobHit(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0 && event.getSource().is(DamageTypes.MOB_ATTACK)
                && event.getSource().getDirectEntity() instanceof LivingEntity attacker && !(attacker instanceof Player)
                && DreadGear.isDreadDragonsteelTool(attacker.getMainHandItem()) && event.getEntity().isAlive()) {
            RIME.active(attacker.getMainHandItem(), event.getEntity(), attacker);
        }
    }
}
