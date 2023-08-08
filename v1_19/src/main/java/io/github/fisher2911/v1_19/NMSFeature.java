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

package io.github.fisher2911.v1_19;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import io.github.fisher2911.hmcleaves.cache.BlockCache;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.world.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.bukkit.craftbukkit.v1_19_R1.block.data.CraftBlockData;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;

public class NMSFeature extends Feature<TreeConfiguration> {
    private static final int BLOCK_UPDATE_FLAGS = 19;

    private final LeavesConfig leavesConfig;
    private final BlockCache blockCache;

    public NMSFeature(Codec<TreeConfiguration> configCodec, LeavesConfig leavesConfig, BlockCache blockCache) {
        super(configCodec);
        this.leavesConfig = leavesConfig;
        this.blockCache = blockCache;
    }

    private static boolean isVine(LevelSimulatedReader world, BlockPos pos) {
        return world.isStateAtPosition(pos, (state) -> {
            return state.is(Blocks.VINE);
        });
    }

    public static boolean isBlockWater(LevelSimulatedReader world, BlockPos pos) {
        return world.isStateAtPosition(pos, (state) -> {
            return state.is(Blocks.WATER);
        });
    }

    public static boolean isAirOrLeaves(LevelSimulatedReader world, BlockPos pos) {
        return world.isStateAtPosition(pos, (state) -> {
            return state.isAir() || state.is(BlockTags.LEAVES);
        });
    }

    private static boolean isReplaceablePlant(LevelSimulatedReader world, BlockPos pos) {
        return world.isStateAtPosition(pos, (state) -> {
            Material material = state.getMaterial();
            return material == Material.REPLACEABLE_PLANT || material == Material.REPLACEABLE_WATER_PLANT || material == Material.REPLACEABLE_FIREPROOF_PLANT;
        });
    }

    private static void setBlockKnownShape(LevelWriter world, BlockPos pos, BlockState state) {
        world.setBlock(pos, state, 19);
    }

    public static boolean validTreePos(LevelSimulatedReader world, BlockPos pos) {
        return isAirOrLeaves(world, pos) || isReplaceablePlant(world, pos) || isBlockWater(world, pos);
    }

    private boolean doPlace(WorldGenLevel world, RandomSource random, BlockPos pos, BiConsumer<BlockPos, BlockState> rootPlacerReplacer, BiConsumer<BlockPos, BlockState> trunkPlacerReplacer, BiConsumer<BlockPos, BlockState> foliagePlacerReplacer, TreeConfiguration config) {
        int i = config.trunkPlacer.getTreeHeight(random);
        int j = config.foliagePlacer.foliageHeight(random, i, config);
        int k = i - j;
        int l = config.foliagePlacer.foliageRadius(random, k);
        BlockPos blockPos = config.rootPlacer.map((rootPlacer) -> {
            return rootPlacer.getTrunkOrigin(pos, random);
        }).orElse(pos);
        int m = Math.min(pos.getY(), blockPos.getY());
        int n = Math.max(pos.getY(), blockPos.getY()) + i + 1;
        if (m >= world.getMinBuildHeight() + 1 && n <= world.getMaxBuildHeight()) {
            OptionalInt optionalInt = config.minimumSize.minClippedHeight();
            int o = this.getMaxFreeTreeHeight(world, i, blockPos, config);
            if (o >= i || !optionalInt.isEmpty() && o >= optionalInt.getAsInt()) {
                if (config.rootPlacer.isPresent() && !config.rootPlacer.get().placeRoots(world, rootPlacerReplacer, random, pos, blockPos, config)) {
                    return false;
                } else {
                    List<FoliagePlacer.FoliageAttachment> list = config.trunkPlacer.placeTrunk(world, trunkPlacerReplacer, random, o, blockPos, config);
                    list.forEach((node) -> {
                        config.foliagePlacer.createFoliage(world, foliagePlacerReplacer, random, config, o, node, j, l);
                    });
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private int getMaxFreeTreeHeight(LevelSimulatedReader world, int height, BlockPos pos, TreeConfiguration config) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for(int i = 0; i <= height + 1; ++i) {
            int j = config.minimumSize.getSizeAtHeight(height, i);

            for(int k = -j; k <= j; ++k) {
                for(int l = -j; l <= j; ++l) {
                    mutableBlockPos.setWithOffset(pos, k, i, l);
                    if (!config.trunkPlacer.isFree(world, mutableBlockPos) || !config.ignoreVines && isVine(world, mutableBlockPos)) {
                        return i - 2;
                    }
                }
            }
        }

        return height;
    }

    @Override
    protected void setBlock(LevelWriter world, BlockPos pos, BlockState state) {
        setBlockKnownShape(world, pos, state);
    }

    @Override
    public final boolean place(FeaturePlaceContext<TreeConfiguration> context) {
        WorldGenLevel worldGenLevel = context.level();
        RandomSource randomSource = context.random();
        BlockPos blockPos = context.origin();
        TreeConfiguration treeConfiguration = context.config();
        Set<BlockPos> set = Sets.newHashSet();
        Set<BlockPos> set2 = Sets.newHashSet();
        Set<BlockPos> set3 = Sets.newHashSet();
        Set<BlockPos> set4 = Sets.newHashSet();
        BiConsumer<BlockPos, BlockState> biConsumer = (pos, state) -> {
            set.add(pos.immutable());
            final org.bukkit.block.data.BlockData craftData = state.createCraftBlockData();
            final BlockData blockData = this.leavesConfig.getByBukkitBlockData(craftData);
            if (blockData == null) {
                worldGenLevel.setBlock(pos, state, 19);
                return;
            }
            final Position position = new Position(worldGenLevel.getMinecraftWorld().uuid, pos.getX(), pos.getY(), pos.getZ());
            this.blockCache.addBlockData(position, blockData);
            final BlockState actualState = ((CraftBlockData) blockData.worldBlockType().createBlockData()).getState();
            worldGenLevel.setBlock(pos, actualState, 19);
        };
        BiConsumer<BlockPos, BlockState> biConsumer2 = (pos, state) -> {
            set2.add(pos.immutable());
            final org.bukkit.block.data.BlockData craftData = state.createCraftBlockData();
            final BlockData blockData = this.leavesConfig.getByBukkitBlockData(craftData);
            if (blockData == null) {
                worldGenLevel.setBlock(pos, state, 19);
                return;
            }
            final Position position = new Position(worldGenLevel.getMinecraftWorld().uuid, pos.getX(), pos.getY(), pos.getZ());
            this.blockCache.addBlockData(position, blockData);
            final BlockState actualState = ((CraftBlockData) blockData.worldBlockType().createBlockData()).getState();
            worldGenLevel.setBlock(pos, actualState, 19);
        };
        BiConsumer<BlockPos, BlockState> biConsumer3 = (pos, state) -> {
            set3.add(pos.immutable());
            final org.bukkit.block.data.BlockData craftData = state.createCraftBlockData();
            final BlockData blockData = this.leavesConfig.getByBukkitBlockData(craftData);
            if (blockData == null) {
                worldGenLevel.setBlock(pos, state, 19);
                return;
            }
            final Position position = new Position(worldGenLevel.getMinecraftWorld().uuid, pos.getX(), pos.getY(), pos.getZ());
            this.blockCache.addBlockData(position, blockData);
            final BlockState actualState = ((CraftBlockData) blockData.worldBlockType().createBlockData()).getState();
            worldGenLevel.setBlock(pos, actualState, 19);
        };
        BiConsumer<BlockPos, BlockState> biConsumer4 = (pos, state) -> {
            set4.add(pos.immutable());
            final org.bukkit.block.data.BlockData craftData = state.createCraftBlockData();
            final BlockData blockData = this.leavesConfig.getByBukkitBlockData(craftData);
            if (blockData == null) {
                worldGenLevel.setBlock(pos, state, 19);
                return;
            }
            final Position position = new Position(worldGenLevel.getMinecraftWorld().uuid, pos.getX(), pos.getY(), pos.getZ());
            this.blockCache.addBlockData(position, blockData);
            final BlockState actualState = ((CraftBlockData) blockData.worldBlockType().createBlockData()).getState();
            worldGenLevel.setBlock(pos, actualState, 19);
        };
        boolean bl = this.doPlace(worldGenLevel, randomSource, blockPos, biConsumer, biConsumer2, biConsumer3, treeConfiguration);
        if (bl && (!set2.isEmpty() || !set3.isEmpty())) {
            if (!treeConfiguration.decorators.isEmpty()) {
                TreeDecorator.Context context2 = new TreeDecorator.Context(worldGenLevel, biConsumer4, randomSource, set2, set3, set);
                treeConfiguration.decorators.forEach((decorator) -> {
                    decorator.place(context2);
                });
            }

            return BoundingBox.encapsulatingPositions(Iterables.concat(set, set2, set3, set4)).map((boundingBox) -> {
                DiscreteVoxelShape discreteVoxelShape = updateLeaves(worldGenLevel, boundingBox, set2, set4, set);
                StructureTemplate.updateShapeAtEdge(worldGenLevel, 3, discreteVoxelShape, boundingBox.minX(), boundingBox.minY(), boundingBox.minZ());
                return true;
            }).orElse(false);
        } else {
            return false;
        }
    }

    private static DiscreteVoxelShape updateLeaves(LevelAccessor world, BoundingBox box, Set<BlockPos> trunkPositions, Set<BlockPos> decorationPositions, Set<BlockPos> set) {
        List<Set<BlockPos>> list = Lists.newArrayList();
        DiscreteVoxelShape discreteVoxelShape = new BitSetDiscreteVoxelShape(box.getXSpan(), box.getYSpan(), box.getZSpan());
        int i = 6;

        for(int j = 0; j < 6; ++j) {
            list.add(Sets.newHashSet());
        }

        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for(BlockPos blockPos : Lists.newArrayList(Sets.union(decorationPositions, set))) {
            if (box.isInside(blockPos)) {
                discreteVoxelShape.fill(blockPos.getX() - box.minX(), blockPos.getY() - box.minY(), blockPos.getZ() - box.minZ());
            }
        }

        for(BlockPos blockPos2 : Lists.newArrayList(trunkPositions)) {
            if (box.isInside(blockPos2)) {
                discreteVoxelShape.fill(blockPos2.getX() - box.minX(), blockPos2.getY() - box.minY(), blockPos2.getZ() - box.minZ());
            }

            for(Direction direction : Direction.values()) {
                mutableBlockPos.setWithOffset(blockPos2, direction);
                if (!trunkPositions.contains(mutableBlockPos)) {
                    BlockState blockState = world.getBlockState(mutableBlockPos);
                    if (blockState.hasProperty(BlockStateProperties.DISTANCE)) {
                        list.get(0).add(mutableBlockPos.immutable());
                        setBlockKnownShape(world, mutableBlockPos, blockState.setValue(BlockStateProperties.DISTANCE, Integer.valueOf(1)));
                        if (box.isInside(mutableBlockPos)) {
                            discreteVoxelShape.fill(mutableBlockPos.getX() - box.minX(), mutableBlockPos.getY() - box.minY(), mutableBlockPos.getZ() - box.minZ());
                        }
                    }
                }
            }
        }

        for(int k = 1; k < 6; ++k) {
            Set<BlockPos> set2 = list.get(k - 1);
            Set<BlockPos> set3 = list.get(k);

            for(BlockPos blockPos3 : set2) {
                if (box.isInside(blockPos3)) {
                    discreteVoxelShape.fill(blockPos3.getX() - box.minX(), blockPos3.getY() - box.minY(), blockPos3.getZ() - box.minZ());
                }

                for(Direction direction2 : Direction.values()) {
                    mutableBlockPos.setWithOffset(blockPos3, direction2);
                    if (!set2.contains(mutableBlockPos) && !set3.contains(mutableBlockPos)) {
                        BlockState blockState2 = world.getBlockState(mutableBlockPos);
                        if (blockState2.hasProperty(BlockStateProperties.DISTANCE)) {
                            int l = blockState2.getValue(BlockStateProperties.DISTANCE);
                            if (l > k + 1) {
                                BlockState blockState3 = blockState2.setValue(BlockStateProperties.DISTANCE, Integer.valueOf(k + 1));
                                setBlockKnownShape(world, mutableBlockPos, blockState3);
                                if (box.isInside(mutableBlockPos)) {
                                    discreteVoxelShape.fill(mutableBlockPos.getX() - box.minX(), mutableBlockPos.getY() - box.minY(), mutableBlockPos.getZ() - box.minZ());
                                }

                                set3.add(mutableBlockPos.immutable());
                            }
                        }
                    }
                }
            }
        }

        return discreteVoxelShape;
    }
}