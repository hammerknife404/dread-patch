package io.github.hammerknife404.dreadpatch.spell;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.entity.RimefireBlastProjectile;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.spells.EntityCastData;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Rimefire Blast (0.51.0, the owner's second drop): Iron's Cone of Cold, in Rimefire. Held, a cone of
 * cold dread fire pours from the caster's hand, hitting low (as the Rimefrost breath does, for a
 * spell: well under Cone of Cold's 1 + 0.75 a power) and leaving Withering Rime on everything it
 * touches. Rimefrost school; the one spell of hers made at Iron's Scroll Forge, from a dread shard
 * focus and ink (the owner bent the old her-loot-only rule for it). Also among her scrolls.
 */
public class RimefireBlastSpell extends AbstractSpell {
    /** Its damage a hit: this, and this much a point of spell power. */
    public static final float BASE_DAMAGE = 0.5F;
    public static final float DAMAGE_PER_POWER = 0.4F;

    private final ResourceLocation id = DreadPatch.id("rimefire_blast");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(DreadSpells.RIMEFROST)
            .setMaxLevel(10)
            .setCooldownSeconds(12.0)
            .build();

    public RimefireBlastSpell() {
        this.manaCostPerLevel = 1;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 1;
        this.castTime = 100;
        this.baseManaCost = 5;
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
    public SchoolType getSchoolType() {
        return DreadSpells.school();
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.empty();
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.CONE_OF_COLD_LOOP.get());
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(this.getDamage(spellLevel, caster), 2)),
                Component.translatable("ui.dreadpatch.withering_rime_seconds", Utils.stringTruncation(RimefireBlastProjectile.RIME_TICKS / 20.0, 1)));
    }

    /** As Cone of Cold: a continuous cast's later ticks fire the cone it opened; its first opens one. */
    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData magicData) {
        if (magicData.isCasting() && magicData.getCastingSpellId().equals(this.getSpellId())
                && magicData.getAdditionalCastData() instanceof EntityCastData cast && cast.getCastingEntity() instanceof AbstractConeProjectile cone) {
            cone.setDealDamageActive();
            return;
        }
        RimefireBlastProjectile cone = new RimefireBlastProjectile(level, entity);
        cone.setPos(entity.position().add(0.0, entity.getEyeHeight() * 0.7, 0.0));
        cone.setDamage(this.getDamage(spellLevel, entity));
        level.addFreshEntity(cone);
        magicData.setAdditionalCastData(new EntityCastData(cone));
        super.onCast(level, spellLevel, entity, castSource, magicData);
    }

    public float getDamage(int spellLevel, LivingEntity caster) {
        return BASE_DAMAGE + this.getSpellPower(spellLevel, caster) * DAMAGE_PER_POWER;
    }

    @Override
    public boolean shouldAIStopCasting(int spellLevel, Mob mob, LivingEntity target) {
        return mob.distanceToSqr(target) > 120.0;
    }
}
