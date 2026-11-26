package io.github.hammerknife404.dreadpatch.client;

import com.iafenvoy.iceandfire.entity.DreadKnightEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.hammerknife404.dreadpatch.magic.KnightRules;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Quaternionf;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A dread knight crouched (0.75.0, {@code entity/DreadKnights}) is drawn ducking: Ice and Fire's model,
 * whose parts we can't reach, leans forward {@link KnightRules#LEAN_DEGREES} from its feet and is squashed
 * so it stands no taller than its crouched box. Pushed on {@link RenderLivingEvent.Pre} (lowest priority, so
 * no later listener cancels it after the push) and popped on Post.
 */
public final class DreadKnightCrouch {
    /** The knights pushed this frame, so a Post pops only what a Pre pushed. */
    private static final Set<DreadKnightEntity> PUSHED = Collections.newSetFromMap(new WeakHashMap<>());

    private DreadKnightCrouch() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EventPriority.LOWEST, DreadKnightCrouch::onPre);
        gameBus.addListener(DreadKnightCrouch::onPost);
    }

    private static void onPre(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof DreadKnightEntity knight) || knight.getPose() != Pose.CROUCHING || knight.isPassenger()) {
            return;
        }
        float standing = knight.getType().getDimensions().scale(knight.getScale()).height();
        float squash = KnightRules.drawnSquash(standing, knight.getBbHeight());
        float yaw = Mth.rotLerp(event.getPartialTick(), knight.yBodyRotO, knight.yBodyRot) * Mth.DEG_TO_RAD;
        // Its facing, and the axis that tips up toward it: up x forward.
        float fx = -Mth.sin(yaw);
        float fz = Mth.cos(yaw);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.mulPose(new Quaternionf().rotateAxis(KnightRules.LEAN_DEGREES * Mth.DEG_TO_RAD, fz, 0.0F, -fx));
        pose.scale(1.0F, squash, 1.0F);
        PUSHED.add(knight);
    }

    private static void onPost(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() instanceof DreadKnightEntity knight && PUSHED.remove(knight)) {
            event.getPoseStack().popPose();
        }
    }
}
