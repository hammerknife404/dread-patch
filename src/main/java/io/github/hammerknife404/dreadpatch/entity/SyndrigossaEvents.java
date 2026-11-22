package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.IceDragonChargeEntity;
import com.iafenvoy.iceandfire.event.DragonFireDamageWorldEvent;
import io.github.hammerknife404.dreadpatch.magic.DragonBreath;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Where a Rimefrost dragon's attacks (Syndrigossa's among them) leave the ice dragon's:
 * <ul>
 * <li>Ice and Fire's breath (and charge) damage, which it posts as a cancellable
 * {@code DragonFireDamageWorldEvent} where it lands, is hers: cancelled, and her
 * {@link RimefrostDragon#witheringBreath} there instead;</li>
 * <li>an ice charge she looses anyway (Ice and Fire's {@code IceDragonEntity} makes them directly in
 * its private {@code shootIceAtMob} and in {@code riderShootFire}, not through
 * {@code createCharge}) becomes Shatter-Spine as it enters the world;</li>
 * <li>each breath hit siphons {@link DragonBreath#SIPHON} more, past armor, into her (shared with her
 * queen, {@link SoulTether});</li>
 * <li>her bite leaves Withering Rime II ({@link RimefrostDragon#BITE_RIME_TICKS}), as every Rimefrost
 * dragon's does (the owner's ask): a melee hit of hers while her bite or her prey-shaking plays, which
 * is when Ice and Fire lands it (it picks the attack's animation first and hits on its tick), so her
 * tail and wing strikes don't carry it;</li>
 * <li>she's undead, and the Wither effect doesn't take (the undead tag already keeps Poison and
 * Regeneration off her, and Withering Rime is refused to all the Dread in {@code DreadEvents}).</li>
 * </ul>
 */
public final class SyndrigossaEvents {
    private SyndrigossaEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SyndrigossaEvents::onDragonFireDamage);
        gameBus.addListener(SyndrigossaEvents::onJoin);
        gameBus.addListener(SyndrigossaEvents::onDamage);
        gameBus.addListener(SyndrigossaEvents::onBite);
        gameBus.addListener(SyndrigossaEvents::onEffectApplicable);
    }

    private static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof RimefrostDragon && event.getEffectInstance().is(MobEffects.WITHER)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    private static void onDragonFireDamage(DragonFireDamageWorldEvent event) {
        if (event.getDragon() instanceof RimefrostDragon dragon) {
            event.setCanceled(true);
            dragon.witheringBreath(new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ()));
        }
    }

    /** Swapped a tick later: adding an entity while another is joining isn't safe. */
    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof IceDragonChargeEntity charge && charge.getOwner() instanceof RimefrostDragon dragon
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
        if (event.getSource().is(ModRegistries.WITHERING_BREATH_DAMAGE) && event.getSource().getEntity() instanceof RimefrostDragon dragon) {
            event.setNewDamage(event.getNewDamage() + DragonBreath.SIPHON);
            SoulTether.heal(dragon, DragonBreath.SIPHON);
        }
    }

    private static void onBite(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0 && event.getSource().is(DamageTypes.MOB_ATTACK)
                && event.getSource().getDirectEntity() instanceof RimefrostDragon dragon && dragon.biting() && event.getEntity().isAlive()) {
            event.getEntity().addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, RimefrostDragon.BITE_RIME_TICKS, RimefrostDragon.BITE_RIME_AMPLIFIER), dragon);
        }
    }
}
