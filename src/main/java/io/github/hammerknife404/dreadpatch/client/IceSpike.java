package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** The Rimefire Crown's ice spikes, as geometry: needs no game, so {@code IceSpikeTest} checks it. */
final class IceSpike {
    private IceSpike() {
    }

    /**
     * A four-sided spike from the origin up the y axis: {@code half} from its axis to each face at the
     * base, narrowing to a point at {@code height}. Drawn in segments at most a block tall, each face
     * cropped from the middle of the sprite at its true width, so the ice isn't stretched. Paler
     * toward the tip; the cyan below is the fire's light.
     */
    static void draw(PoseStack.Pose pose, VertexConsumer out, float u0, float u1, float v0, float v1, float height, float half, int light) {
        draw(pose, out, u0, u1, v0, v1, height, half, light, 1.0F, 1.0F, 1.0F);
    }

    /** As {@link #draw(PoseStack.Pose, VertexConsumer, float, float, float, float, float, float, int)}, its colour multiplied by a tint. */
    static void draw(PoseStack.Pose pose, VertexConsumer out, float u0, float u1, float v0, float v1, float height, float half, int light,
                     float tintR, float tintG, float tintB) {
        if (height <= 0.0F) {
            return;
        }
        int segments = Math.max(1, Mth.ceil(height));
        float slope = (float) Math.sqrt(height * height + half * half);
        for (int face = 0; face < 4; face++) {
            // Outward direction of this face, and the direction along it.
            int nx = face == 0 ? 1 : face == 2 ? -1 : 0;
            int nz = face == 1 ? 1 : face == 3 ? -1 : 0;
            int tx = -nz;
            int tz = nx;
            float normalX = nx * height / slope;
            float normalY = half / slope;
            float normalZ = nz * height / slope;
            for (int s = 0; s < segments; s++) {
                float yLow = height * s / segments;
                float yHigh = height * (s + 1) / segments;
                float wLow = half * (1.0F - yLow / height);
                float wHigh = half * (1.0F - yHigh / height);
                float vLow = v1;
                float vHigh = Mth.lerp(yHigh - yLow, v1, v0);
                float[] tint = {tintR, tintG, tintB};
                vertex(pose, out, nx, nz, tx, tz, -1, wLow, yLow, Mth.lerp(0.5F - wLow, u0, u1), vLow, light, normalX, normalY, normalZ, tint);
                vertex(pose, out, nx, nz, tx, tz, 1, wLow, yLow, Mth.lerp(0.5F + wLow, u0, u1), vLow, light, normalX, normalY, normalZ, tint);
                vertex(pose, out, nx, nz, tx, tz, 1, wHigh, yHigh, Mth.lerp(0.5F + wHigh, u0, u1), vHigh, light, normalX, normalY, normalZ, tint);
                vertex(pose, out, nx, nz, tx, tz, -1, wHigh, yHigh, Mth.lerp(0.5F - wHigh, u0, u1), vHigh, light, normalX, normalY, normalZ, tint);
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer out, int nx, int nz, int tx, int tz, int side, float w, float y,
                               float u, float v, int light, float normalX, float normalY, float normalZ, float[] tint) {
        float pale = Mth.clamp(y / 1.2F, 0.0F, 1.0F);
        out.addVertex(pose, (nx + tx * side) * w, y, (nz + tz * side) * w)
                .setColor(Mth.lerp(pale, 0.72F, 1.0F) * tint[0], Mth.lerp(pale, 0.92F, 1.0F) * tint[1], tint[2], 1.0F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, normalX, normalY, normalZ);
    }
}
