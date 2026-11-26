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
            "head", RegaliaGeometry::head, "chest", RegaliaGeometry::chest, "chest_winged", RegaliaGeometry::chestWinged,
            "legs", RegaliaGeometry::legs, "feet", RegaliaGeometry::feet);
    private static final Map<String, String[]> PARTS = Map.of(
            "head", new String[]{"head/head_regalia"},
            "chest", new String[]{"body/body_regalia", "right_arm/right_arm_regalia", "left_arm/left_arm_regalia"},
            "chest_winged", new String[]{"body/body_regalia", "right_arm/right_arm_regalia", "left_arm/left_arm_regalia",
                    "body/right_wing", "body/left_wing", "body/right_wing/right_wing_mid", "body/right_wing/right_wing_mid/right_wing_tip",
                    "body/left_wing/left_wing_mid/left_wing_tip"},
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
        // Only the winged bodice has wings.
        assertTrue(!RegaliaGeometry.chest().bakeRoot().getChild("body").hasChild("right_wing"), "the plain bodice has wings");
    }
}
