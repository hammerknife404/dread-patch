package io.github.hammerknife404.dreadpatch.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Each piece of her regalia bakes into a humanoid (a missing part would only fail in a client) and carries its own part of her outfit. */
class QueenArmorModelsTest {
    private static final Map<EquipmentSlot, String[]> PARTS = Map.of(
            EquipmentSlot.HEAD, new String[]{"head/headware"},
            EquipmentSlot.CHEST, new String[]{"body/chestplate", "body/chestplate/right_bust", "body/chestplate/left_bust",
                    "body/chestplate/necklace", "body/cloak", "right_arm/right_sleeve", "left_arm/left_sleeve"},
            EquipmentSlot.LEGS, new String[]{"right_leg/right_robe", "left_leg/left_robe"},
            EquipmentSlot.FEET, new String[]{"right_leg/right_wraps", "left_leg/left_wraps"});

    @Test
    void everyPieceBakesWithItsParts() {
        for (Map.Entry<EquipmentSlot, String[]> piece : PARTS.entrySet()) {
            ModelPart root = QueenArmorModels.createLayer(piece.getKey()).bakeRoot();
            assertDoesNotThrow(() -> new HumanoidModel<>(root), piece.getKey().getName());
            for (String path : piece.getValue()) {
                ModelPart part = root;
                for (String name : path.split("/")) {
                    part = part.getChild(name);
                }
                assertTrue(!part.isEmpty(), piece.getKey().getName() + ": " + path + " has no cubes");
            }
            // Nothing of the wearer's own shape: the bare parts are empty.
            for (String bare : new String[]{"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
                assertTrue(root.getChild(bare).isEmpty(), piece.getKey().getName() + ": " + bare + " has cubes of its own");
            }
        }
    }
}
