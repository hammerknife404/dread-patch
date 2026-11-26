package io.github.hammerknife404.dreadpatch.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each piece of her regalia (0.81.0's, {@link RegaliaGeometry}) bakes into a humanoid (a missing part would only fail in a
 * client) and carries its own boxes; the winged bodice its wings too.
 */
class QueenArmorModelsTest {
    private static final Map<String, Supplier<LayerDefinition>> LAYERS = Map.of(
            "head", RegaliaGeometry::head, "chest", RegaliaGeometry::chest,
            "legs", RegaliaGeometry::legs, "feet", RegaliaGeometry::feet);
    private static final Map<String, String[]> PARTS = Map.of(
            "head", new String[]{"head/head_regalia"},
            "chest", new String[]{"body/body_regalia", "right_arm/right_arm_regalia", "left_arm/left_arm_regalia"},
            "legs", new String[]{"right_leg/right_leg_regalia", "left_leg/left_leg_regalia"},
            "feet", new String[]{"right_leg/right_leg_regalia", "left_leg/left_leg_regalia"});

    @Test
    void everyPieceBakesWithItsParts() {
        for (Map.Entry<String, String[]> piece : PARTS.entrySet()) {
            ModelPart root = LAYERS.get(piece.getKey()).get().bakeRoot();
            assertDoesNotThrow(() -> new HumanoidModel<>(root), piece.getKey());
            for (String path : piece.getValue()) {
                ModelPart part = root;
                for (String name : path.split("/")) {
                    part = part.getChild(name);
                }
                assertTrue(!part.isEmpty(), piece.getKey() + ": " + path + " has no cubes");
            }
            // Nothing of the wearer's own shape: the bare parts are empty.
            for (String bare : new String[]{"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
                assertTrue(root.getChild(bare).isEmpty(), piece.getKey() + ": " + bare + " has cubes of its own");
            }
        }
        // The wings (0.82.0) are a model of their own: each a chain, root, arm, hand, four fingers, all with cubes.
        ModelPart wings = RegaliaWingsGeometry.create().bakeRoot();
        for (String side : new String[]{"right_wing", "left_wing"}) {
            ModelPart hand = wings.getChild(side).getChild(side + "_arm").getChild(side + "_hand");
            assertTrue(!hand.isEmpty(), side + ": its hand has no cubes");
            for (int i = 0; i < 4; i++) {
                assertTrue(!hand.getChild(side + "_finger" + i).isEmpty(), side + ": finger " + i + " has no cubes");
            }
        }
        assertDoesNotThrow(() -> new RegaliaWingsModel(wings));
    }
}
