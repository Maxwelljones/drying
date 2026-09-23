package io.github.maxwelljones.tierworks.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Foundation block for the Tierworks drying rack.
 *
 * <p>The rack reserves a 2 x 2 footprint and two vertical blocks. One block
 * state is the master and the other seven states are structural parts. The
 * master is always local (0, 0, 0).</p>
 */
public final class DryingRackBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART_X = IntegerProperty.create("part_x", 0, 1);
    public static final IntegerProperty PART_Y = IntegerProperty.create("part_y", 0, 1);
    public static final IntegerProperty PART_Z = IntegerProperty.create("part_z", 0, 1);

    private static final VoxelShape LOWER_PART_SHAPE = box(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D);
    private static final VoxelShape UPPER_PART_SHAPE = box(0.0D, 0.0D, 6.0D, 16.0D, 2.0D, 10.0D);

    private static final ThreadLocal<Boolean> CLEARING_STRUCTURE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    public DryingRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART_X, 0)
                .setValue(PART_Y, 0)
                .setValue(PART_Z, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART_X, PART_Y, PART_Z);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        final Direction facing = context.getHorizontalDirection().getOpposite();
        final BlockPos masterPos = context.getClickedPos();

        if (!canPlaceStructure(context, masterPos, facing)) {
            return null;
        }

        return defaultBlockState().setValue(FACING, facing);
    }

    private boolean canPlaceStructure(BlockPlaceContext context, BlockPos masterPos, Direction facing) {
        final Level level = context.getLevel();

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                for (int z = 0; z < 2; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    final BlockPos partPos = partPos(masterPos, facing, x, y, z);
                    if (partPos.getY() < level.getMinBuildHeight()
                            || partPos.getY() >= level.getMaxBuildHeight()
                            || !level.getBlockState(partPos).canBeReplaced(context)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide() || !isMaster(state)) {
            return;
        }

        final Direction facing = state.getValue(FACING);

        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                for (int z = 0; z < 2; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    final BlockPos partPos = partPos(pos, facing, x, y, z);
                    final BlockState partState = defaultBlockState()
                            .setValue(FACING, facing)
                            .setValue(PART_X, x)
                            .setValue(PART_Y, y)
                            .setValue(PART_Z, z);

                    level.setBlock(partPos, partState, Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return isMaster(state) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return state.getValue(PART_Y) == 0 ? LOWER_PART_SHAPE : UPPER_PART_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return Shapes.empty();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return isMaster(state) ? List.of(new ItemStack(this)) : List.of();
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            final BlockPos masterPos = masterPos(pos, state);

            if (!masterPos.equals(pos)) {
                final BlockState masterState = level.getBlockState(masterPos);
                if (masterState.is(this) && isMaster(masterState)) {
                    level.destroyBlock(masterPos, !player.isCreative(), player);
                }
            }

            clearRemainingParts(level, masterPos, state.getValue(FACING));
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        if (!level.isClientSide()
                && !state.is(newState.getBlock())
                && !CLEARING_STRUCTURE.get()) {
            final BlockPos masterPos = masterPos(pos, state);

            if (!isMaster(state)) {
                final BlockState masterState = level.getBlockState(masterPos);
                if (masterState.is(this) && isMaster(masterState)) {
                    level.destroyBlock(masterPos, false);
                }
            }

            clearRemainingParts(level, masterPos, state.getValue(FACING));
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void clearRemainingParts(Level level, BlockPos masterPos, Direction facing) {
        if (CLEARING_STRUCTURE.get()) {
            return;
        }

        CLEARING_STRUCTURE.set(Boolean.TRUE);
        try {
            for (int y = 0; y < 2; y++) {
                for (int x = 0; x < 2; x++) {
                    for (int z = 0; z < 2; z++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }

                        final BlockPos partPos = partPos(masterPos, facing, x, y, z);

                        if (level.getBlockState(partPos).is(this)) {
                            level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                }
            }
        } finally {
            CLEARING_STRUCTURE.set(Boolean.FALSE);
        }
    }

    public static boolean isMaster(BlockState state) {
        return state.getValue(PART_X) == 0
                && state.getValue(PART_Y) == 0
                && state.getValue(PART_Z) == 0;
    }

    public static BlockPos masterPos(BlockPos partPos, BlockState state) {
        final Direction facing = state.getValue(FACING);
        final int x = state.getValue(PART_X);
        final int y = state.getValue(PART_Y);
        final int z = state.getValue(PART_Z);
        final Offset offset = offsetFor(facing, x, y, z);

        return partPos.offset(-offset.x(), -offset.y(), -offset.z());
    }

    public static BlockPos partPos(
            BlockPos masterPos,
            Direction facing,
            int x,
            int y,
            int z) {
        final Offset offset = offsetFor(facing, x, y, z);
        return masterPos.offset(offset.x(), offset.y(), offset.z());
    }

    private static Offset offsetFor(Direction facing, int x, int y, int z) {
        final Direction right = facing.getClockWise();
        final Direction back = facing.getOpposite();

        return new Offset(
                right.getStepX() * x + back.getStepX() * z,
                y,
                right.getStepZ() * x + back.getStepZ() * z);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    private record Offset(int x, int y, int z) {
    }
}
