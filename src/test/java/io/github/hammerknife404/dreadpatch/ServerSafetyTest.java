package io.github.hammerknife404.dreadpatch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * No class a dedicated server loads names a client-only class (0.98.13, a v1.0 review): a dedicated server has none of
 * them, and the dev server runs with the merged jar, so the smoke test can't catch one. Every compiled class outside
 * {@code client/} and {@code compat/jei/} is read for {@code net/minecraft/client/}, {@code com/mojang/blaze3d/} and
 * {@code net/neoforged/neoforge/client/}; the play-to-client payloads' handlers call into a client class from a lambda
 * that only a client ever runs (CLAUDE.md, Network), so those few classes are let be.
 */
class ServerSafetyTest {
    private static final String[] CLIENT_ONLY = {"net/minecraft/client/", "com/mojang/blaze3d/", "net/neoforged/neoforge/client/"};
    /** The payload senders, whose handler lambdas reach a client class (checked by hand: called only on a client). */
    private static final Set<String> HANDLERS = Set.of("BossMusic", "DreadlandsMusic", "Escalation", "DreadWeather", "RisenDread");

    @Test
    void noServerClassNamesAClientOne() throws IOException {
        Path root = Path.of("build/classes/java/main/io/github/hammerknife404/dreadpatch");
        if (!Files.isDirectory(root)) {
            return;
        }
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".class")).toList()) {
                String relative = root.relativize(file).toString().replace('\\', '/');
                if (relative.startsWith("client/") || relative.startsWith("compat/jei/")) {
                    continue;
                }
                String outer = file.getFileName().toString().replaceAll("[$.].*", "");
                if (HANDLERS.contains(outer)) {
                    continue;
                }
                String bytes = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
                for (String prefix : CLIENT_ONLY) {
                    if (bytes.contains(prefix)) {
                        offenders.add(relative + " names " + prefix);
                    }
                }
            }
        }
        assertTrue(offenders.isEmpty(), "client-only classes named where a server loads them: " + offenders);
    }
}
