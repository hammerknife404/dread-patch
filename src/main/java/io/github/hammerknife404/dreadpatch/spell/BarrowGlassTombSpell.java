package io.github.hammerknife404.dreadpatch.spell;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.entity.BarrowGlassTomb;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Barrow-Glass Tomb as an Ice spell: raises the tomb where the caster looks (on whoever they look at,
 * or the ground), rooting everyone inside and putting out fires. Her Withering Rime-Bolt shatters it.
 * Only she drops it, like her bolt.
 */
public class BarrowGlassTombSpell extends AbstractSpell {
    /** How far the caster can raise it, in blocks. */
    public static final float RANGE = 24.0F;

    private final ResourceLocation id = DreadPatch.id("barrow_glass_tomb");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SchoolRegistry.ICE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(15)
            .setAllowCrafting(false)
            .build();

    public BarrowGlassTombSpell() {
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.baseManaCost = 50;
        this.manaCostPerLevel = 10;
        this.castTime = 0;
    }

    /** Ticks the tomb stands: 3.5 s at level 1 up to 5.5 s at level 5. */
    public static int lifetime(int spellLevel) {
        return 60 + 10 * spellLevel;
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
                Component.translatable("ui.dreadpatch.tomb_seconds", Utils.stringTruncation(lifetime(spellLevel) / 20.0F, 1)),
                Component.translatable("ui.dreadpatch.radius", Utils.stringTruncation(BarrowGlassTomb.RADIUS, 0)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        level.addFreshEntity(BarrowGlassTomb.create(level, entity, targetPoint(level, entity), lifetime(spellLevel)));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** Whoever the caster looks at, else the block they look at, else as far as it reaches; then down to the ground. */
    static Vec3 targetPoint(Level level, LivingEntity caster) {
        HitResult hit = RaycastBuilder.begin(level, caster).range(RANGE).checkForBlocks(true).build();
        Vec3 point = hit instanceof EntityHitResult entityHit ? entityHit.getEntity().position() : hit.getLocation();
        return Utils.moveToRelativeGroundLevel(level, point, 6);
    }
}
