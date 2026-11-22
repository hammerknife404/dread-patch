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
 * them back there.
 */
public class DreadlandsData extends SavedData {
    static final String NAME = "dreadpatch_dreadlands";

    /** Where a player came in from: the level, the spot in front of the gate, and facing away from it. */
    public record Origin(ResourceKey<Level> level, Vec3 pos, float yaw) {
    }

    private final Map<UUID, Origin> origins = new HashMap<>();

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
        return tag;
    }
}
