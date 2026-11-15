package io.github.hammerknife404.dreadpatch.client;

import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
                "head/headware", "body/chestplate/necklace", "body/cloak",
                "right_arm/right_sleeve", "left_arm/left_sleeve", "right_leg/right_robe", "left_leg/left_robe"}) {
            ModelPart part = root;
            for (String name : path.split("/")) {
                part = part.getChild(name);
            }
            assertTrue(!part.isEmpty(), path + " has no cubes");
        }
    }
}
