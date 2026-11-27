package io.github.hammerknife404.dreadpatch.world;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.entity.DreadLichEntity;
import com.iafenvoy.iceandfire.entity.DreadMobEntity;
import io.github.hammerknife404.dreadpatch.entity.DreadFish;
import io.github.hammerknife404.dreadpatch.entity.RisenDread;
import io.github.hammerknife404.dreadpatch.entity.SummonedCourt;
import io.github.hammerknife404.dreadpatch.magic.CourtRules;
import io.github.hammerknife404.dreadpatch.magic.SwarmRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The wild Dread swarm a fight (0.94.0, {@link SwarmRules}): once a second, round each fight, the Dread that are no
 * one's (not her court, a lich's, anyone's summons or her storm's risen; Ice and Fire's dread mobs and Dread Fish) that
 * are fighting call in those idle near them and share out what's near, by her court's caps. Liches (casters, keeping
 * their distance) and dread horses (mounts) are left to their own.
 */
public final class DreadSwarm {
    private DreadSwarm() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadSwarm::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % SwarmRules.EVERY != 7) {
            return;
        }
        // Every fight the wild Dread are in (only ticking entities fight), and the wild Dread near enough one to join it.
        List<Mob> fighting = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Mob mob && wild(mob) && fighting(mob)) {
                fighting.add(mob);
            }
        }
        if (fighting.isEmpty()) {
            return;
        }
        Set<Mob> swarm = new LinkedHashSet<>(fighting);
        for (Mob mob : fighting) {
            swarm.addAll(level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(SwarmRules.RALLY_RANGE), DreadSwarm::wild));
        }
        if (swarm.size() >= 2) {
            swarm(level, new ArrayList<>(swarm));
        }
    }

    /** Of the Dread, alive, with AI, no one's, and of a kind that swarms. */
    static boolean wild(Mob mob) {
        if (!mob.isAlive() || mob.isNoAi() || SummonedCourt.isMember(mob) || RisenDread.raiserId(mob) != null) {
            return false;
        }
        if (mob instanceof DreadMobEntity dread) {
            return dread.getCommanderId() == null && !(dread instanceof DreadLichEntity);
        }
        return mob instanceof DreadFish;
    }

    /** What the wild Dread hunt, as Ice and Fire's do: players (not in creative) and any mob not of the Dread; not dragons, bosses or armour stands. */
    static boolean prey(LivingEntity entity) {
        if (!entity.isAlive() || !entity.attackable() || entity instanceof ArmorStand || RisenDread.dread(entity)) {
            return false;
        }
        if (entity instanceof Player player) {
            return EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player);
        }
        return entity instanceof Mob && !(entity instanceof DragonBaseEntity) && !entity.getType().is(Tags.EntityTypes.BOSSES);
    }

    private static void swarm(ServerLevel level, List<Mob> all) {
        List<Mob> fighting = new ArrayList<>();
        for (Mob mob : all) {
            if (fighting(mob)) {
                fighting.add(mob);
            }
        }
        if (fighting.isEmpty()) {
            return;
        }
        // Those in the fight, and those idle near enough one of them to be called in.
        List<Mob> members = new ArrayList<>(fighting);
        List<Boolean> rallying = new ArrayList<>();
        fighting.forEach(m -> rallying.add(false));
        for (Mob mob : all) {
            if (!fighting.contains(mob) && fighting.stream().anyMatch(f -> f.distanceTo(mob) <= SwarmRules.RALLY_RANGE)) {
                members.add(mob);
                rallying.add(true);
            }
        }
        // The fight's targets, and whatever else they'd hunt near those fighting.
        Set<LivingEntity> found = new LinkedHashSet<>();
        for (Mob mob : fighting) {
            found.add(mob.getTarget());
        }
        for (Mob mob : fighting) {
            AABB near = mob.getBoundingBox().inflate(CourtRules.SPREAD_RANGE);
            found.addAll(level.getEntitiesOfClass(LivingEntity.class, near, DreadSwarm::prey));
        }
        List<LivingEntity> targets = new ArrayList<>(found);
        int[] caps = new int[targets.size()];
        for (int t = 0; t < targets.size(); t++) {
            caps[t] = CourtRules.pursuerCap(targets.get(t).getMaxHealth(), targets.get(t) instanceof Player);
        }
        double[][] seen = new double[members.size()][targets.size()];
        double[][] raw = new double[members.size()][targets.size()];
        int[] current = new int[members.size()];
        boolean[] calls = new boolean[members.size()];
        for (int m = 0; m < members.size(); m++) {
            Mob member = members.get(m);
            calls[m] = rallying.get(m);
            current[m] = fighting(member) ? targets.indexOf(member.getTarget()) : -1;
            for (int t = 0; t < targets.size(); t++) {
                LivingEntity target = targets.get(t);
                boolean able = member.canAttack(target);
                raw[m][t] = able ? member.distanceTo(target) : Double.POSITIVE_INFINITY;
                // Sight is only asked of what's near enough to be taken up (a raycast each, once a second).
                seen[m][t] = able && (t == current[m] || raw[m][t] <= CourtRules.SPREAD_RANGE && member.hasLineOfSight(target))
                        ? raw[m][t] : Double.POSITIVE_INFINITY;
            }
        }
        int[] chosen = SwarmRules.swarm(seen, raw, current, caps, calls);
        for (int m = 0; m < members.size(); m++) {
            LivingEntity pick = chosen[m] >= 0 ? targets.get(chosen[m]) : null;
            if (pick != null && members.get(m).getTarget() != pick) {
                members.get(m).setTarget(pick);
            }
        }
    }

    private static boolean fighting(Mob mob) {
        LivingEntity target = mob.getTarget();
        return target != null && prey(target);
    }
}
