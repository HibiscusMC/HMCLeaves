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

package io.github.fisher2911.hmcleaves.listener;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.cache.BlockCache;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.data.AgeableData;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.data.CaveVineData;
import io.github.fisher2911.hmcleaves.data.LeafData;
import io.github.fisher2911.hmcleaves.data.LogData;
import io.github.fisher2911.hmcleaves.data.SaplingData;
import io.github.fisher2911.hmcleaves.hook.Hooks;
import io.github.fisher2911.hmcleaves.packet.PacketUtils;
import io.github.fisher2911.hmcleaves.util.ChainedBlockUtil;
import io.github.fisher2911.hmcleaves.util.LeafDropUtil;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.StructureGrowEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LeafAndLogEditListener implements Listener {

    private final HMCLeaves plugin;
    private final BlockCache blockCache;
    private final LeavesConfig leavesConfig;

    public LeafAndLogEditListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.blockCache = plugin.getBlockCache();
        this.leavesConfig = plugin.getLeavesConfig();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLeafDecay(LeavesDecayEvent event) {
        final Block block = event.getBlock();
        if (!this.leavesConfig.isWorldWhitelisted(block.getWorld())) return;
        final Position position = Position.fromLocation(block.getLocation());
        ChainedBlockUtil.handleBlockBreak(block, this.blockCache, this.leavesConfig);
        // delay so that saplings can be replaced
        final BlockData blockData = this.blockCache.removeBlockData(position);
        if (blockData == BlockData.EMPTY) return;
        LeafDropUtil.addToDropPositions(this.blockCache, position, blockData);
//        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
//            PacketUtils.sendBlock(Material.AIR, position, Bukkit.getOnlinePlayers());
//        }, 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPistonPush(BlockPistonExtendEvent event) {
        if (!this.leavesConfig.isWorldWhitelisted(event.getBlock().getWorld())) return;
        final BlockFace direction = event.getDirection();
        final List<Block> copyList = new ArrayList<>(event.getBlocks());
        // sort from furthest to closest to piston so that blocks don't get replaced in the cache
        final Location pistonLocation = event.getBlock().getLocation();
        copyList.sort((o1, o2) -> {
            final double o1Distance = o1.getLocation().distanceSquared(pistonLocation);
            final double o2Distance = o2.getLocation().distanceSquared(pistonLocation);
            return Double.compare(o2Distance, o1Distance);
        });
        final List<Block> checkBlocks = new ArrayList<>(copyList);
        if (!copyList.isEmpty()) {
            checkBlocks.add(copyList.get(0).getRelative(direction));
        } else {
            checkBlocks.add(event.getBlock().getRelative(direction));
        }
        checkBlocks.add(event.getBlock());
        final var adjacent = this.getAdjacentBlockData(checkBlocks);
        final List<Runnable> runnables = new ArrayList<>();
        for (Block block : copyList) {
            ChainedBlockUtil.handleBlockBreak(block, this.blockCache, this.leavesConfig);
            final Position originalPosition = Position.fromLocation(block.getLocation());
            final Position newPosition = Position.fromLocation(block.getRelative(direction).getLocation());
            final BlockData blockData = this.blockCache.getBlockData(originalPosition);
            // leaves get destroyed by pistons
            if (blockData == BlockData.EMPTY) continue;
            if (blockData instanceof LeafData) {
                runnables.add(() -> {
                    this.blockCache.removeBlockData(originalPosition);
                    LeafDropUtil.addToDropPositions(this.blockCache, originalPosition, blockData);
                });
                continue;
            }
            if (!(blockData instanceof final LogData logData)) continue;
            if (blockData.equals(this.leavesConfig.getDefaultLogData(logData.realBlockType(), logData.axis()))) {
                runnables.add(() -> {
                    this.blockCache.addBlockData(newPosition, blockData);
                    this.blockCache.removeBlockData(originalPosition);
                });
                continue;
            }
            event.setCancelled(true);
            return;
        }
        runnables.forEach(Runnable::run);
        if (adjacent.isEmpty()) {
            return;
        }
        Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, () -> {
            for (var entry : adjacent.entries()) {
                final AdjacentInfo adjacentInfo = entry.getValue();
                PacketUtils.sendBlockData(
                        adjacentInfo.blockData(),
                        adjacentInfo.relativePosition(),
                        adjacentInfo.realWorldMaterial(),
                        Bukkit.getOnlinePlayers()
                );
            }
        }, 1);
    }

    //
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!this.leavesConfig.isWorldWhitelisted(event.getBlock().getWorld())) return;
        final BlockFace direction = event.getDirection();
        final List<Block> copyList = new ArrayList<>(event.getBlocks());
        // sort from closest to furthest to piston so that blocks don't get replaced in the cache
        final Location pistonLocation = event.getBlock().getLocation();
        copyList.sort((o1, o2) -> {
            final double o1Distance = o1.getLocation().distanceSquared(pistonLocation);
            final double o2Distance = o2.getLocation().distanceSquared(pistonLocation);
            return Double.compare(o1Distance, o2Distance);
        });
        final List<Runnable> runnables = new ArrayList<>();
        final List<Block> checkBlocks = new ArrayList<>(copyList);
        checkBlocks.add(event.getBlock());
        checkBlocks.add(event.getBlock().getRelative(direction.getOppositeFace()));
        final var adjacent = this.getAdjacentBlockData(checkBlocks);
        for (Block block : copyList) {
            ChainedBlockUtil.handleBlockBreak(block, this.blockCache, this.leavesConfig);
            final Position originalPosition = Position.fromLocation(block.getLocation());
            final Position newPosition = Position.fromLocation(block.getRelative(direction).getLocation());
            final BlockData blockData = this.blockCache.getBlockData(originalPosition);
            if (blockData == BlockData.EMPTY) continue;
            if (blockData instanceof LeafData) {
                event.setCancelled(true);
                return;
            }
            if (!(blockData instanceof final LogData logData)) continue;
            if (blockData.equals(this.leavesConfig.getDefaultLogData(logData.realBlockType(), logData.axis()))) {
                runnables.add(() -> {
                    this.blockCache.removeBlockData(originalPosition);
                    this.blockCache.addBlockData(newPosition, blockData);
                });
                continue;
            }
            event.setCancelled(true);
            return;
        }
        runnables.forEach(Runnable::run);
        if (adjacent.isEmpty()) {
            return;
        }
        Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, () -> {
            for (var entry : adjacent.entries()) {
                final AdjacentInfo adjacentInfo = entry.getValue();
                PacketUtils.sendBlockData(
                        adjacentInfo.blockData(),
                        adjacentInfo.relativePosition(),
                        adjacentInfo.realWorldMaterial(),
                        Bukkit.getOnlinePlayers()
                );
            }
        }, 1);
    }

    private static final Set<BlockFace> ADJACENT_FACES = Set.of(
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    );

    private Multimap<Position, AdjacentInfo> getAdjacentBlockData(List<Block> blocks) {
        final Multimap<Position, AdjacentInfo> adjacent = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
        for (Block block : blocks) {
            final Position position = Position.fromLocation(block.getLocation());
            for (BlockFace face : ADJACENT_FACES) {
                final Position relativePosition = Position.fromLocation(block.getRelative(face).getLocation());
                if (blocks.contains(relativePosition.toLocation().getBlock())) continue;
                final BlockData blockData = this.blockCache.getBlockData(relativePosition);
                if (blockData == BlockData.EMPTY) continue;
                adjacent.put(position, new AdjacentInfo(relativePosition, blockData, block.getRelative(face).getType()));
            }
        }
        return adjacent;
    }

    private static record AdjacentInfo(Position relativePosition, BlockData blockData, Material realWorldMaterial) {

    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onTreeGrow(StructureGrowEvent event) {
        if (!this.leavesConfig.isWorldWhitelisted(event.getWorld())) return;
        if (event.getBlocks().size() == 1) {
            return;
        }
        final Position sourcePosition = Position.fromLocation(event.getLocation());
        final BlockData sourceBlockData = this.blockCache.getBlockData(sourcePosition);
        if (sourceBlockData instanceof final SaplingData saplingData) {
            if (!saplingData.schematicFiles().isEmpty()) {
                event.setCancelled(true);
                Hooks.pasteSaplingSchematic(saplingData, sourcePosition);
                return;
            }
            this.blockCache.removeBlockData(sourcePosition);
        }
        for (BlockState blockState : event.getBlocks()) {
            final Position position = Position.fromLocation(blockState.getLocation());
            final BlockData blockData;
            if (Tag.LOGS.isTagged(blockState.getType())) {
                final Orientable orientable = (Orientable) blockState.getBlockData();
                blockData = this.leavesConfig.getDefaultLogData(blockState.getType(), orientable.getAxis());
            } else if (Tag.LEAVES.isTagged(blockState.getType())) {
                blockData = this.leavesConfig.getDefaultLeafData(blockState.getType());
            } else {
                continue;
            }
            this.blockCache.addBlockData(position, blockData);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBurn(BlockBurnEvent event) {
        final Block block = event.getBlock();
        if (!this.leavesConfig.isWorldWhitelisted(block.getWorld())) return;
        final Position position = Position.fromLocation(block.getLocation());
        final BlockData blockData = this.blockCache.removeBlockData(position);
        ChainedBlockUtil.handleBlockBreak(block, this.blockCache, this.leavesConfig);
        if (blockData == BlockData.EMPTY) return;
        LeafDropUtil.addToDropPositions(this.blockCache, position, blockData);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onWaterFlow(BlockFromToEvent event) {
        final Block block = event.getToBlock();
        if (!this.leavesConfig.isWorldWhitelisted(block.getWorld())) return;
        final Position position = Position.fromLocation(block.getLocation());
        final BlockData blockData = this.blockCache.getBlockData(position);
        ChainedBlockUtil.handleBlockBreak(block, this.blockCache, this.leavesConfig);
//        if (!(blockData instanceof SaplingData)) return;
        this.blockCache.removeBlockData(position);
        if (blockData == BlockData.EMPTY) return;
        LeafDropUtil.addToDropPositions(this.blockCache, position, blockData);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockGrow(BlockSpreadEvent event) {
        final Block block = event.getSource();
        if (!this.leavesConfig.isWorldWhitelisted(block.getWorld())) return;
        final Position oldPosition = Position.fromLocation(block.getLocation());
        final BlockData oldBlockData = this.blockCache.getBlockData(oldPosition);
        final BlockState newState = event.getNewState();
        final Position position = Position.fromLocation(newState.getLocation());
        if (block.getBlockData() instanceof final CaveVinesPlant caveVines) {
            final CaveVineData blockData;
            if (oldBlockData instanceof final CaveVineData caveVineData) {
                blockData = caveVineData;
            } else {
                blockData = (CaveVineData) this.leavesConfig.getDefaultCaveVinesData(caveVines.isBerries());
            }
            this.blockCache.addBlockData(position, blockData.withGlowBerry(caveVines.isBerries()));
            return;
        }
        if (LeavesConfig.AGEABLE_MATERIALS.contains(block.getType())) {
            final AgeableData blockData;
            if (oldBlockData instanceof final AgeableData ageableData) {
                blockData = ageableData;
            } else {
                blockData = (AgeableData) this.leavesConfig.getDefaultAgeableData(block.getType());
            }
            this.blockCache.addBlockData(position, blockData);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockGrow(BlockGrowEvent event) {
        final Block block = event.getBlock();
        if (!this.leavesConfig.isWorldWhitelisted(block.getWorld())) return;
        final BlockState newState = event.getNewState();
        final Position position = Position.fromLocation(newState.getLocation());
        final Material material = newState.getType();
        if (!LeavesConfig.AGEABLE_MATERIALS.contains(material)) {
            return;
        }
        final BlockData blockData;
        if (material != Material.SUGAR_CANE) return;
        final Position below = position.subtract(0, 1, 0);
        final BlockData belowBlockData = this.blockCache.getBlockData(below);
        if (belowBlockData instanceof final AgeableData ageableData && ageableData.realBlockType() == Material.SUGAR_CANE) {
            blockData = ageableData;
        } else {
            blockData = this.leavesConfig.getDefaultAgeableData(material);
        }
        this.blockCache.addBlockData(position, blockData);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onExplode(BlockExplodeEvent event) {
        if (!this.leavesConfig.isWorldWhitelisted(event.getBlock().getWorld())) return;
        event.blockList().forEach(b -> ChainedBlockUtil.handleBlockBreak(b, this.blockCache, this.leavesConfig));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onExplode(EntityExplodeEvent event) {
        if (!this.leavesConfig.isWorldWhitelisted(event.getEntity().getWorld())) return;
        event.blockList().forEach(b -> ChainedBlockUtil.handleBlockBreak(b, this.blockCache, this.leavesConfig));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockFertilize(BlockFertilizeEvent event) {
        final Block block = event.getBlock();
        if (!this.leavesConfig.isWorldWhitelisted(block.getWorld())) return;
        if (!(block.getBlockData() instanceof CaveVinesPlant)) return;
        final Position position = Position.fromLocation(block.getLocation());
        final BlockData blockData = this.blockCache.getBlockData(position);
        if (!(blockData instanceof final CaveVineData data)) return;
        if (!data.shouldGrowBerries()) {
            event.setCancelled(true);
            return;
        }
        this.blockCache.addBlockData(position, data.withGlowBerry(true));
    }

}
