package io.github.hammerknife404.dreadpatch.spell;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.magic.RimeBoltRules;
import io.github.hammerknife404.dreadpatch.entity.WitheringRimeBolt;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Her necrotic bolt as an Ice spell (so Ice spell power, the Cursium Mage set and the Codex of Malice
 * all count): Ice damage from spell power, plus a share of the target's max health, plus Withering
 * Rime. Only she drops it: kept out of Iron's random loot and the scroll forge.
 */
public class WitheringRimeBoltSpell extends AbstractSpell {
    /** Withering Rime it leaves: 4.5 s. */
    public static final int RIME_TICKS = 90;

    private final ResourceLocation id = DreadPatch.id("withering_rime_bolt");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(DreadSpells.RIMEFROST)
            .setMaxLevel(5)
            .setCooldownSeconds(6)
            .setAllowCrafting(false)
            .build();

    public WitheringRimeBoltSpell() {
        this.baseSpellPower = 6;
        this.spellPowerPerLevel = 2;
        this.baseManaCost = 40;
        this.manaCostPerLevel = 5;
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
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(this.getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.dreadpatch.max_health_share", Utils.stringTruncation(RimeBoltRules.spellShare(spellLevel) * 100, 0)),
                Component.translatable("ui.dreadpatch.withering_rime_seconds", Utils.stringTruncation(RIME_TICKS / 20.0F, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        // The share of max health by level (0.51.0: 3% at level 1, a point a level; 8% flat before).
        WitheringRimeBolt bolt = new WitheringRimeBolt(level, entity, this.getDamage(spellLevel, entity), RimeBoltRules.spellShare(spellLevel), RIME_TICKS);
        bolt.setPos(entity.position().add(0, entity.getEyeHeight() - bolt.getBoundingBox().getYsize() * 0.5F, 0));
        bolt.shoot(entity.getLookAngle());
        level.addFreshEntity(bolt);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private float getDamage(int spellLevel, LivingEntity caster) {
        return this.getSpellPower(spellLevel, caster);
    }
}
