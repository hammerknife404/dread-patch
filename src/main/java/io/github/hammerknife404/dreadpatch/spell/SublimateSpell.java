package io.github.hammerknife404.dreadpatch.spell;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.effect.RimefrostEffect;
import io.github.hammerknife404.dreadpatch.entity.RimefireCrown;
import io.github.hammerknife404.dreadpatch.magic.SublimateRules;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Sublimate (0.74.0; the owner's Feature_Dump_11-24 "Whiteout" spell, named by them: "a brief near instant
 * cast piercing cone"): an instant cone of rime turning to vapour from the caster's eyes, {@link SublimateRules}.
 * It passes through everyone in it the caster may harm and can see (blocks stop it), for Rimefrost damage from
 * spell power and one level of Rimefrost (the dump: "1 stack of frost or freezing"; renewed, never raised).
 * Made at the Scroll Forge from Epic up (the dump: "Craftable at Epic and up"), and found on Dread Liches and in
 * spire summit chests; kept out of Iron's random loot, as all of hers.
 */
public class SublimateSpell extends AbstractSpell {
    private final ResourceLocation id = DreadPatch.id("sublimate");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(DreadSpells.RIMEFROST)
            .setMaxLevel(5)
            .setCooldownSeconds(8)
            .build();

    public SublimateSpell() {
        this.baseSpellPower = 4;
        this.spellPowerPerLevel = 1;
        this.baseManaCost = 30;
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
                Component.translatable("ui.dreadpatch.distance", Utils.stringTruncation(SublimateRules.range(spellLevel), 0)),
                Component.translatable("ui.dreadpatch.rimefrost_seconds", Utils.stringTruncation(SublimateRules.rimefrostTicks(spellLevel) / 20.0, 1)));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        if (level instanceof ServerLevel server) {
            blast(server, caster, spellLevel, this.getDamage(spellLevel, caster));
        }
        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    /** The cone: everyone in it the caster may harm and sees from its eyes. How many it struck. */
    public int blast(ServerLevel level, LivingEntity caster, int spellLevel, float damage) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        double range = SublimateRules.range(spellLevel);
        int struck = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(range + 1.0),
                e -> e != caster && e.isAlive() && !e.isSpectator() && RimefireCrown.canHarm(caster, e))) {
            Vec3 aim = target.getBoundingBox().getCenter();
            Vec3 to = aim.subtract(eye);
            if (!SublimateRules.inCone(to.x, to.y, to.z, look.x, look.y, look.z, range, target.getBbWidth() * 0.5)) {
                continue;
            }
            if (level.clip(new ClipContext(eye, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getType() != HitResult.Type.MISS) {
                continue;
            }
            if (DamageSources.applyDamage(target, damage, this.getDamageSource(caster))) {
                struck++;
            }
            target.addEffect(new MobEffectInstance(ModRegistries.RIMEFROST, SublimateRules.rimefrostTicks(spellLevel), SublimateRules.RIMEFROST_AMPLIFIER), caster);
        }
        // A burst of rime turning to vapour down the cone.
        for (int i = 0; i < 24; i++) {
            double t = (i + 1) / 24.0 * range;
            double spread = t * Math.tan(Math.toRadians(SublimateRules.HALF_ANGLE_DEGREES)) * 0.7;
            Vec3 at = eye.add(look.scale(t));
            level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 3, spread * 0.5, spread * 0.5, spread * 0.5, 0.02);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 1, spread * 0.4, spread * 0.4, spread * 0.4, 0.01);
            }
        }
        return struck;
    }

    public float getDamage(int spellLevel, LivingEntity caster) {
        return this.getSpellPower(spellLevel, caster);
    }

    @Override
    public boolean shouldAIStopCasting(int spellLevel, Mob mob, LivingEntity target) {
        return mob.distanceToSqr(target) > SublimateRules.range(spellLevel) * SublimateRules.range(spellLevel);
    }
}
