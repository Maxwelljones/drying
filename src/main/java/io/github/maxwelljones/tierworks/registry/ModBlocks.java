package io.github.maxwelljones.tierworks.registry;

import io.github.maxwelljones.tierworks.Tierworks;
import io.github.maxwelljones.tierworks.block.DryingRackBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Tierworks.MOD_ID);

    public static final DeferredBlock<DryingRackBlock> DRYING_RACK = BLOCKS.register(
            "drying_rack",
            () -> new DryingRackBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
