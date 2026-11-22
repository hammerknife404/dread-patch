package io.github.hammerknife404.dreadpatch.loot;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.hammerknife404.dreadpatch.registry.DreadGear;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/**
 * {@code dreadpatch:rimefrost_scales}: a Rimefrost corpse's scales, as many for its age as Ice and Fire's
 * {@code dragon_loot} gives a dragon's coloured ones (age / 25 + a roll up to age / 5, days). Ice and
 * Fire's function only counts its own {@code DragonScalesItem}s (and swaps them to the dragon's colour).
 */
public class RimefrostScalesFunction extends LootItemConditionalFunction {
    public static final MapCodec<RimefrostScalesFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance).apply(instance, RimefrostScalesFunction::new));

    protected RimefrostScalesFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    public LootItemFunctionType<RimefrostScalesFunction> getType() {
        return DreadGear.RIMEFROST_SCALES_FUNCTION.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (entity instanceof DragonBaseEntity dragon) {
            int age = dragon.getAgeInDays();
            stack.setCount(Math.max(1, age / 25 + dragon.getRandom().nextInt(1 + age / 5)));
        }
        return stack;
    }
}
