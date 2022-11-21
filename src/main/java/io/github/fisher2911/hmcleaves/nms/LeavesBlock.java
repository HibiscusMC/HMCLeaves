/*
 *
 *  *     HMCLeaves
 *  *     Copyright (C) 2022  Hibiscus Creative Studios
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU General Public License
 *  *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.github.fisher2911.hmcleaves.nms;

import com.jeff_media.customblockdata.CustomBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.Bukkit;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class LeavesBlock extends net.minecraft.world.level.block.LeavesBlock implements SimpleWaterloggedBlock {

    protected static LeafDataSupplier leafDataSupplier;
    protected static PDCHelper pdcHelper;

    public LeavesBlock(Properties settings) {
        super(settings);
        this.registerDefaultState((BlockState) ((BlockState) ((BlockState) ((BlockState) this.stateDefinition.any())
                .setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE, 7))
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, false))
                .setValue(net.minecraft.world.level.block.LeavesBlock.WATERLOGGED, false));
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter world, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        // return true always because there is no way to access the position
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        final UUID worldUUID = world.getLevel().getWorld().getUID();
        final FakeLeafData data = this.getFakeDataAt(worldUUID, pos);
        if (data == null) return;
        if (this.decaying(state, data)) {
            // CraftBukkit start
            LeavesDecayEvent event = new LeavesDecayEvent(world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()));
            world.getCraftServer().getPluginManager().callEvent(event);

            if (event.isCancelled() || world.getBlockState(pos).getBlock() != this) {
                return;
            }
            // CraftBukkit end
            dropResources(state, world, pos);
            world.removeBlock(pos, false);
            leafDataSupplier.remove(worldUUID, pos.getX(), pos.getY(), pos.getZ());
        }

    }

    // override this to work with the fake leaf data
    protected boolean decaying(BlockState state, FakeLeafData data) {
        return !data.actualPersistence() && data.actualDistance() == 7;
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        world.setBlock(pos, updateDistance(state, world, pos), 3);
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return 1;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(net.minecraft.world.level.block.LeavesBlock.WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        final UUID worldUUID = world.getMinecraftWorld().getWorld().getUID();
        int i = getDistanceAt(neighborState, neighborPos, worldUUID) + 1;
        final FakeLeafData data = leafDataSupplier.getAtOrCreate(worldUUID, pos.getX(), pos.getY(), pos.getZ());

        if (i != 1 || data.actualDistance() != i) {
            world.scheduleTick(pos, this, 1);
        }

        return state;
    }

    private static BlockState updateDistance(BlockState state, LevelAccessor world, BlockPos pos) {
        int i = 7;
        BlockPos.MutableBlockPos blockposition_mutableblockposition = new BlockPos.MutableBlockPos();
        Direction[] aenumdirection = Direction.values();
        int j = aenumdirection.length;

        final UUID worldUUID = world.getMinecraftWorld().getWorld().getUID();
        final FakeLeafData data = leafDataSupplier.getAtOrCreate(worldUUID, pos.getX(), pos.getY(), pos.getZ());
        for (int k = 0; k < j; ++k) {
            Direction enumdirection = aenumdirection[k];

            blockposition_mutableblockposition.setWithOffset(pos, enumdirection);
            i = Math.min(i, getDistanceAt(world.getBlockState(blockposition_mutableblockposition), blockposition_mutableblockposition, worldUUID) + 1);
            if (i == 1) {
                break;
            }
        }

        final PersistentDataContainer blockData = new CustomBlockData(
                world.getMinecraftWorld().getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()),
                leafDataSupplier.plugin()
        );
        int previousDistance = data.actualDistance();
        data.actualDistance(i);
        pdcHelper.setDistance(blockData, (byte) data.fakeDistance());
        pdcHelper.setPersistent(blockData, (byte) (data.fakePersistence() ? 1 : 0));
        pdcHelper.setActualPersistent(blockData, (byte) (data.actualPersistence() ? 1 : 0));
        pdcHelper.setActualDistance(blockData, (byte) data.actualDistance());
        // make sure neighbors are updated properly
        if (previousDistance != i) {
            // if all the leaves around this one are the same, then we need to tick again because they will not cycle back to cause a tick
            Bukkit.getScheduler().runTaskLater(leafDataSupplier.plugin(), () -> {
                final BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
                for (int k = 0; k < j; ++k) {
                    Direction enumdirection = aenumdirection[k];
                    mutableBlockPos.setWithOffset(pos, enumdirection);
                    world.scheduleTick(mutableBlockPos, world.getBlockState(mutableBlockPos).getBlock(), 0);
                }
            }, 1);
            world.scheduleTick(pos, state.getBlock(), 1);
        } else {
            state = state.setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, data.fakePersistence());
        }

        return state.setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE, data.fakeDistance());
    }

    // override with our custom data
    private static int getDistanceAt(BlockState state, BlockPos checking, UUID world) {
        if (state.is(BlockTags.LOGS) || leafDataSupplier.isLogAt(world, checking.getX(), checking.getY(), checking.getZ())) {
            return 0;
        }
        final FakeLeafData data = leafDataSupplier.getAt(world, checking.getX(), checking.getY(), checking.getZ());
        return data == null ? 7 : data.actualDistance();
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(net.minecraft.world.level.block.LeavesBlock.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (world.isRainingAt(pos.above())) {
            if (random.nextInt(15) == 1) {
                BlockPos blockposition1 = pos.below();
                BlockState iblockdata1 = world.getBlockState(blockposition1);

                if (!iblockdata1.canOcclude() || !iblockdata1.isFaceSturdy(world, blockposition1, Direction.UP)) {
                    double d0 = (double) pos.getX() + random.nextDouble();
                    double d1 = (double) pos.getY() - 0.05D;
                    double d2 = (double) pos.getZ() + random.nextDouble();

                    world.addParticle(ParticleTypes.DRIPPING_WATER, d0, d1, d2, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
                net.minecraft.world.level.block.LeavesBlock.DISTANCE,
                net.minecraft.world.level.block.LeavesBlock.PERSISTENT,
                net.minecraft.world.level.block.LeavesBlock.WATERLOGGED
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        FluidState fluid = ctx.getLevel().getFluidState(ctx.getClickedPos());
        BlockState iblockdata = this.defaultBlockState().setValue(PERSISTENT, true).setValue(LeavesBlock.WATERLOGGED, fluid.getType() == Fluids.WATER);
        return updateDistance(iblockdata, ctx.getLevel(), ctx.getClickedPos());
    }

    @Nullable
    public FakeLeafData getFakeDataAt(UUID world, BlockPos pos) {
        return leafDataSupplier.getAt(world, pos.getX(), pos.getY(), pos.getZ());
    }

}
