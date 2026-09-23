package io.github.maxwelljones.tierworks.registry;

import io.github.maxwelljones.tierworks.Tierworks;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Tierworks.MOD_ID);

    public static final DeferredItem<BlockItem> DRYING_RACK =
            ITEMS.registerSimpleBlockItem("drying_rack", ModBlocks.DRYING_RACK);

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
