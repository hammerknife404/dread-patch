package io.github.hammerknife404.dreadpatch.spell;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.entity.LichSkull;
import io.github.hammerknife404.dreadpatch.entity.SummonedCourt;
import io.github.hammerknife404.dreadpatch.magic.SkullRules;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Lich Skull (0.98.5, the owner: "Turn the innate spell ability of the dread lich staff into its own spell. At higher
 * levels it fires up to 5 total projectiles that first try to seek different targets"): Ice and Fire's lich staff loosed
 * a lich skull on right-click; now it's this, locked into the staff at level 1 ({@code DreadLichStaff}), and a scroll of
 * its own. One skull a level ({@link SkullRules}), fanned out ahead, each seeking its own foe ({@link LichSkull}): what's
 * in sight within {@link SkullRules#RANGE} and against the caster (a monster, what's after them, what they've hurt or
 * what's hurt them, a mob caster's target), nearest the aim first. Rimefrost; made at the Scroll Forge.
 */
public class LichSkullSpell extends AbstractSpell {
    private final ResourceLocation id = DreadPatch.id("lich_skull");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(DreadSpells.RIMEFROST)
            .setMaxLevel(SkullRules.MAX_SKULLS)
            .setCooldownSeconds(3)
            .setAllowCrafting(true)
            .build();

    public LichSkullSpell() {
        this.baseSpellPower = (int) SkullRules.DAMAGE;
        this.spellPowerPerLevel = 0;
        this.baseManaCost = 15;
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
                Component.translatable("ui.dreadpatch.skulls", SkullRules.skulls(spellLevel)),
                Component.translatable("ui.irons_spellbooks.damage", String.format("%.1f", this.getSpellPower(spellLevel, caster))));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            int count = SkullRules.skulls(spellLevel);
            List<LivingEntity> targets = targets(caster);
            Vec3 eye = caster.getEyePosition();
            float damage = this.getSpellPower(spellLevel, caster);
            for (int i = 0; i < count; i++) {
                int pick = SkullRules.targetFor(i, targets.size());
                LichSkull skull = new LichSkull(level, caster, pick < 0 ? null : targets.get(pick), damage);
                Vec3 aim = Vec3.directionFromRotation(caster.getXRot(), caster.getYRot() + SkullRules.fan(i, count));
                skull.setPos(eye.x + aim.x * 0.8, eye.y + aim.y * 0.8 - 0.2, eye.z + aim.z * 0.8);
                skull.setDeltaMovement(aim.scale(SkullRules.SPEED));
                level.addFreshEntity(skull);
            }
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.ZOMBIE_INFECT, SoundSource.PLAYERS, 0.75F,
                    0.5F + caster.getRandom().nextFloat() * 0.2F);
        }
        super.onCast(level, spellLevel, caster, castSource, playerMagicData);
    }

    /** Its foes in sight and range, nearest the caster's aim first. */
    static List<LivingEntity> targets(LivingEntity caster) {
        Vec3 look = caster.getLookAngle();
        Vec3 eye = caster.getEyePosition();
        return caster.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(SkullRules.RANGE),
                        entity -> entity != caster && entity.isAlive() && !entity.isSpectator() && foe(caster, entity)
                                && entity.distanceToSqr(caster) <= SkullRules.RANGE * SkullRules.RANGE && caster.hasLineOfSight(entity))
                .stream()
                .sorted(Comparator.comparingDouble(entity -> -look.dot(entity.getEyePosition().subtract(eye).normalize())))
                .toList();
    }

    static boolean foe(LivingEntity caster, LivingEntity entity) {
        if (SummonedCourt.serves(entity, caster) || entity.isAlliedTo(caster) || DamageSources.isFriendlyFireBetween(caster, entity)
                || entity instanceof OwnableEntity owned && caster.equals(owned.getOwner())) {
            return false;
        }
        return entity instanceof Enemy || entity instanceof Mob mob && mob.getTarget() == caster
                || entity == caster.getLastHurtMob() || entity == caster.getLastHurtByMob()
                || caster instanceof Mob mob && mob.getTarget() == entity;
    }
}
