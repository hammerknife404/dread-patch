package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadMobEntity;
import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import com.iafenvoy.iceandfire.registry.IafSounds;
import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import io.github.hammerknife404.dreadpatch.magic.CourtRules;
import io.github.hammerknife404.dreadpatch.magic.PhantomRules;
import io.github.hammerknife404.dreadpatch.magic.SummonRules;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericCopyOwnerTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericFollowOwnerGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericOwnerHurtByTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericOwnerHurtTargetGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.GenericProtectOwnerTargetGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
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
import java.util.UUID;

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
 * The dread phantoms a player's spell calls (0.45.0) are a summoned court too ({@link #raisePhantoms}),
 * under the same rules: they answer to their {@code Summoner} instead, escort their caster in the air
 * rather than following on foot, and a new cast of either spell dismisses only its own.
 */
public final class SummonedCourt {
    /** Game time when a summoned court member goes. Its presence is what makes a dread mob one. */
    public static final String EXPIRES_KEY = "dreadpatch:court_expires";
    /**
     * Set on a mob's summons (0.93.0, Syndrigossa's): bound by no player's terms, they fight all their caster does, players
     * too, and don't follow it about (she flies).
     */
    public static final String UNBOUND_KEY = "dreadpatch:court_unbound";

    private static final ParticleOptions CHARCOAL = new DustParticleOptions(new Vector3f(0.078F, 0.086F, 0.114F), 1.6F);

    private SummonedCourt() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SummonedCourt::onJoin);
        gameBus.addListener(SummonedCourt::onChangeTarget);
        gameBus.addListener(SummonedCourt::onIncomingDamage);
        gameBus.addListener(SummonedCourt::onTick);
    }

    /**
     * Whether {@code entity} is a court member a caster raised: a dread mob of Summon Court's (or Syndrigossa's), a dread
     * phantom of the spell's, or a Dread Fish of Summon Dread Fish (or hers; 0.93.0).
     */
    public static boolean isMember(@Nullable Entity entity) {
        return (entity instanceof DreadMobEntity || entity instanceof DreadPhantom || entity instanceof DreadFish)
                && entity.getPersistentData().contains(EXPIRES_KEY);
    }

    /** Who a court member answers to: a dread mob's {@code CommanderUUID}, a phantom's {@code Summoner}, a fish's raiser. */
    @Nullable
    static UUID commanderId(Entity member) {
        if (member instanceof DreadMobEntity mob) {
            return mob.getCommanderId();
        }
        if (member instanceof DreadFish fish) {
            return RisenDread.raiserId(fish);
        }
        return member instanceof DreadPhantom phantom ? phantom.getSummonerId() : null;
    }

    /** Whether it's a mob's summons, bound by no player's terms ({@link #UNBOUND_KEY}). */
    static boolean unbound(Entity member) {
        return member.getPersistentData().getBoolean(UNBOUND_KEY);
    }

    /**
     * Its caster, resolved from its {@code CommanderUUID} (a player, or any entity in its level), not by
     * Ice and Fire's {@code getCommander}: its scuttler's always returns null (as its lich's does), so a
     * scuttler of a caster's court followed no one, defended no one, and its caster could hurt it (until 0.42.0).
     */
    @Nullable
    public static Entity commander(Entity member) {
        UUID id = commanderId(member);
        if (id == null || !(member.level() instanceof ServerLevel level)) {
            return null;
        }
        Player player = level.getPlayerByUUID(id);
        return player != null ? player : level.getEntity(id);
    }

    /** Whether {@code entity} is a court member {@code caster} raised. */
    public static boolean serves(@Nullable Entity entity, @Nullable Entity caster) {
        return caster != null && isMember(entity) && caster.getUUID().equals(commanderId(entity));
    }

    /**
     * Raises {@code caster}'s court: {@link CourtRules#summonCount} of her court's kinds round them, for
     * {@link CourtRules#summonSeconds}, after dismissing the court they raised before. How many rose.
     */
    public static int raise(ServerLevel level, LivingEntity caster, int spellLevel) {
        dismiss(level, caster, DreadMobEntity.class);
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

    /**
     * The spell's dread phantoms (0.45.0): {@link PhantomRules#SPELL_COUNT} rise over {@code caster} for
     * {@link PhantomRules#SPELL_SECONDS}, after dismissing the phantoms they called before. How many rose.
     */
    public static int raisePhantoms(ServerLevel level, LivingEntity caster) {
        dismiss(level, caster, DreadPhantom.class);
        long expires = level.getGameTime() + PhantomRules.SPELL_SECONDS * 20L;
        int raised = 0;
        for (int i = 0; i < PhantomRules.SPELL_COUNT; i++) {
            if (PhantomCalling.rise(level, caster, expires) != null) {
                raised++;
            }
        }
        if (raised > 0) {
            caster.swing(InteractionHand.MAIN_HAND);
        }
        return raised;
    }

    /**
     * Summon Dread Fish (0.93.0): {@link SummonRules#fishCount} fish rise round {@code caster} for
     * {@link SummonRules#FISH_SPELL_SECONDS}, after dismissing the fish they summoned before. How many rose.
     */
    public static int raiseFishSpell(ServerLevel level, LivingEntity caster, int spellLevel) {
        dismiss(level, caster, DreadFish.class);
        long expires = level.getGameTime() + SummonRules.FISH_SPELL_SECONDS * 20L;
        int raised = raiseFish(level, caster, caster.position(), SummonRules.fishCount(spellLevel), expires, false, null);
        if (raised > 0) {
            caster.swing(InteractionHand.MAIN_HAND);
        }
        return raised;
    }

    /**
     * {@code count} Dread Fish rise round {@code center}, {@code caster}'s court until {@code expires}: in water near it if
     * there's any ({@link #findWater}), else on the ground. Each is sized and given its lantern as a wild one spawning is
     * (the owner: "Lantern variants and spawn rate are kept the same"). {@code unbound} for a mob's ({@link #UNBOUND_KEY});
     * set on {@code target} if given. How many rose.
     */
    public static int raiseFish(ServerLevel level, LivingEntity caster, Vec3 center, int count, long expires, boolean unbound, @Nullable LivingEntity target) {
        int raised = 0;
        for (int i = 0; i < count; i++) {
            DreadFish fish = ModRegistries.DREAD_FISH.get().create(level);
            if (fish == null) {
                continue;
            }
            Vec3 spot = findWater(level, center, caster.getRandom(), fish);
            if (spot == null) {
                spot = DreadCourt.findSpot(level, center, caster.getRandom(), fish);
            }
            if (spot == null) {
                continue;
            }
            fish.moveTo(spot.x, spot.y, spot.z, caster.getRandom().nextFloat() * 360.0F, 0.0F);
            EventHooks.finalizeMobSpawn(fish, level, level.getCurrentDifficultyAt(fish.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            fish.getPersistentData().putUUID(RisenDread.RAISED_BY, caster.getUUID());
            fish.getPersistentData().putLong(EXPIRES_KEY, expires);
            if (unbound) {
                fish.getPersistentData().putBoolean(UNBOUND_KEY, true);
            }
            fish.setPersistenceRequired();
            if (target != null) {
                fish.setTarget(target);
            }
            if (level.addFreshEntity(fish)) {
                MagicManager.spawnParticles(level, ParticleTypes.BUBBLE_COLUMN_UP, spot.x, spot.y + 0.3, spot.z, 20, 0.4, 0.3, 0.4, 0.05, false);
                MagicManager.spawnParticles(level, ParticleTypes.SOUL, spot.x, spot.y + 0.3, spot.z, 6, 0.3, 0.3, 0.3, 0.02, false);
                MagicManager.spawnParticles(level, ParticleTypes.SNOWFLAKE, spot.x, spot.y + 0.3, spot.z, 10, 0.5, 0.3, 0.5, 0.02, false);
                level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ELDER_GUARDIAN_FLOP, SoundSource.HOSTILE, 1.0F, 0.6F);
                raised++;
            }
        }
        return raised;
    }

    /** Water within {@link SummonRules#WATER_REACH} across and {@link SummonRules#WATER_DEPTH} up or down of {@code center} that {@code fish} fits in, or null. */
    @Nullable
    static Vec3 findWater(ServerLevel level, Vec3 center, RandomSource random, Mob fish) {
        for (int attempt = 0; attempt < 24; attempt++) {
            BlockPos at = BlockPos.containing(center.x + random.nextInt(2 * SummonRules.WATER_REACH + 1) - SummonRules.WATER_REACH,
                    center.y + random.nextInt(2 * SummonRules.WATER_DEPTH + 1) - SummonRules.WATER_DEPTH,
                    center.z + random.nextInt(2 * SummonRules.WATER_REACH + 1) - SummonRules.WATER_REACH);
            if (!level.getFluidState(at).is(FluidTags.WATER)) {
                continue;
            }
            Vec3 spot = Vec3.atBottomCenterOf(at);
            if (level.noCollision(fish, fish.getType().getSpawnAABB(spot.x, spot.y, spot.z))) {
                return spot;
            }
        }
        return null;
    }

    /** Sends back every court member of {@code kind} {@code caster} raised in this level. */
    static void dismiss(ServerLevel level, Entity caster, Class<? extends Mob> kind) {
        List<Mob> court = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (kind.isInstance(entity) && serves(entity, caster)) {
                court.add((Mob) entity);
            }
        }
        court.forEach(SummonedCourt::crumble);
    }

    /** Back into the ground (or the air), with whatever dread mount it rode. */
    private static void crumble(Mob member) {
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
        if (event.getEntity() instanceof DreadPhantom phantom) {
            // A phantom escorts its caster in the air (its own flight), so no following goal, and turns on what hurts it its own way.
            phantom.targetSelector.removeAllGoals(goal -> true);
            phantom.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(phantom, () -> commander(phantom)));
            phantom.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(phantom, () -> commander(phantom)));
            phantom.targetSelector.addGoal(4, new DreadPhantom.RevengeGoal(phantom));
            phantom.targetSelector.addGoal(5, new GenericProtectOwnerTargetGoal(phantom, () -> commander(phantom)));
            return;
        }
        // A dread mob of a court, or a Dread Fish (0.93.0): both pathfinders.
        PathfinderMob member = (PathfinderMob) event.getEntity();
        member.targetSelector.removeAllGoals(goal -> true);
        member.targetSelector.addGoal(1, new GenericOwnerHurtByTargetGoal(member, () -> commander(member)));
        member.targetSelector.addGoal(2, new GenericOwnerHurtTargetGoal(member, () -> commander(member)));
        member.targetSelector.addGoal(3, new GenericCopyOwnerTargetGoal(member, () -> commander(member)));
        member.targetSelector.addGoal(4, new HurtByTargetGoal(member));
        member.targetSelector.addGoal(5, new GenericProtectOwnerTargetGoal(member, () -> commander(member)));
        if (unbound(member)) {
            // A mob's summons stay at the fight they rose to (Syndrigossa flies).
            return;
        }
        // Between Ice and Fire's melee (2) and its stroll (5), as Iron's summons follow (15 out, to 5; past 25, a teleport).
        // A fish too (0.93.0), after its leap and bite (1-2), before its breaching (4); never teleported, so it keeps to what it can reach.
        boolean fish = member instanceof DreadFish;
        member.goalSelector.addGoal(fish ? 3 : 4, new GenericFollowOwnerGoal(member, () -> commander(member), 1.0, 15.0F, 5.0F, false, fish ? Float.MAX_VALUE : 25.0F));
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
            if (!mayFight((Mob) event.getEntity(), target)) {
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
        if (isMember(victim) && onCastersSide(commander(victim), attacker)) {
            event.setCanceled(true);
        } else if (isMember(attacker) && !mayFight((Mob) attacker, victim)) {
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
            crumble((Mob) entity);
        }
    }

    /** Whether court member {@code member} may fight {@code target}, by the server config's terms. */
    public static boolean mayFight(Mob member, Entity target) {
        boolean retaliating = target == member.getLastHurtByMob();
        CourtRules.Stance stance = stance(commander(member), target);
        if (unbound(member)) {
            return stance != CourtRules.Stance.CASTERS_SIDE && stance != CourtRules.Stance.DREAD;
        }
        return CourtRules.mayFight(stance, retaliating, DreadPatchConfig.courtTerms());
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
