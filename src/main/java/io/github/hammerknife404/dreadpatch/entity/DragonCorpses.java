package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Nothing fights a dragon's corpse (0.35.0, the owner's ask: dread mobs fighting a dragon when it died
 * hacked at its corpse forever). Ice and Fire keeps a dead dragon "alive" ({@code DragonBaseEntity.isAlive}
 * is true while its corpse lies, until it's removed), so a mob's target goal kept it, and its own dread
 * mobs' goal only skips corpses when picking a new target. Every Ice and Fire dragon's, Syndrigossa's too:
 * <ul>
 * <li>the corpse itself, the owner's suggestion for other mods' sake: it's made invulnerable, so it can't
 * be "seen as an enemy" ({@code LivingEntity.canBeSeenAsEnemy}), which vanilla's targeting checks
 * ({@code TargetingConditions}, {@code Mob.canAttack}: a running target goal drops it, brains' sensors
 * skip it) and so do mods using them. Nothing changes for the corpse: Ice and Fire's {@code hurt} takes
 * only the void's damage from one, which bypasses invulnerability, and it's harvested by right-click;</li>
 * <li>and any mob's target: a corpse handed to it directly (a target set outside its goals, as the
 * queen hands hers to her court) is refused, and dropped if it has one (checked every half second).</li>
 * </ul>
 */
public final class DragonCorpses {
    private DragonCorpses() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DragonCorpses::onChangeTarget);
        gameBus.addListener(DragonCorpses::onTick);
    }

    /** A dead dragon's corpse, which Ice and Fire still counts as alive. */
    public static boolean isCorpse(@Nullable Entity entity) {
        return entity instanceof DragonBaseEntity dragon && dragon.isModelDead();
    }

    private static void onChangeTarget(LivingChangeTargetEvent event) {
        if (isCorpse(event.getNewAboutToBeSetTarget())) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    private static void onTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        if (entity instanceof DragonBaseEntity dragon && dragon.isModelDead() && !dragon.isInvulnerable()) {
            dragon.setInvulnerable(true);
        } else if (entity instanceof Mob mob && mob.tickCount % 10 == 0 && isCorpse(mob.getTarget())) {
            mob.setTarget(null);
        }
    }
}
