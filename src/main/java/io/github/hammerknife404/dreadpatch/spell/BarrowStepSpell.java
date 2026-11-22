package io.github.hammerknife404.dreadpatch.spell;

import com.iafenvoy.iceandfire.registry.IafParticles;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.entity.BarrowHusk;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Barrow-Step as an Ice spell, after Iron's Frost Step: the caster steps through the barrow-cold to
 * where they look and leaves a {@link BarrowHusk} where they stood, which draws their hunters and
 * roots whoever strikes it. The Dread Queen steps away from whoever corners her instead. Only she
 * drops it, like her other spells.
 */
public class BarrowStepSpell extends AbstractSpell {
    private final ResourceLocation id = DreadPatch.id("barrow_step");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(DreadSpells.RIMEFROST)
            .setMaxLevel(5)
            .setCooldownSeconds(12)
            .setAllowCrafting(false)
            .build();

    public BarrowStepSpell() {
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.baseManaCost = 30;
        this.manaCostPerLevel = 5;
        this.castTime = 0;
    }

    /** How far it can take the caster: 11 blocks at level 1 up to 15 at level 5. */
    public static float distance(int spellLevel) {
        return 10.0F + spellLevel;
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
                Component.translatable("ui.dreadpatch.distance", Utils.stringTruncation(distance(spellLevel), 0)),
                Component.translatable("ui.dreadpatch.root_seconds", Utils.stringTruncation(BarrowHusk.ROOT_TICKS / 20.0F, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        step(entity, TeleportSpell.findTeleportLocation(level, entity, distance(spellLevel)));
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /**
     * Takes {@code caster} to {@code to} through Iron's spell teleport (its teleport event, so other
     * mods can stop it), and if they went, leaves their husk where they stood.
     */
    public static boolean step(LivingEntity caster, Vec3 to) {
        Vec3 from = caster.position();
        float facing = caster.getYRot();
        if (caster.isPassenger()) {
            caster.stopRiding();
        }
        // Iron's returns whether its teleport event was cancelled, not whether the caster went.
        boolean cancelled = Utils.handleSpellTeleport(ModRegistries.BARROW_STEP_SPELL.get(), caster, to);
        if (cancelled) {
            return false;
        }
        caster.resetFallDistance();
        BarrowHusk.leaveBehind(caster, from, facing);
        if (caster.level() instanceof ServerLevel level) {
            level.sendParticles(IafParticles.DREAD_TORCH.get(), to.x, to.y + 1.0, to.z, 15, 0.3, 0.6, 0.3, 0.05);
            level.sendParticles(ParticleTypes.SNOWFLAKE, to.x, to.y + 1.0, to.z, 15, 0.4, 0.6, 0.4, 0.02);
            level.playSound(null, to.x, to.y, to.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, caster.getSoundSource(), 1.0F, 0.7F);
        }
        return true;
    }
}
