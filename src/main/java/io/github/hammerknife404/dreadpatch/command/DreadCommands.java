package io.github.hammerknife404.dreadpatch.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.hammerknife404.dreadpatch.entity.DreadPatrols;
import io.github.hammerknife404.dreadpatch.magic.WeatherRules;
import io.github.hammerknife404.dreadpatch.world.Dreadlands;
import io.github.hammerknife404.dreadpatch.world.DreadlandsData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;

/**
 * {@code /dreadpatch} (0.56.0, the owner: "weather commands for dread" and "other dreadpatch related
 * console/server commands"), for operators (level 2) and the console:
 * <ul>
 * <li>{@code weather}: what the Dreadlands' weather is (a storm, a whiteout, whether the last was one);</li>
 * <li>{@code weather clear|snow|storm|whiteout [seconds]}: sets it. The Dreadlands share the overworld's
 * weather, so the overworld's changes too; {@code whiteout} makes this storm one, past its rolls;</li>
 * <li>{@code encounters reset [players]}: forgets who's met which spire's encounter (everyone's, by default);</li>
 * <li>{@code dreadlands enter|home [players]}: into the Dreadlands, by the nearest spire, or home again;</li>
 * <li>{@code patrol [pos]} and {@code rise [count] [pos]}: a Dread patrol, or 1-8 loose Dread, there.</li>
 * </ul>
 */
public final class DreadCommands {
    /** A weather set with no time given lasts this long: vanilla's {@code /weather} is random; ours, 10 minutes. */
    private static final int DEFAULT_SECONDS = 600;

    private DreadCommands() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((RegisterCommandsEvent event) -> register(event.getDispatcher()));
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dreadpatch").requires(source -> source.hasPermission(2))
                .then(Commands.literal("weather")
                        .executes(DreadCommands::queryWeather)
                        .then(weather("clear")).then(weather("snow")).then(weather("storm")).then(weather("whiteout")))
                .then(Commands.literal("encounters").then(Commands.literal("reset")
                        .executes(context -> resetEncounters(context, null))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> resetEncounters(context, EntityArgument.getPlayers(context, "players"))))))
                .then(Commands.literal("dreadlands")
                        .then(Commands.literal("enter")
                                .executes(context -> enter(context, List.of(context.getSource().getPlayerOrException())))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> enter(context, EntityArgument.getPlayers(context, "players")))))
                        .then(Commands.literal("home")
                                .executes(context -> home(context, List.of(context.getSource().getPlayerOrException())))
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> home(context, EntityArgument.getPlayers(context, "players"))))))
                .then(Commands.literal("patrol")
                        .executes(context -> patrol(context, context.getSource().getPosition()))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(context -> patrol(context, Vec3Argument.getVec3(context, "pos")))))
                .then(Commands.literal("rise")
                        .executes(context -> rise(context, 3, context.getSource().getPosition()))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 8))
                                .executes(context -> rise(context, IntegerArgumentType.getInteger(context, "count"), context.getSource().getPosition()))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(context -> rise(context, IntegerArgumentType.getInteger(context, "count"), Vec3Argument.getVec3(context, "pos")))))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> weather(String kind) {
        return Commands.literal(kind)
                .executes(context -> setWeather(context, kind, DEFAULT_SECONDS))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 1_000_000))
                        .executes(context -> setWeather(context, kind, IntegerArgumentType.getInteger(context, "seconds"))));
    }

    private static int queryWeather(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        WeatherRules.Storms storms = DreadlandsData.get(server).storms();
        boolean raining = server.overworld().getLevelData().isRaining();
        boolean thundering = server.overworld().getLevelData().isThundering();
        String now = !raining ? "clear" : storms.whiteout ? "whiteout" : thundering ? "storm" : "snow";
        context.getSource().sendSuccess(() -> Component.translatable("commands.dreadpatch.weather.query",
                Component.translatable("commands.dreadpatch.weather." + now),
                Component.translatable(storms.lastWhiteout ? "gui.yes" : "gui.no")), false);
        return raining ? 1 : 0;
    }

    private static int setWeather(CommandContext<CommandSourceStack> context, String kind, int seconds) {
        MinecraftServer server = context.getSource().getServer();
        int ticks = seconds * 20;
        boolean raining = !kind.equals("clear");
        server.overworld().setWeatherParameters(raining ? 0 : ticks, raining ? ticks : 0, raining, kind.equals("storm"));
        DreadlandsData data = DreadlandsData.get(server);
        WeatherRules.Storms storms = data.storms();
        if (kind.equals("whiteout")) {
            // This storm is one, whatever its roll was or would be.
            storms.storming = true;
            storms.whiteout = true;
            storms.wasWhiteout = true;
        } else if (raining) {
            // A plain storm now, not left to a roll.
            storms.storming = true;
            storms.whiteout = false;
            storms.wasWhiteout = false;
        }
        data.setDirty();
        context.getSource().sendSuccess(() -> Component.translatable("commands.dreadpatch.weather.set",
                Component.translatable("commands.dreadpatch.weather." + kind), seconds), true);
        return 1;
    }

    private static int resetEncounters(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
        DreadlandsData data = DreadlandsData.get(context.getSource().getServer());
        int forgotten = players == null ? data.forgetEncounters(null) : players.stream().mapToInt(p -> data.forgetEncounters(p.getUUID())).sum();
        context.getSource().sendSuccess(() -> Component.translatable("commands.dreadpatch.encounters.reset", forgotten), true);
        return forgotten;
    }

    private static int enter(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        int sent = 0;
        for (ServerPlayer player : players) {
            if (Dreadlands.is(player.level())) {
                continue;
            }
            Dreadlands.Arrival arrival = Dreadlands.arrival(server, player.blockPosition(), player.getRandom());
            if (arrival == null) {
                context.getSource().sendFailure(Component.translatable("commands.dreadpatch.dreadlands.missing"));
                return 0;
            }
            Dreadlands.enter(player, arrival, new DreadlandsData.Origin(player.level().dimension(), player.position(), player.getYRot()));
            sent++;
        }
        int count = sent;
        context.getSource().sendSuccess(() -> Component.translatable("commands.dreadpatch.dreadlands.enter", count), true);
        return count;
    }

    private static int home(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
        int sent = 0;
        for (ServerPlayer player : players) {
            if (Dreadlands.is(player.level())) {
                Dreadlands.goHome(player);
                sent++;
            }
        }
        int count = sent;
        context.getSource().sendSuccess(() -> Component.translatable("commands.dreadpatch.dreadlands.home", count), true);
        return count;
    }

    private static int patrol(CommandContext<CommandSourceStack> context, Vec3 at) {
        ServerLevel level = context.getSource().getLevel();
        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2.0;
        BlockPos toward = BlockPos.containing(at.add(Math.cos(angle) * 64.0, 0.0, Math.sin(angle) * 64.0));
        int raised = DreadPatrols.raise(level, at, toward, random);
        return report(context, raised);
    }

    private static int rise(CommandContext<CommandSourceStack> context, int count, Vec3 at) {
        ServerLevel level = context.getSource().getLevel();
        return report(context, DreadPatrols.raiseLoose(level, at, count, level.getRandom()));
    }

    private static int report(CommandContext<CommandSourceStack> context, int raised) {
        if (raised == 0) {
            context.getSource().sendFailure(Component.translatable("commands.dreadpatch.rise.none"));
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("commands.dreadpatch.rise.done", raised), true);
        }
        return raised;
    }
}
