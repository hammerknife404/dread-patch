package io.github.hammerknife404.dreadpatch.spell;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.entity.SummonedCourt;
import io.github.hammerknife404.dreadpatch.magic.SummonRules;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Summon Dread Fish (0.93.0, the owner: "create a summon dread fish/alpha scroll & spell. Craftable, summons 1 - 5 pending
 * rank. Lantern variants and spawn rate are kapt the same."): one Dread Fish a level rises round the caster, in water
 * near them if there's any, and fights for them for a minute by a summoned court's rules
 * ({@link SummonedCourt#raiseFishSpell}); each sized (an alpha, a Rime Wyrm, now and then) and lit as a wild one is. Made
 * at Iron's Scroll Forge (dread shard focus, from common ink); in her loot and the spire summit chest's.
 */
public class SummonDreadFishSpell extends AbstractSpell {
    private final ResourceLocation id = DreadPatch.id("summon_dread_fish");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(DreadSpells.RIMEFROST)
            .setMaxLevel(SummonRules.FISH_SPELL_MAX_LEVEL)
            .setCooldownSeconds(60)
            .setAllowCrafting(true)
            .build();

    public SummonDreadFishSpell() {
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.baseManaCost = 30;
        this.manaCostPerLevel = 10;
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

    /** Rimefrost, whatever an older world's Iron's config says ({@link DreadSpells}). */
    @Override
    public SchoolType getSchoolType() {
        return DreadSpells.school();
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
                Component.translatable("ui.dreadpatch.dread_fish", SummonRules.fishCount(spellLevel)),
                Component.translatable("ui.dreadpatch.court_seconds", SummonRules.FISH_SPELL_SECONDS));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            SummonedCourt.raiseFishSpell(server, entity, spellLevel);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
