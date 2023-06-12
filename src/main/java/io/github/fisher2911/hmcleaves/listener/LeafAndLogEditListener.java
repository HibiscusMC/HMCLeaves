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
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.data.LeafData;
import io.github.fisher2911.hmcleaves.data.LogData;
import io.github.fisher2911.hmcleaves.packet.PacketUtils;
import io.github.fisher2911.hmcleaves.util.Pair;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.LeavesDecayEvent;

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
        final Position position = Position.fromLocation(event.getBlock().getLocation());
        // delay so that saplings can be replaced
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            this.blockCache.removeBlockData(position);
            PacketUtils.sendBlock(Material.AIR, position, Bukkit.getOnlinePlayers());
        }, 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPistonPush(BlockPistonExtendEvent event) {
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
            final Position originalPosition = Position.fromLocation(block.getLocation());
            final Position newPosition = Position.fromLocation(block.getRelative(direction).getLocation());
            final BlockData blockData = this.blockCache.getBlockData(originalPosition);
            // leaves get destroyed by pistons
            if (blockData == BlockData.EMPTY || blockData instanceof LeafData) continue;
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
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            for (var entry : adjacent.entries()) {
                final Pair<Position, BlockData> pair = entry.getValue();
                PacketUtils.sendBlockData(pair.getSecond(), pair.getFirst(), Bukkit.getOnlinePlayers());
            }
        }, 1);
    }

    //
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPistonRetract(BlockPistonRetractEvent event) {
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
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            for (var entry : adjacent.entries()) {
                final Pair<Position, BlockData> pair = entry.getValue();
                PacketUtils.sendBlockData(pair.getSecond(), pair.getFirst(), Bukkit.getOnlinePlayers());
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

    private Multimap<Position, Pair<Position, BlockData>> getAdjacentBlockData(List<Block> blocks) {
        final Multimap<Position, Pair<Position, BlockData>> adjacent = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
        for (Block block : blocks) {
            final Position position = Position.fromLocation(block.getLocation());
            for (BlockFace face : ADJACENT_FACES) {
                final Position relativePosition = Position.fromLocation(block.getRelative(face).getLocation());
                if (blocks.contains(relativePosition.toLocation().getBlock())) continue;
                final BlockData blockData = this.blockCache.getBlockData(relativePosition);
                if (blockData == BlockData.EMPTY) continue;
                adjacent.put(position, Pair.of(relativePosition, blockData));
            }
        }
        return adjacent;
    }

//    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
//    public void onBlockUpdate(BlockPhysicsEvent event) {
//        final Block block = event.getBlock();
//        final BlockData blockData = this.blockCache.getBlockData(Position.fromLocation(block.getLocation()));
//        if (blockData == BlockData.EMPTY) return;
//        if (!(blockData instanceof LogData)) return;
//        if (event.getSourceBlock().getType() == Material.MOVING_PISTON) {
//            event.setCancelled(true);
//            Bukkit.broadcastMessage("canceled: " + event.getBlock().getLocation());
//        }
//    }

}
