package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.IceDragonChargeEntity;
import com.iafenvoy.iceandfire.event.DragonFireDamageWorldEvent;
import io.github.hammerknife404.dreadpatch.magic.DragonBreath;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Where Syndrigossa's attacks leave the ice dragon's:
 * <ul>
 * <li>Ice and Fire's breath (and charge) damage, which it posts as a cancellable
 * {@code DragonFireDamageWorldEvent} where it lands, is hers: cancelled, and her
 * {@link Syndrigossa#witheringBreath} there instead;</li>
 * <li>an ice charge she looses anyway (Ice and Fire's {@code IceDragonEntity} makes them directly in
 * its private {@code shootIceAtMob} and in {@code riderShootFire}, not through
 * {@code createCharge}) becomes Shatter-Spine as it enters the world;</li>
 * <li>each breath hit siphons {@link DragonBreath#SIPHON} more, past armor, into her.</li>
 * </ul>
 */
public final class SyndrigossaEvents {
    private SyndrigossaEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SyndrigossaEvents::onDragonFireDamage);
        gameBus.addListener(SyndrigossaEvents::onJoin);
        gameBus.addListener(SyndrigossaEvents::onDamage);
    }

    private static void onDragonFireDamage(DragonFireDamageWorldEvent event) {
        if (event.getDragon() instanceof Syndrigossa dragon) {
            event.setCanceled(true);
            dragon.witheringBreath(new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ()));
        }
    }

    /** Swapped a tick later: adding an entity while another is joining isn't safe. */
    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof IceDragonChargeEntity charge && charge.getOwner() instanceof Syndrigossa dragon
                && event.getLevel() instanceof ServerLevel level) {
            event.setCanceled(true);
            Vec3 from = charge.position();
            Vec3 direction = charge.getDeltaMovement();
            MinecraftServer server = level.getServer();
            server.tell(new TickTask(server.getTickCount() + 1, () -> {
                if (dragon.isAlive()) {
                    level.addFreshEntity(ShatterSpineCore.launch(level, dragon, from, direction, ShatterSpineCore.aimOver(dragon.getTarget())));
                }
            }));
        }
    }

    private static void onDamage(LivingDamageEvent.Pre event) {
        if (event.getSource().is(ModRegistries.WITHERING_BREATH_DAMAGE) && event.getSource().getEntity() instanceof Syndrigossa dragon) {
            event.setNewDamage(event.getNewDamage() + DragonBreath.SIPHON);
            dragon.heal(DragonBreath.SIPHON);
        }
    }
}
