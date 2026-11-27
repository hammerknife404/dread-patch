package io.github.hammerknife404.dreadpatch.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A dread phantom's bite against a shield, and a small one's without knockback (0.85.0, the owner: "Fix attacks
 * bypassing raised shields, remove knockback from small variants").
 * <ul>
 * <li>A phantom bites when its box touches its target's, so it's often right over them as it lands: vanilla takes a
 * shield's block from where the attacker stands, which, straight overhead or just past, is seldom in front. A raised
 * shield facing where its dive came from ({@link #cameFrom}: a few ticks back along its flight) blocks it.</li>
 * <li>A miniature one's bite knocks nothing back: the knockback that follows its hurting (vanilla's, in the same tick)
 * is cancelled.</li>
 * </ul>
 */
public final class PhantomHits {
    /** Server thread only: who a miniature phantom bit, and on which tick. */
    private static final Map<LivingEntity, Integer> BITTEN = new WeakHashMap<>();
    /** How far back along its flight its dive is taken to come from, in ticks. */
    static final double LOOK_BACK = 4.0;

    private PhantomHits() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(PhantomHits::onShield);
        gameBus.addListener(PhantomHits::onIncoming);
        gameBus.addListener(PhantomHits::onKnockBack);
    }

    /** Where a phantom's dive came from: a few ticks back along its flight. */
    static Vec3 cameFrom(DreadPhantom phantom) {
        return phantom.position().subtract(phantom.getDeltaMovement().scale(LOOK_BACK));
    }

    private static void onShield(LivingShieldBlockEvent event) {
        LivingEntity blocker = event.getEntity();
        if (event.getBlocked() || !blocker.isBlocking() || !(event.getDamageSource().getDirectEntity() instanceof DreadPhantom phantom)) {
            return;
        }
        Vec3 view = blocker.calculateViewVector(0.0F, blocker.getYHeadRot());
        Vec3 toward = cameFrom(phantom).vectorTo(blocker.position());
        toward = new Vec3(toward.x, 0.0, toward.z);
        if (toward.lengthSqr() > 1e-6 && toward.normalize().dot(view) < 0.0) {
            event.setBlocked(true);
        }
    }

    private static void onIncoming(LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof MiniatureDreadPhantom) {
            BITTEN.put(event.getEntity(), event.getEntity().tickCount);
        }
    }

    private static void onKnockBack(LivingKnockBackEvent event) {
        Integer tick = BITTEN.get(event.getEntity());
        if (tick != null && tick == event.getEntity().tickCount) {
            event.setCanceled(true);
        }
    }
}
