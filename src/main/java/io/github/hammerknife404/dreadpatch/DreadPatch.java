package io.github.hammerknife404.dreadpatch;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(DreadPatch.MODID)
public final class DreadPatch {
    public static final String MODID = "dreadpatch";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DreadPatch(IEventBus modBus, ModContainer container) {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
