package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A missing or misnamed part only fails when a client bakes the model while loading, which neither
 * the build nor a dedicated server reaches. Baking needs no game, so check it here.
 */
class DreadQueenModelTest {
    @Test
    void bakesIntoAHumanoidModel() {
        ModelPart root = DreadQueenModel.createBodyLayer().bakeRoot();
        assertDoesNotThrow(() -> new DreadQueenModel<>(root));
    }

    @Test
    void keepsEveryPartOfTheOriginal() {
        ModelPart root = DreadQueenModel.createBodyLayer().bakeRoot();
        for (String path : new String[]{
                "head/headware", "body/chestplate/necklace", "body/cloak", "body/chestplate/right_bust", "body/chestplate/left_bust",
                "right_arm/right_sleeve", "left_arm/left_sleeve", "right_leg/right_robe", "left_leg/left_robe"}) {
            ModelPart part = root;
            for (String name : path.split("/")) {
                part = part.getChild(name);
            }
            assertTrue(!part.isEmpty(), path + " has no cubes");
        }
    }

    /** Texture coordinates as a part emits them, in texels of her 128×64 sheet. */
    private static final class Recorder implements VertexConsumer {
        final List<float[]> uvs = new ArrayList<>();

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.uvs.add(new float[]{u * 128.0F, v * 64.0F});
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
            return this;
        }
    }

    /**
     * Her bust is ours, drawn from Ice and Fire's texture, which has nothing for it: every texel any
     * face of it takes must be opaque there, or it shows a hole (her chestplate's straps and neckline
     * are cut out of the rows above the ones it uses).
     */
    @Test
    void herBustIsWholeInIceAndFiresTexture() throws IOException {
        BufferedImage texture;
        try (InputStream in = DreadQueenModelTest.class.getResourceAsStream("/assets/iceandfire/textures/entity/dread/dread_queen.png")) {
            assertNotNull(in, "Ice and Fire's queen texture on the test classpath");
            texture = ImageIO.read(in);
        }
        assertEquals(128, texture.getWidth());
        ModelPart chestplate = DreadQueenModel.createBodyLayer().bakeRoot().getChild("body").getChild("chestplate");
        for (String name : new String[]{"right_bust", "left_bust"}) {
            Recorder out = new Recorder();
            chestplate.getChild(name).render(new PoseStack(), out, 0, 0);
            assertEquals(24, out.uvs.size(), name + ": six faces");
            for (int face = 0; face < out.uvs.size(); face += 4) {
                float u0 = Float.MAX_VALUE, v0 = Float.MAX_VALUE, u1 = -Float.MAX_VALUE, v1 = -Float.MAX_VALUE;
                for (float[] uv : out.uvs.subList(face, face + 4)) {
                    u0 = Math.min(u0, uv[0]);
                    v0 = Math.min(v0, uv[1]);
                    u1 = Math.max(u1, uv[0]);
                    v1 = Math.max(v1, uv[1]);
                }
                for (int v = Math.round(v0); v < Math.round(v1); v++) {
                    for (int u = Math.round(u0); u < Math.round(u1); u++) {
                        int alpha = texture.getRGB(u, v) >>> 24;
                        assertEquals(255, alpha, name + " face " + face / 4 + " takes a clear texel at " + u + ", " + v);
                    }
                }
            }
        }
    }
}
