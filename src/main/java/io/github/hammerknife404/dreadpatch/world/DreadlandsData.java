package io.github.hammerknife404.dreadpatch.world;

import io.github.hammerknife404.dreadpatch.magic.WeatherRules;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What the Dreadlands remember, kept with the overworld's data ({@code data/dreadpatch_dreadlands.dat}):
 * where each player came in from, in front of the gate they stepped through, so a gate home takes
 * them back there; and when each player last met each spire's encounter (0.47.0), by real time, so it
 * comes to them once and again only after the server config's hours; and its weather (0.54.0): whether
 * this storm is a Whiteout Dominion, and whether the last one was.
 */
public class DreadlandsData extends SavedData {
    static final String NAME = "dreadpatch_dreadlands";

    /** Where a player came in from: the level, the spot in front of the gate, and facing away from it. */
    public record Origin(ResourceKey<Level> level, Vec3 pos, float yaw) {
    }

    private final Map<UUID, Origin> origins = new HashMap<>();
    /** Each spire's (by its base, packed) last encounter with each player, in epoch milliseconds. */
    private final Map<Long, Map<UUID, Long>> encounters = new HashMap<>();
    private final WeatherRules.Storms storms = new WeatherRules.Storms();
    /** Lanterns and torches placed in the Dreadlands still fading (0.57.0): position (packed) to the game time of its next step. */
    private final Map<Long, Long> fading = new HashMap<>();
    /** The overworld's escalation (0.62.0): whether a Dread Queen has ever been defeated, and the defeat storm's end (game time) and last start (real time). */
    private boolean queenFallen;
    private long defeatStormUntil;
    private long lastDefeatStorm;
    /** The overworld's dread weather (0.89.0): whether this rain is a dread storm, whether it reaches the hot lands. */
    private boolean dreadWeather;
    private boolean dreadWeatherHot;
    /** Snow layers it laid, to melt once it's over. */
    private final it.unimi.dsi.fastutil.longs.LongArrayList dreadSnow = new it.unimi.dsi.fastutil.longs.LongArrayList();

    public static DreadlandsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(DreadlandsData::new, DreadlandsData::load, null), NAME);
    }

    @Nullable
    public Origin origin(UUID player) {
        return this.origins.get(player);
    }

    public void setOrigin(UUID player, Origin origin) {
        this.origins.put(player, origin);
        this.setDirty();
    }

    /** When {@code player} last met the encounter at {@code spire}, or null if never. */
    @Nullable
    public Long lastEncounter(long spire, UUID player) {
        Map<UUID, Long> met = this.encounters.get(spire);
        return met == null ? null : met.get(player);
    }

    public void recordEncounter(long spire, UUID player, long when) {
        this.encounters.computeIfAbsent(spire, s -> new HashMap<>()).put(player, when);
        this.setDirty();
    }

    public Map<Long, Long> fading() {
        return this.fading;
    }

    /** Its weather's memory; {@link #setDirty} after changing it. */
    public boolean queenFallen() {
        return this.queenFallen;
    }

    public long defeatStormUntil() {
        return this.defeatStormUntil;
    }

    public long lastDefeatStorm() {
        return this.lastDefeatStorm;
    }

    public void setQueenFallen(boolean fallen) {
        this.queenFallen = fallen;
        this.setDirty();
    }

    /** A defeat storm until {@code until} (game time), started at {@code startedAt} (real time; 0 forgets it). */
    public void setDefeatStorm(long until, long startedAt) {
        this.defeatStormUntil = until;
        this.lastDefeatStorm = startedAt;
        this.setDirty();
    }

    public boolean dreadWeather() {
        return this.dreadWeather;
    }

    public boolean dreadWeatherHot() {
        return this.dreadWeatherHot;
    }

    public void setDreadWeather(boolean on, boolean hot) {
        this.dreadWeather = on;
        this.dreadWeatherHot = hot;
        this.setDirty();
    }

    /** The snow layers the dread weather laid (block positions), still to melt. */
    public it.unimi.dsi.fastutil.longs.LongArrayList dreadSnow() {
        return this.dreadSnow;
    }

    public WeatherRules.Storms storms() {
        return this.storms;
    }

    /** Forgets {@code player}'s meetings with every spire's encounter, or everyone's if null. How many were forgotten. */
    public int forgetEncounters(@Nullable UUID player) {
        int forgotten = 0;
        for (Map<UUID, Long> met : this.encounters.values()) {
            if (player == null) {
                forgotten += met.size();
                met.clear();
            } else if (met.remove(player) != null) {
                forgotten++;
            }
        }
        this.encounters.values().removeIf(Map::isEmpty);
        this.setDirty();
        return forgotten;
    }

    private static DreadlandsData load(CompoundTag tag, HolderLookup.Provider registries) {
        DreadlandsData data = new DreadlandsData();
        CompoundTag weather = tag.getCompound("Weather");
        data.storms.storming = weather.getBoolean("Storming");
        data.storms.whiteout = weather.getBoolean("Whiteout");
        data.storms.wasWhiteout = weather.getBoolean("WasWhiteout");
        data.storms.lastWhiteout = weather.getBoolean("LastWhiteout");
        CompoundTag escalation = tag.getCompound("Escalation");
        data.queenFallen = escalation.getBoolean("QueenFallen");
        data.defeatStormUntil = escalation.getLong("StormUntil");
        data.lastDefeatStorm = escalation.getLong("LastStorm");
        data.dreadWeather = escalation.getBoolean("DreadWeather");
        data.dreadWeatherHot = escalation.getBoolean("DreadWeatherHot");
        for (long pos : escalation.getLongArray("DreadSnow")) {
            data.dreadSnow.add(pos);
        }
        CompoundTag fading = tag.getCompound("Fading");
        for (String pos : fading.getAllKeys()) {
            try {
                data.fading.put(Long.parseLong(pos), fading.getLong(pos));
            } catch (NumberFormatException ignored) {
                // Not a position: skip it.
            }
        }
        CompoundTag origins = tag.getCompound("Origins");
        for (String key : origins.getAllKeys()) {
            CompoundTag o = origins.getCompound(key);
            ResourceLocation level = ResourceLocation.tryParse(o.getString("Level"));
            if (level == null) {
                continue;
            }
            try {
                data.origins.put(UUID.fromString(key), new Origin(ResourceKey.create(Registries.DIMENSION, level),
                        new Vec3(o.getDouble("X"), o.getDouble("Y"), o.getDouble("Z")), o.getFloat("Yaw")));
            } catch (IllegalArgumentException ignored) {
                // Not a player's UUID: skip it.
            }
        }
        CompoundTag encounters = tag.getCompound("Encounters");
        for (String spire : encounters.getAllKeys()) {
            CompoundTag met = encounters.getCompound(spire);
            try {
                Map<UUID, Long> players = new HashMap<>();
                for (String player : met.getAllKeys()) {
                    players.put(UUID.fromString(player), met.getLong(player));
                }
                data.encounters.put(Long.parseLong(spire), players);
            } catch (IllegalArgumentException ignored) {
                // Not a spire's or a player's: skip it.
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag origins = new CompoundTag();
        this.origins.forEach((player, origin) -> {
            CompoundTag o = new CompoundTag();
            o.putString("Level", origin.level().location().toString());
            o.putDouble("X", origin.pos().x);
            o.putDouble("Y", origin.pos().y);
            o.putDouble("Z", origin.pos().z);
            o.putFloat("Yaw", origin.yaw());
            origins.put(player.toString(), o);
        });
        tag.put("Origins", origins);
        CompoundTag encounters = new CompoundTag();
        this.encounters.forEach((spire, players) -> {
            CompoundTag met = new CompoundTag();
            players.forEach((player, when) -> met.putLong(player.toString(), when));
            encounters.put(Long.toString(spire), met);
        });
        tag.put("Encounters", encounters);
        CompoundTag weather = new CompoundTag();
        weather.putBoolean("Storming", this.storms.storming);
        weather.putBoolean("Whiteout", this.storms.whiteout);
        weather.putBoolean("WasWhiteout", this.storms.wasWhiteout);
        weather.putBoolean("LastWhiteout", this.storms.lastWhiteout);
        tag.put("Weather", weather);
        CompoundTag fading = new CompoundTag();
        this.fading.forEach((pos, due) -> fading.putLong(Long.toString(pos), due));
        tag.put("Fading", fading);
        CompoundTag escalation = new CompoundTag();
        escalation.putBoolean("QueenFallen", this.queenFallen);
        escalation.putLong("StormUntil", this.defeatStormUntil);
        escalation.putLong("LastStorm", this.lastDefeatStorm);
        escalation.putBoolean("DreadWeather", this.dreadWeather);
        escalation.putBoolean("DreadWeatherHot", this.dreadWeatherHot);
        escalation.putLongArray("DreadSnow", this.dreadSnow.toLongArray());
        tag.put("Escalation", escalation);
        return tag;
    }
}
