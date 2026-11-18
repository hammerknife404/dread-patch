package io.github.hammerknife404.dreadpatch.spell;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.effect.WhiteoutEffect;
import io.github.hammerknife404.dreadpatch.entity.DreadQueen;
import io.github.hammerknife404.dreadpatch.entity.WhiteoutDominion;
import io.github.hammerknife404.dreadpatch.magic.WhiteoutStorm;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Whiteout Dominion as an Ice spell: a blizzard over the ground round the caster, standing where it
 * was raised. Everyone in it the caster may harm is slowed, can't see past a few blocks, and takes a
 * point of freezing damage every few seconds. Only the Dread Queen's raises the dead. Only she drops
 * it, like her other spells; at level 5 a caster's is as big and long as hers.
 */
public class WhiteoutDominionSpell extends AbstractSpell {
    private final ResourceLocation id = DreadPatch.id("whiteout_dominion");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(WhiteoutStorm.QUEEN_COOLDOWN / 20.0)
            .setAllowCrafting(false)
            .build();

    public WhiteoutDominionSpell() {
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.baseManaCost = 80;
        this.manaCostPerLevel = 20;
        this.castTime = 0;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return this.id;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return this.defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.dreadpatch.radius", Utils.stringTruncation(WhiteoutStorm.radius(spellLevel), 0)),
                Component.translatable("ui.dreadpatch.storm_seconds", Utils.stringTruncation(WhiteoutStorm.lifetime(spellLevel) / 20.0F, 0)),
                Component.translatable("ui.dreadpatch.slow", Math.round(WhiteoutEffect.SLOW * 100)),
                Component.translatable("ui.dreadpatch.freezing_damage", Utils.stringTruncation(WhiteoutStorm.FREEZE_DAMAGE, 0),
                        Utils.stringTruncation(WhiteoutStorm.FREEZE_INTERVAL / 20.0F, 0)));
    }

    /** A new storm of the caster's ends the last; it's raised on the ground under them. */
    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        for (WhiteoutDominion old : level.getEntitiesOfClass(WhiteoutDominion.class, entity.getBoundingBox().inflate(128.0), s -> s.ownedBy(entity))) {
            old.discard();
        }
        level.addFreshEntity(WhiteoutDominion.create(level, entity, Utils.moveToRelativeGroundLevel(level, entity.position(), 64),
                WhiteoutStorm.radius(spellLevel), WhiteoutStorm.lifetime(spellLevel), entity instanceof DreadQueen));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
