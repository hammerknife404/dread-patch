package io.github.hammerknife404.dreadpatch.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;

/**
 * Rimefrost (0.51.0), an Iron's spell school, "a subtype of ice" (the owner): the Dread's cold magic,
 * all her spells. A Rimefrost spell draws on Ice spell power as well as Rimefrost's, and Ice magic
 * resistance guards against it as well as Rimefrost's (the two multiply), so everything that helps
 * with Ice helps with it too. Its focus at the Scroll Forge is a dread shard.
 */
public class RimefrostSchool extends SchoolType {
    public RimefrostSchool(ResourceLocation id, TagKey<Item> focus, Component displayName, Holder<Attribute> power, Holder<Attribute> resistance,
                           Holder<SoundEvent> castSound, ResourceKey<DamageType> damageType) {
        super(id, focus, displayName, power, resistance, castSound, damageType);
    }

    @Override
    public double getPowerFor(LivingEntity entity) {
        return super.getPowerFor(entity) * SchoolRegistry.ICE.get().getPowerFor(entity);
    }

    @Override
    public double getResistanceFor(LivingEntity entity) {
        return super.getResistanceFor(entity) * SchoolRegistry.ICE.get().getResistanceFor(entity);
    }

    /** Whether spells of {@code school} count as Ice: Ice itself, or Rimefrost. */
    public static boolean isIce(SchoolType school) {
        return school == SchoolRegistry.ICE.get() || school instanceof RimefrostSchool;
    }
}
