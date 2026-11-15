package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rendering has no automated check, and a spike drawn wrong (faces lit from inside, a stretched or
 * overrun sprite) only shows in a client. Its geometry needs no game: record it and check it.
 */
class IceSpikeTest {
    private static final float HEIGHT = 2.4F;
    private static final float HALF = 0.36F;

    /** Positions, normals and texture coordinates as the spike emits them. */
    private static final class Recorder implements VertexConsumer {
        final List<Vector3f> positions = new ArrayList<>();
        final List<Vector3f> normals = new ArrayList<>();
        final List<float[]> uvs = new ArrayList<>();

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.positions.add(new Vector3f(x, y, z));
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.uvs.add(new float[]{u, v});
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.normals.add(new Vector3f(x, y, z));
            return this;
        }
    }

    private static Recorder draw() {
        Recorder out = new Recorder();
        // A sprite at u 0.25-0.5, v 0.5-0.75 of its atlas.
        IceSpike.draw(new PoseStack().last(), out, 0.25F, 0.5F, 0.5F, 0.75F, HEIGHT, HALF, 0);
        return out;
    }

    @Test
    void fourFacesOfBlockTallSegments() {
        Recorder out = draw();
        int segments = 3;  // 2.4 blocks tall
        assertEquals(4 * segments * 4, out.positions.size());
        assertEquals(out.positions.size(), out.normals.size());
        assertEquals(out.positions.size(), out.uvs.size());
    }

    @Test
    void tapersFromItsBaseToAPoint() {
        for (Vector3f p : draw().positions) {
            assertTrue(p.y >= 0.0F && p.y <= HEIGHT + 1e-5F, "height " + p.y);
            float width = HALF * (1.0F - p.y / HEIGHT);
            assertTrue(Math.abs(p.x) <= width + 1e-5F && Math.abs(p.z) <= width + 1e-5F, "outside the spike: " + p);
            if (p.y >= HEIGHT - 1e-5F) {
                assertTrue(Math.abs(p.x) < 1e-5F && Math.abs(p.z) < 1e-5F, "the tip isn't a point: " + p);
            }
        }
    }

    @Test
    void facesAreLitFromOutside() {
        Recorder out = draw();
        for (int i = 0; i < out.positions.size(); i++) {
            Vector3f p = out.positions.get(i);
            Vector3f n = out.normals.get(i);
            assertEquals(1.0F, n.length(), 1e-4F);
            assertTrue(n.y > 0.0F, "a face looks down: " + n);
            if (p.y < HEIGHT - 1e-5F) {
                assertTrue(n.x * p.x + n.z * p.z > 0.0F, "normal " + n + " points into the spike at " + p);
            }
        }
    }

    @Test
    void staysInsideItsSprite() {
        for (float[] uv : draw().uvs) {
            assertTrue(uv[0] >= 0.25F && uv[0] <= 0.5F, "u " + uv[0]);
            assertTrue(uv[1] >= 0.5F && uv[1] <= 0.75F, "v " + uv[1]);
        }
    }
}
