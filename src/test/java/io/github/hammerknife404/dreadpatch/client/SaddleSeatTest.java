package io.github.hammerknife404.dreadpatch.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seat is measured off Ice and Fire's dragon models ({@code BodyUpper}'s cube); a change there would
 * leave riders floating or sunk. Ice and Fire's jar is on the test classpath.
 */
class SaddleSeatTest {
    /**
     * Every Ice and Fire dragon's model (0.49.0: a player rider sits on any of them): fire, ice and
     * lightning share the same {@code BodyUpper} cube.
     */
    @ParameterizedTest
    @ValueSource(strings = {"firedragon", "icedragon", "lightningdragon"})
    void riderSitsOnTopOfTheDragonsUpperBody(String model) throws Exception {
        try (InputStream in = SaddleSeatTest.class.getResourceAsStream("/assets/iceandfire/geo/" + model + ".geo.json")) {
            assertNotNull(in, "Ice and Fire's " + model + ".geo.json on the classpath");
            JsonObject geometry = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
                    .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonObject body = null;
            for (JsonElement bone : geometry.getAsJsonArray("bones")) {
                if ("BodyUpper".equals(bone.getAsJsonObject().get("name").getAsString())) {
                    body = bone.getAsJsonObject();
                }
            }
            assertNotNull(body, "BodyUpper");
            JsonObject cube = body.getAsJsonArray("cubes").get(0).getAsJsonObject();
            JsonArray origin = cube.getAsJsonArray("origin");
            JsonArray size = cube.getAsJsonArray("size");
            float top = (origin.get(1).getAsFloat() + size.get(1).getAsFloat()) / 16.0F;
            float front = origin.get(2).getAsFloat() / 16.0F;
            float back = front + size.get(2).getAsFloat() / 16.0F;
            assertEquals(top, SaddleSeat.Y, 1.0E-4F, "on its top");
            assertTrue(SaddleSeat.Z > front && SaddleSeat.Z < back, "over it, front to back");
        }
    }
}
