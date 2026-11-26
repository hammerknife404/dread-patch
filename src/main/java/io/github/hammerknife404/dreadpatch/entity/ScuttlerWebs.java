package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadScuttlerEntity;
import io.github.hammerknife404.dreadpatch.magic.WebRules;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Dread scuttlers spit webs (0.75.0, {@link WebRules}) at what they hunt 4-24 blocks off, in sight, every
 * 5-8 s (0.75.1), never at one already held in a web, never in the tick they spit a bolt ({@code ScuttlerBolts}).
 * Every scuttler, not while rising. The class is Ice and Fire's, so it's done from its tick.
 */
public final class ScuttlerWebs {
    /** When its next web is ready (game time), in its persistent data. */
    static final String NEXT_WEB = "dreadpatch:next_web";

    private ScuttlerWebs() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ScuttlerWebs::onTick);
        gameBus.addListener(ScuttlerWebs::onJoin);
    }

    /** 0.75.1: it notices what's 24 blocks off (its follow range, Ice and Fire's 12, raised), and it's 10% faster. Transient, set as it joins. */
    private static void onJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof DreadScuttlerEntity scuttler)) {
            return;
        }
        var follow = scuttler.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
        if (follow != null) {
            double raise = Math.max(0.0, WebRules.SCUTTLER_FOLLOW_RANGE - follow.getBaseValue());
            follow.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    io.github.hammerknife404.dreadpatch.DreadPatch.id("scuttler_reach"), raise, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        }
        var speed = scuttler.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    io.github.hammerknife404.dreadpatch.DreadPatch.id("scuttler_speed"), WebRules.SCUTTLER_SPEED_BONUS,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    private static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof DreadScuttlerEntity scuttler) || scuttler.level().isClientSide || !scuttler.isAlive()
                || scuttler.getAnimation() == DreadScuttlerEntity.ANIMATION_SPAWN) {
            return;
        }
        LivingEntity target = scuttler.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        long now = scuttler.level().getGameTime();
        CompoundTag data = scuttler.getPersistentData();
        if (now < data.getLong(NEXT_WEB) || data.getLong(ScuttlerBolts.NEXT_BOLT) - now >= ScuttlerBolts.COOLDOWN) {
            return;
        }
        double distance = scuttler.distanceTo(target);
        if (distance < WebRules.MIN_RANGE || distance > WebRules.MAX_RANGE || !scuttler.hasLineOfSight(target)) {
            return;
        }
        var held = scuttler.level().getBlockState(target.blockPosition());
        if (held.is(ModRegistries.DREAD_WEB.get()) || held.is(Blocks.COBWEB)) {
            return;
        }
        ScuttlerWeb web = new ScuttlerWeb(scuttler.level(), scuttler);
        web.setPos(scuttler.getX(), scuttler.getEyeY() - 0.2, scuttler.getZ());
        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.3, 0).subtract(web.position());
        // A lob: aimed a little high for its drop, as a snowball's thrower does.
        web.shoot(aim.x, aim.y + aim.horizontalDistance() * 0.12, aim.z, 1.1F, 2.0F);
        scuttler.level().addFreshEntity(web);
        scuttler.swing(InteractionHand.MAIN_HAND);
        scuttler.playSound(SoundEvents.SPIDER_AMBIENT, 0.7F, 0.6F);
        data.putLong(NEXT_WEB, now + WebRules.COOLDOWN + scuttler.getRandom().nextInt(WebRules.COOLDOWN_SPREAD));
    }
}
