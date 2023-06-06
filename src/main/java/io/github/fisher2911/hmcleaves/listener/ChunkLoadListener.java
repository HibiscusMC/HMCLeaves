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
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.UUID;

public class ChunkLoadListener implements Listener {

    private final HMCLeaves plugin;
    private final BlockCache blockCache;
    private final LeavesConfig leavesConfig;

    public ChunkLoadListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.blockCache = plugin.getBlockCache();
        this.leavesConfig = plugin.getLeavesConfig();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        final Chunk chunk = event.getChunk();
        final World world = chunk.getWorld();
        final UUID worldUUID = world.getUID();
        final int minY = world.getMinHeight();
        final int maxY = world.getMaxHeight();
        if (!(PDCUtil.hasLeafData(chunk.getPersistentDataContainer()))) {
            final int chunkX = chunk.getX();
            final int chunkZ = chunk.getZ();
            final ChunkSnapshot snapshot = chunk.getChunkSnapshot();
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                for (int x = 0; x < 16; x++) {
                    for (int y = minY; y < maxY; y++) {
                        for (int z = 0; z < 16; z++) {
                            final org.bukkit.block.data.BlockData bukkitBlockData = snapshot.getBlockData(x, y, z);
                            final Material material = bukkitBlockData.getMaterial();
                            if (Tag.LOGS.isTagged(material)) {
                                final BlockData blockData;
                                if (material.toString().contains("STRIPPED")) {
                                    blockData = this.leavesConfig.getDefaultStrippedLogData();
                                } else {
                                    blockData = this.leavesConfig.getDefaultLogData();
                                }
                                final Position position = Position.at(
                                        worldUUID,
                                        chunkX * 16 + x,
                                        y,
                                        chunkZ * 16 + z
                                );
                                this.blockCache.addBlockData(
                                        position,
                                        blockData
                                );
                                continue;
                            }
                            if (!(bukkitBlockData instanceof Leaves)) continue;
                            final Position position = Position.at(
                                    worldUUID,
                                    chunkX * 16 + x,
                                    y,
                                    chunkZ * 16 + z
                            );
                            this.blockCache.addBlockData(
                                    position,
                                    this.leavesConfig.getDefaultLeafData()
                            );
                        }
                    }
                }
            });
        }
    }

}
