package io.github.maxwelljones.tierworks;

import com.mojang.logging.LogUtils;
import io.github.maxwelljones.tierworks.registry.ModBlocks;
import io.github.maxwelljones.tierworks.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Tierworks.MOD_ID)
public final class Tierworks {
    public static final String MOD_ID = "tierworks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Tierworks(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        LOGGER.info("Tierworks initialized");
    }
}
