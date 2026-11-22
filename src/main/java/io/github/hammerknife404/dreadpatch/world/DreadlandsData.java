package io.github.hammerknife404.dreadpatch.world;

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
 * comes to them once and again only after the server config's hours.
 */
public class DreadlandsData extends SavedData {
    static final String NAME = "dreadpatch_dreadlands";

    /** Where a player came in from: the level, the spot in front of the gate, and facing away from it. */
    public record Origin(ResourceKey<Level> level, Vec3 pos, float yaw) {
    }

    private final Map<UUID, Origin> origins = new HashMap<>();
    /** Each spire's (by its base, packed) last encounter with each player, in epoch milliseconds. */
    private final Map<Long, Map<UUID, Long>> encounters = new HashMap<>();

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

    private static DreadlandsData load(CompoundTag tag, HolderLookup.Provider registries) {
        DreadlandsData data = new DreadlandsData();
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
        return tag;
    }
}
