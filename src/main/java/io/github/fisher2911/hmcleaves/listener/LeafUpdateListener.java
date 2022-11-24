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

import io.github.fisher2911.hmcleaves.Config;
import io.github.fisher2911.hmcleaves.FakeLeafState;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafCache;
import io.github.fisher2911.hmcleaves.util.LeafUpdater3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.world.StructureGrowEvent;

public class LeafUpdateListener implements Listener {

    private final HMCLeaves plugin;
    private final LeafCache leafCache;
    private final Config config;

    public LeafUpdateListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.leafCache = this.plugin.getLeafCache();
        this.config = this.plugin.config();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysicsUpdate(BlockPhysicsEvent event) {
        final Block block = event.getBlock();
        /* previous the code below with paper, not sure if it changes anything yet */
        /* checkBlock(event, block, event.getChangedBlockData()); */

        checkBlock(event, block, block.getBlockData());

//        final Location location = block.getLocation();
////        if (Tag.LEAVES.isTagged(block.getType()) || this.leafCache.isLogAt(location)) {
////            LeafUpdater3.scheduleTick(location);
////            return;
////        }
////
////        if (this.leafCache.isLogAt(location) || this.config.isLogBlock(block.getBlockData())) {
////            LeafUpdater3.scheduleTick(location);
////            return;
////        }
//
//        final BlockData blockData = block.getBlockData();
//
////        event.getChangedBlockData()
//
//        if (this.leafCache.isLogAt(location)) {
//            if (this.config.isLogBlock(blockData)) return;
////            this.leafCache.removeLogAt(location);
////            removedLogs.add(location);
//            LeafUpdater3.scheduleTick(location);
//            return;
//        }
//        if (this.config.isLogBlock(blockData)) {
////                if (leafCache.isLogAt(location)) continue;
////            this.leafCache.setLogAt(location);
////            toUpdate.put(location, blockData);
////            addedLogs.add(location);
//            LeafUpdater3.scheduleTick(location);
//            return;
//        }
//        final FakeLeafState fakeLeafState = this.leafCache.getAt(location);
//        final Material material = block.getType();
//        if (fakeLeafState == null) {
//            if (!Tag.LEAVES.isTagged(material)) return;
//            final FakeLeafState newState = this.leafCache.createAtOrGetAndSet(
//                    location,
//                    material
//            );
//            if (blockData instanceof final Leaves leaves) {
//                newState.actuallyPersistent(leaves.isPersistent());
//                newState.actualDistance(leaves.getDistance());
//            }
////            state.setPersistent(newState.state().isPersistent());
////            state.setDistance(newState.state().getDistance());
////            toUpdate.put(location, blockData);
//            LeafUpdater3.scheduleTick(location);
//            return;
//        }
//        if (!Tag.LEAVES.isTagged(material)) {
//            this.leafCache.remove(location);
//            // we only really care about checking for updates if it can
//            // save another leaf from decaying
//            if (fakeLeafState.actualDistance() < 6) {
////                toUpdate.put(location, blockData);
//                LeafUpdater3.scheduleTick(location);
//            }
//            return;
//        }

//        final Block block = event.getBlock();
//        if (!(block.getBlockData() instanceof final Leaves leaves)) return;
//        if (leaves.getDistance() < 7 || !leaves.isPersistent()) return;
//        final Chunk chunk = block.getChunk();
//        final Position2D chunkPos = new Position2D(block.getWorld().getUID(), chunk.getX(), chunk.getZ());
//        final Position position = new Position(ChunkUtil.getCoordInChunk(block.getX()), block.getY(), ChunkUtil.getCoordInChunk(block.getZ()));
//        final FakeLeafState fakeLeafState = this.plugin.getLeafCache().getAt(chunkPos, position);
//        if (fakeLeafState == null || fakeLeafState.actuallyPersistent()) return;
//        leaves.setPersistent(false);
//        block.setBlockData(leaves, false);
    }

//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//    public void onBlockBreak(BlockBreakEvent event) {
//        Bukkit.broadcastMessage("Broke block");
//        this.checkBlock(event, event.getBlock(), Material.AIR.createBlockData());
//    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        for (BlockState state : event.getBlocks()) {
            final BlockData blockData = state.getBlockData();
            if (!(blockData instanceof final Leaves leaves)) {
                if (this.config.isLogBlock(blockData)) {
                    this.leafCache.setLogAt(state.getLocation());
                    state.update(true, false);
                }
                continue;
            }
            final FakeLeafState fakeLeafState = this.leafCache.createAtOrGetAndSet(state.getLocation(), blockData.getMaterial());
            fakeLeafState.actualDistance(leaves.getDistance());
            state.update(true, false);
        }
    }

    private void checkBlock(Event event, Block block, BlockData blockData) {
        final boolean isBlockBreak = event instanceof BlockBreakEvent;
        final Location location = block.getLocation();

//        event.getChangedBlockData()

        if (this.leafCache.isLogAt(location)) {
            if (this.config.isLogBlock(blockData)) return;
//            this.leafCache.removeLogAt(location);
//            removedLogs.add(location);
            LeafUpdater3.scheduleTick(location);
            return;
        }
        if (this.config.isLogBlock(blockData)) {
//                if (leafCache.isLogAt(location)) continue;
//            this.leafCache.setLogAt(location);
//            toUpdate.put(location, blockData);
//            addedLogs.add(location);
            LeafUpdater3.scheduleTick(location);
            return;
        }
        final FakeLeafState fakeLeafState = this.leafCache.getAt(location);
        final Material material = blockData.getMaterial();
        if (fakeLeafState == null) {
            if (!Tag.LEAVES.isTagged(material)) return;
            final FakeLeafState newState = this.leafCache.createAtOrGetAndSet(
                    location,
                    material
            );
            if (blockData instanceof final Leaves leaves) {
                newState.actuallyPersistent(leaves.isPersistent());
                newState.actualDistance(leaves.getDistance());
            }
//            state.setPersistent(newState.state().isPersistent());
//            state.setDistance(newState.state().getDistance());
//            toUpdate.put(location, blockData);
            LeafUpdater3.scheduleTick(location);
            return;
        }
        if (!Tag.LEAVES.isTagged(material)) {
//            this.leafCache.remove(location);
            // we only really care about checking for updates if it can
            // save another leaf from decaying
            if (fakeLeafState.actualDistance() < 6) {
//                toUpdate.put(location, blockData);
                LeafUpdater3.scheduleTick(location);
            }
        } /*else {
//            Bukkit.broadcastMessage("Material: " + material + " - " + event.getClass().getName());
            final Leaves leaves = (Leaves) blockData;
            Bukkit.broadcastMessage(LeafUpdater3.getCurrentTick() + " - " + leaves.getDistance() + " - " + leaves.isPersistent());
        }*/
    }

//    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
//    public void onCustomBlockBreak(CustomBlockDataRemoveEvent event) {
//        final Block block = event.getBlock();
//        if (!(block.getBlockData() instanceof Leaves)) return;
//        Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.removeBlock(block, Bukkit.getOnlinePlayers()), 1);
//    }

}
