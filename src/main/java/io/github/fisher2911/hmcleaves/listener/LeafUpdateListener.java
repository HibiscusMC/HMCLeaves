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
import io.github.fisher2911.hmcleaves.util.LeafUpdater;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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

        checkBlock(block, block.getBlockData());
    }

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

    private void checkBlock(Block block, BlockData blockData) {
        final Location location = block.getLocation();

        if (this.leafCache.isLogAt(location)) {
            if (this.config.isLogBlock(blockData)) return;
            LeafUpdater.scheduleTick(location);
            return;
        }
        if (this.config.isLogBlock(blockData)) {
            LeafUpdater.scheduleTick(location);
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
            LeafUpdater.scheduleTick(location);
            return;
        }
        if (!Tag.LEAVES.isTagged(material)) {
            // we only really care about checking for updates if it can
            // save another leaf from decaying
            if (fakeLeafState.actualDistance() < 6) {
                LeafUpdater.scheduleTick(location);
            }
        }
    }
}
