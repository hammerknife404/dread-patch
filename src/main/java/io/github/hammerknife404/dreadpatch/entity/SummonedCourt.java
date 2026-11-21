package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadMobEntity;
import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import com.iafenvoy.iceandfire.registry.IafSounds;
import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import io.github.hammerknife404.dreadpatch.magic.CourtRules;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericCopyOwnerTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericFollowOwnerGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericOwnerHurtByTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericOwnerHurtTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericProtectOwnerTargetGoal;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * A court raised by Summon Court: her court's dread mobs (Ice and Fire's, through
 * {@link EventHooks#finalizeMobSpawn}, rising with their own animations and gear), commanded by the
 * caster ({@code CommanderUUID}, which Ice and Fire resolves for players too) and marked with when they
 * go ({@link #EXPIRES_KEY}, in their persistent data, so it survives a save).
 * <ul>
 * <li>Their own targeting goes (Ice and Fire's goes for every player and everything not of the Dread);
 * in its place Iron's summons' (its {@code SummonedZombie}'s): what hurts the caster, what the caster
 * hurts, the caster's target, what hurts them, what goes for the caster; and they follow the caster.</li>
 * <li>Every target they'd take, and every hit they'd land, passes {@link CourtRules#mayFight}: hostile
 * mobs, never the caster's side or the Dread, and friendly mobs and players only as the server config
 * allows (by default never, not even to fight back). With friendly mobs off limits, friendly mobs
 * leave them be too (an iron golem goes for any monster).</li>
 * <li>The caster's side can't hurt them: their own spells, sweeps and arrows.</li>
 * <li>When their time is up they crumble back into the ground; a new cast dismisses the last.</li>
 * </ul>
 */
public final class SummonedCourt {
    /** Game time when a summoned court member goes. Its presence is what makes a dread mob one. */
    public static final String EXPIRES_KEY = "dreadpatch:court_expires";

    private static final ParticleOptions CHARCOAL = new DustParticleOptions(new Vector3f(0.078F, 0.086F, 0.114F), 1.6F);

    private SummonedCourt() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SummonedCourt::onJoin);
        gameBus.addListener(SummonedCourt::onChangeTarget);
        gameBus.addListener(SummonedCourt::onIncomingDamage);
        gameBus.addListener(SummonedCourt::onTick);
    }

    /** Whether {@code entity} is a court member a caster raised. */
    public static boolean isMember(@Nullable Entity entity) {
        return entity instanceof DreadMobEntity mob && mob.getPersistentData().contains(EXPIRES_KEY);
    }

    /** Whether {@code entity} is a court member {@code caster} raised. */
    public static boolean serves(@Nullable Entity entity, @Nullable Entity caster) {
        return caster != null && isMember(entity) && caster.getUUID().equals(((DreadMobEntity) entity).getCommanderId());
    }

    /**
     * Raises {@code caster}'s court: {@link CourtRules#summonCount} of her court's kinds round them, for
     * {@link CourtRules#summonSeconds}, after dismissing the court they raised before. How many rose.
     */
    public static int raise(ServerLevel level, LivingEntity caster, int spellLevel) {
        dismiss(level, caster);
        long expires = level.getGameTime() + CourtRules.summonSeconds(spellLevel) * 20L;
        Vec3 center = caster.position();
        int raised = 0;
        for (int i = 0; i < CourtRules.summonCount(spellLevel); i++) {
            DreadMobEntity member = DreadCourt.create(level, CourtRules.pick(caster.getRandom().nextInt(CourtRules.TOTAL_WEIGHT)));
            if (member == null) {
                continue;
            }
            Vec3 spot = DreadCourt.findSpot(level, center, caster.getRandom(), member);
            if (spot == null) {
                continue;
            }
            member.moveTo(spot.x, spot.y, spot.z, caster.getYRot(), 0.0F);
            EventHooks.finalizeMobSpawn(member, level, level.getCurrentDifficultyAt(member.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            member.setCommanderId(caster.getUUID());
            member.getPersistentData().putLong(EXPIRES_KEY, expires);
            // They go when their time's up, not when the caster walks away.
            member.setPersistenceRequired();
            if (level.addFreshEntity(member)) {
                MagicManager.spawnParticles(level, CHARCOAL, spot.x, spot.y + 0.2, spot.z, 20, 0.4, 0.1, 0.4, 0.02, false);
                MagicManager.spawnParticles(level, ParticleTypes.SOUL, spot.x, spot.y + 0.4, spot.z, 6, 0.3, 0.3, 0.3, 0.02, false);
                raised++;
            }
        }
        if (raised > 0) {
            caster.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), IafSounds.DREAD_LICH_SUMMON.get(), SoundSource.PLAYERS, 1.5F, 0.8F);
        }
        return raised;
    }

    /** Sends back every court member {@code caster} raised in this level. */
    static void dismiss(ServerLevel level, Entity caster) {
        List<DreadMobEntity> court = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (serves(entity, caster)) {
                court.add((DreadMobEntity) entity);
            }
        }
        court.forEach(SummonedCourt::crumble);
    }

    /** Back into the ground, with whatever dread mount it rode. */
    private static void crumble(DreadMobEntity member) {
        if (member.level() instanceof ServerLevel level) {
            MagicManager.spawnParticles(level, CHARCOAL, member.getX(), member.getY(0.5), member.getZ(), 25, 0.4, 0.6, 0.4, 0.03, false);
            MagicManager.spawnParticles(level, ParticleTypes.SOUL, member.getX(), member.getY(0.5), member.getZ(), 6, 0.3, 0.5, 0.3, 0.02, false);
        }
        if (member.getVehicle() instanceof IDreadMob && member.getVehicle() != null) {
            member.getVehicle().discard();
        }
        member.discard();
    }

    /** Iron's summons' goals in place of Ice and Fire's targeting, every time one joins a level (goals aren't saved). */
    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !isMember(event.getEntity())) {
            return;
        }
        DreadMobEntity member = (DreadMobEntity) event.getEntity();
        member.targetSelector.removeAllGoals(goal -> true);
        member.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(member, member::getCommander));
        member.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(member, member::getCommander));
        member.targetSelector.addGoal(3, new GenericCopyOwnerTargetGoal(member, member::getCommander));
        member.targetSelector.addGoal(4, new HurtByTargetGoal(member));
        member.targetSelector.addGoal(5, new GenericProtectOwnerTargetGoal(member, member::getCommander));
        // Between Ice and Fire's melee (2) and its stroll (5), as Iron's summons follow (15 out, to 5; past 25, a teleport).
        member.goalSelector.addGoal(4, new GenericFollowOwnerGoal(member, member::getCommander, 1.0, 15.0F, 5.0F, false, 25.0F));
    }

    /**
     * Its targets pass {@link #mayFight}. With friendly mobs off limits to it, a friendly mob doesn't
     * take it for a target either.
     */
    private static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null || event.getEntity().level().isClientSide()) {
            return;
        }
        if (isMember(event.getEntity())) {
            if (!mayFight((DreadMobEntity) event.getEntity(), target)) {
                event.setCanceled(true);
            }
        } else if (isMember(target) && friendly(event.getEntity()) && !DreadPatchConfig.courtTerms().attackFriendlyMobs()) {
            event.setCanceled(true);
        }
    }

    /** Its caster's side can't hurt it, and it hurts nothing it mayn't fight. {@code /kill} and the void still reach it. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (attacker == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (isMember(victim) && onCastersSide(((DreadMobEntity) victim).getCommander(), attacker)) {
            event.setCanceled(true);
        } else if (isMember(attacker) && !mayFight((DreadMobEntity) attacker, victim)) {
            event.setCanceled(true);
        }
    }

    /** Its time up, it goes. Checked once a second. */
    private static void onTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.tickCount % 20 != 0 || entity.level().isClientSide() || !isMember(entity)) {
            return;
        }
        if (entity.level().getGameTime() >= entity.getPersistentData().getLong(EXPIRES_KEY)) {
            crumble((DreadMobEntity) entity);
        }
    }

    /** Whether court member {@code member} may fight {@code target}, by the server config's terms. */
    public static boolean mayFight(DreadMobEntity member, Entity target) {
        boolean retaliating = target == member.getLastHurtByMob();
        return CourtRules.mayFight(stance(member.getCommander(), target), retaliating, DreadPatchConfig.courtTerms());
    }

    private static CourtRules.Stance stance(@Nullable Entity caster, Entity target) {
        if (onCastersSide(caster, target)) {
            return CourtRules.Stance.CASTERS_SIDE;
        }
        if (target instanceof IDreadMob) {
            return CourtRules.Stance.DREAD;
        }
        if (target instanceof Player) {
            return CourtRules.Stance.PLAYER;
        }
        return target instanceof Enemy ? CourtRules.Stance.HOSTILE : CourtRules.Stance.FRIENDLY;
    }

    /** A mob that isn't hostile, a player or one of the Dread. */
    private static boolean friendly(Entity entity) {
        return !(entity instanceof Enemy) && !(entity instanceof Player) && !(entity instanceof IDreadMob);
    }

    /**
     * {@code entity} is {@code caster}, on their team, their pet, their summon (Iron's), their court,
     * or their Barrow-Step husk.
     */
    static boolean onCastersSide(@Nullable Entity caster, Entity entity) {
        if (caster == null) {
            return false;
        }
        if (entity == caster || caster.isAlliedTo(entity) || entity.isAlliedTo(caster) || serves(entity, caster)) {
            return true;
        }
        if (entity instanceof OwnableEntity pet && caster.getUUID().equals(pet.getOwnerUUID())) {
            return true;
        }
        if (entity instanceof IMagicSummon summon && summon.getSummoner() == caster || SummonManager.getOwner(entity) == caster) {
            return true;
        }
        return entity instanceof BarrowHusk husk && husk.getOwner() == caster;
    }
}
