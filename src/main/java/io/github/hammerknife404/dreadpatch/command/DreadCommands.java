package io.github.hammerknife404.dreadpatch.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.hammerknife404.dreadpatch.DreadPatchConfig;
import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.neoforge.common.ModConfigSpec;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.hammerknife404.dreadpatch.entity.DreadPatrols;
import io.github.hammerknife404.dreadpatch.magic.EscalationRules;
import io.github.hammerknife404.dreadpatch.magic.WeatherRules;
import io.github.hammerknife404.dreadpatch.world.Escalation;
import io.github.hammerknife404.dreadpatch.world.Dreadlands;
import io.github.hammerknife404.dreadpatch.world.DreadlandsData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
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
 * <li>{@code dreadlands enter|home [players]}: into the Dreadlands (where a gate would set them down), or home again;</li>
 * <li>{@code dreadlands arrival [x z]} (0.60.0): where a gate there would set someone down, and how far from a spire;</li>
 * <li>{@code escalation [storm [seconds]|reset]} (0.62.0): whether a queen's been defeated here (the Mausoleums stir) and the
 * defeat storm; a storm now; or all forgotten;</li>
 * <li>{@code config [setting [value]]} (0.58.0): every setting, one, or one set (and saved to the file);</li>
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
                                        .executes(context -> home(context, EntityArgument.getPlayers(context, "players")))))
                        .then(Commands.literal("arrival")
                                .executes(context -> arrival(context, BlockPos.containing(context.getSource().getPosition())))
                                .then(Commands.argument("pos", ColumnPosArgument.columnPos())
                                        .executes(context -> arrival(context, column(ColumnPosArgument.getColumnPos(context, "pos")))))))
                .then(Commands.literal("escalation")
                        .executes(DreadCommands::queryEscalation)
                        .then(Commands.literal("storm")
                                .executes(context -> defeatStorm(context, -1))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                        .executes(context -> defeatStorm(context, IntegerArgumentType.getInteger(context, "seconds")))))
                        .then(Commands.literal("reset").executes(DreadCommands::resetEscalation)))
                .then(Commands.literal("config")
                        .executes(DreadCommands::listConfig)
                        .then(Commands.argument("setting", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(DreadPatchConfig.values().keySet(), builder))
                                .executes(context -> showConfig(context, StringArgumentType.getString(context, "setting")))
                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                        .executes(context -> setConfig(context, StringArgumentType.getString(context, "setting"),
                                                StringArgumentType.getString(context, "value"))))))
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

    /** Whether a queen has been defeated in this world (so the Mausoleums stir), and the defeat storm: its time left, or when one's due. */
    private static int queryEscalation(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        DreadlandsData data = DreadlandsData.get(server);
        long left = Escalation.stormLeft(server);
        long wait = data.lastDefeatStorm() == 0L ? 0L
                : Math.max(0L, data.lastDefeatStorm() + DreadPatchConfig.defeatStormCooldownMillis() - System.currentTimeMillis());
        Component storm = left > 0 ? Component.translatable("commands.dreadpatch.escalation.storm_on", left / 20)
                : wait > 0 ? Component.translatable("commands.dreadpatch.escalation.storm_wait", String.format(java.util.Locale.ROOT, "%.1f", wait / 3_600_000.0))
                : Component.translatable("commands.dreadpatch.escalation.storm_due");
        context.getSource().sendSuccess(() -> Component.translatable("commands.dreadpatch.escalation.query",
                Component.translatable(data.queenFallen() ? "gui.yes" : "gui.no"), storm), false);
        return data.queenFallen() ? 1 : 0;
    }

    /** A defeat storm now, for {@code seconds} (-1: as a defeat brings, 30-60 s), whatever the cooldown. */
    private static int defeatStorm(CommandContext<CommandSourceStack> context, int seconds) {
        MinecraftServer server = context.getSource().getServer();
        int ticks = seconds > 0 ? seconds * 20 : EscalationRules.stormTicks(server.overworld().getRandom()::nextInt);
        Escalation.startStorm(server, ticks, System.currentTimeMillis());
        context.getSource().sendSuccess(() -> Component.translatable("commands.dreadpatch.escalation.storm", ticks / 20), true);
        return ticks / 20;
    }

    private static int resetEscalation(CommandContext<CommandSourceStack> context) {
        Escalation.reset(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.translatable("commands.dreadpatch.escalation.reset"), true);
        return 1;
    }

    private static int listConfig(CommandContext<CommandSourceStack> context) {
        DreadPatchConfig.values().forEach((name, setting) -> context.getSource().sendSuccess(
                () -> Component.translatable("commands.dreadpatch.config.value", name, String.valueOf(setting.get())), false));
        return DreadPatchConfig.values().size();
    }

    private static int showConfig(CommandContext<CommandSourceStack> context, String name) {
        ModConfigSpec.ConfigValue<?> setting = DreadPatchConfig.values().get(name);
        if (setting == null) {
            context.getSource().sendFailure(Component.translatable("commands.dreadpatch.config.unknown", name));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.dreadpatch.config.value", name, String.valueOf(setting.get())), false);
        return 1;
    }

    private static int setConfig(CommandContext<CommandSourceStack> context, String name, String value) {
        ModConfigSpec.ConfigValue<?> setting = DreadPatchConfig.values().get(name);
        if (setting == null) {
            context.getSource().sendFailure(Component.translatable("commands.dreadpatch.config.unknown", name));
            return 0;
        }
        if (!DreadPatchConfig.set(setting, value.trim())) {
            context.getSource().sendFailure(Component.translatable("commands.dreadpatch.config.invalid", name, value));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.dreadpatch.config.set", name, String.valueOf(setting.get())), true);
        return 1;
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

    /**
     * Where someone coming through a gate near {@code near} would arrive (0.60.0), and how far that is from the
     * nearest spire's middle: an arrival is random, so each call may differ. Generates the ground there.
     */
    private static int arrival(CommandContext<CommandSourceStack> context, BlockPos near) {
        MinecraftServer server = context.getSource().getServer();
        ServerLevel lands = server.getLevel(Dreadlands.LEVEL);
        Dreadlands.Arrival arrival = lands == null ? null : Dreadlands.arrival(server, near, lands.getRandom());
        if (arrival == null) {
            context.getSource().sendFailure(Component.translatable("commands.dreadpatch.dreadlands.missing"));
            return 0;
        }
        BlockPos at = BlockPos.containing(arrival.pos());
        BlockPos spire = lands.findNearestMapStructure(Dreadlands.SPIRES, at, 100, false);
        String distance = spire == null ? "-" : String.valueOf((int) Math.floor(Math.hypot((spire.getX() & ~15) + 8.5 - arrival.pos().x,
                (spire.getZ() & ~15) + 8.5 - arrival.pos().z)));
        context.getSource().sendSuccess(() -> Component.translatable("commands.dreadpatch.dreadlands.arrival",
                near.getX(), near.getZ(), at.getX(), at.getY(), at.getZ(), distance), false);
        return 1;
    }

    private static BlockPos column(net.minecraft.server.level.ColumnPos column) {
        return new BlockPos(column.x(), 64, column.z());
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
