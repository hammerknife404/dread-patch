package io.github.hammerknife404.dreadpatch.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Syndrigossa's animation layers play Ice and Fire's ice dragon animations by name; a misspelt one
 * only shows in a client as a frozen pose. Ice and Fire's jar is on the test classpath, so check the
 * names against its animation file.
 */
class SyndrigossaAnimationsTest {
    private static final List<String> PLAYED = List.of(
            "attack_stream_charge", "attack_stream_breath", "attack_blast_charge", "attack_blast_breath",
            "dead", "swim", "flight", "sleeping", "sitting", "walk", "ground",
            "bite", "grab_shake", "wing_blast", "roar", "epic_roar", "tail");

    @Test
    void everyAnimationSheUsesIsInIceAndFiresFile() throws Exception {
        try (InputStream in = SyndrigossaAnimationsTest.class.getResourceAsStream("/assets/iceandfire/animations/icedragon.animation.json")) {
            assertNotNull(in, "Ice and Fire's icedragon.animation.json on the classpath");
            JsonObject animations = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations");
            for (String name : PLAYED) {
                assertTrue(animations.has("animation.icedragon." + name), name);
            }
        }
    }
}
