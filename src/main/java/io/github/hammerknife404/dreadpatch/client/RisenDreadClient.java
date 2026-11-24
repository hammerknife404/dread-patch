package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/**
 * Risen Dread on a client (0.63.0, {@code entity/RisenDread}): the server marks each as a player starts
 * seeing it, and it's drawn as its own kind would be through a frost tint: every vertex's colour times
 * {@link #TINT}, a cold, dead grey-blue. Only for mobs drawn by a {@link LivingEntityRenderer} (vanilla's and
 * most mods'); one drawn otherwise (a GeckoLib renderer's) shows untinted. No mixin: its draw is cancelled
 * and done again through a tinting buffer.
 */
public final class RisenDreadClient {
    static final float[] TINT = {0.55F, 0.70F, 0.88F};
    private static final IntSet MARKED = new IntOpenHashSet();
    private static ClientLevel markedIn;
    private static boolean drawing;

    private RisenDreadClient() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(RenderLivingEvent.Pre.class, RisenDreadClient::onRender);
    }

    /** From the server: entity {@code id} is risen Dread. */
    public static void mark(int id) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != markedIn) {
            MARKED.clear();
            markedIn = level;
        }
        MARKED.add(id);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void onRender(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (drawing || entity.level() != markedIn || !MARKED.contains(entity.getId())) {
            return;
        }
        event.setCanceled(true);
        MultiBufferSource source = event.getMultiBufferSource();
        drawing = true;
        try {
            ((LivingEntityRenderer) event.getRenderer()).render(entity, entity.getYRot(), event.getPartialTick(), event.getPoseStack(),
                    type -> new Tinted(source.getBuffer(type)), event.getPackedLight());
        } finally {
            drawing = false;
        }
    }

    /** Passes every vertex on, its colour tinted. */
    private record Tinted(VertexConsumer inner) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.inner.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.inner.setColor(Math.round(red * TINT[0]), Math.round(green * TINT[1]), Math.round(blue * TINT[2]), alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.inner.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.inner.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.inner.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.inner.setNormal(x, y, z);
            return this;
        }
    }
}
