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

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.cache.BlockCache;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.data.LeafData;
import io.github.fisher2911.hmcleaves.packet.PacketUtils;
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
import java.util.List;

public class LeafAndLogEditListener implements Listener {

    private final HMCLeaves plugin;
    private final BlockCache blockCache;

    public LeafAndLogEditListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.blockCache = plugin.getBlockCache();
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
        for (Block block : copyList) {
            final Position originalPosition = Position.fromLocation(block.getLocation());
            final Position newPosition = Position.fromLocation(block.getRelative(direction).getLocation());
            final BlockData blockData = this.blockCache.removeBlockData(originalPosition);
            // leaves get destroyed by pistons
            if (blockData == BlockData.EMPTY || blockData instanceof LeafData) continue;
            this.blockCache.addBlockData(newPosition, blockData);
        }
    }

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
        for (Block block : copyList) {
            final Position originalPosition = Position.fromLocation(block.getLocation());
            final Position newPosition = Position.fromLocation(block.getRelative(direction).getLocation());
            final BlockData blockData = this.blockCache.removeBlockData(originalPosition);
            if (blockData == BlockData.EMPTY) continue;
            this.blockCache.addBlockData(newPosition, blockData);
        }
    }

}
