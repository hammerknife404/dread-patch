package io.github.hammerknife404.dreadpatch.spell;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.entity.SummonedCourt;
import io.github.hammerknife404.dreadpatch.magic.CourtRules;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Summon Court, her Court of the Dread for whoever casts it: Ice and Fire's thralls, ghouls, knights
 * and scuttlers rise round the caster and fight for them for a while ({@link SummonedCourt}). Ice, like
 * all her magic; on her staff, and only she drops its scroll.
 */
public class SummonCourtSpell extends AbstractSpell {
    private final ResourceLocation id = DreadPatch.id("summon_court");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(120)
            .setAllowCrafting(false)
            .build();

    public SummonCourtSpell() {
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.baseManaCost = 60;
        this.manaCostPerLevel = 15;
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
                Component.translatable("ui.dreadpatch.court_members", CourtRules.summonCount(spellLevel)),
                Component.translatable("ui.dreadpatch.court_seconds", CourtRules.summonSeconds(spellLevel)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            SummonedCourt.raise(server, entity, spellLevel);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
