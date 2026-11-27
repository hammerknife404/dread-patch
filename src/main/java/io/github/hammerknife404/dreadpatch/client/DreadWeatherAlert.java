package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.magic.DreadWeatherRules;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * A dread storm's warning as it starts (0.89.0, the owner: "Show random on-screen warning in fading rimefrost colors on
 * storm start"): one of {@link DreadWeatherRules#ALERT_LINES} lines ({@code message.dreadpatch.dread_weather.<n>}),
 * centred a third of the way down, fading in, held, fading out ({@link DreadWeatherRules#alertAlpha}), its colour drifting
 * through the Rimefrost's pale cyans and frost white as it shows.
 */
public final class DreadWeatherAlert {
    /** Rimefrost's colours: frost white, pale cyan, dread-fire cyan, pale cyan again. */
    private static final int[] COLOURS = {0xE8FBFF, 0xAEEBFF, 0x5FD3F0, 0xAEEBFF};
    private static Component line;
    private static int age = DreadWeatherRules.ALERT_TICKS;

    private DreadWeatherAlert() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(DreadWeatherAlert::onRegisterLayers);
        gameBus.addListener(DreadWeatherAlert::onClientTick);
    }

    /** From the server: warning {@code index} starts now. */
    public static void show(int index) {
        line = Component.translatable("message.dreadpatch.dread_weather." + Mth.clamp(index, 0, DreadWeatherRules.ALERT_LINES - 1));
        age = 0;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (age < DreadWeatherRules.ALERT_TICKS && !Minecraft.getInstance().isPaused()) {
            age++;
        }
    }

    private static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.TITLE, DreadPatch.id("dread_weather_alert"), DreadWeatherAlert::render);
    }

    private static int colour(float t) {
        float at = (t % 1.0F) * COLOURS.length;
        int from = COLOURS[(int) at % COLOURS.length];
        int to = COLOURS[((int) at + 1) % COLOURS.length];
        float mix = at - (int) at;
        return FastColor.ARGB32.lerp(mix, from | 0xFF000000, to | 0xFF000000) & 0xFFFFFF;
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        if (line == null) {
            return;
        }
        float time = age + delta.getGameTimeDeltaPartialTick(false);
        float alpha = DreadWeatherRules.alertAlpha((int) time);
        if (alpha <= 0.02F) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2.0F, height / 3.0F, 0.0F);
        graphics.pose().scale(1.5F, 1.5F, 1.0F);
        int argb = ((int) (alpha * 255.0F) << 24) | colour(time / DreadWeatherRules.ALERT_TICKS * 1.5F);
        graphics.drawString(font, line, -font.width(line) / 2, -font.lineHeight / 2, argb, true);
        graphics.pose().popPose();
    }
}
