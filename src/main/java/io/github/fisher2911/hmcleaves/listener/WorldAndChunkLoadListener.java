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
import io.github.fisher2911.hmcleaves.cache.ChunkBlockCache;
import io.github.fisher2911.hmcleaves.cache.WorldBlockCache;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.data.LeafDatabase;
import io.github.fisher2911.hmcleaves.packet.PacketUtils;
import io.github.fisher2911.hmcleaves.world.ChunkPosition;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class WorldAndChunkLoadListener implements Listener {

    private final HMCLeaves plugin;
    private final BlockCache blockCache;
    private final LeavesConfig leavesConfig;
    private final LeafDatabase leafDatabase;

    public WorldAndChunkLoadListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.blockCache = plugin.getBlockCache();
        this.leavesConfig = plugin.getLeavesConfig();
        this.leafDatabase = plugin.getLeafDatabase();
    }

    public void loadDefaultWorlds() {
        for (World world : Bukkit.getWorlds()) {
            if (!this.leavesConfig.isWorldWhitelisted(world)) continue;
            for (Chunk chunk : world.getLoadedChunks()) {
                this.loadChunk(chunk);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        final World world = event.getWorld();
        if (!this.leavesConfig.isWorldWhitelisted(world)) return;
        final Chunk chunk = event.getChunk();
        this.loadChunk(chunk);
    }

    private void loadChunk(Chunk chunk) {
        final World world = chunk.getWorld();
        final UUID worldUUID = world.getUID();
        final ChunkPosition chunkPosition = ChunkPosition.at(worldUUID, chunk.getX(), chunk.getZ());
        final ChunkSnapshot snapshot = chunk.getChunkSnapshot();
        this.leafDatabase.doDatabaseReadAsync(() -> {
            if (!this.leafDatabase.isChunkLoaded(chunkPosition)) {
                this.loadNewChunkData(snapshot, world);
                return;
            }
            this.loadChunkFromDatabase(ChunkPosition.at(worldUUID, chunk.getX(), chunk.getZ()), snapshot, worldUUID);
        });
    }

    private void loadNewChunkData(ChunkSnapshot chunkSnapshot, World world) {
        final List<Integer> yLevels = new ArrayList<>();
        final Map<Position, Material> worldMaterials = new HashMap<>();
        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
            int count = 0;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    final org.bukkit.block.data.BlockData bukkitBlockData = chunkSnapshot.getBlockData(x, y, z);
                    final BlockData blockData = this.leavesConfig.getDefaultBlockData(bukkitBlockData);
                    if (blockData == null) continue;
                    final Position position = Position.at(world.getUID(), chunkSnapshot.getX() * 16 + x, y, chunkSnapshot.getZ() * 16 + z);
                    this.blockCache.addBlockData(position, blockData);
                    worldMaterials.put(position, bukkitBlockData.getMaterial());
                    count++;
                }
            }
            if (count == 0) continue;
            yLevels.add(y);
        }
        final ChunkPosition chunkPosition = ChunkPosition.at(world.getUID(), chunkSnapshot.getX(), chunkSnapshot.getZ());
        this.sendBlocksToPlayersAlreadyInChunk(chunkPosition, worldMaterials);
        this.leafDatabase.doDatabaseWriteAsync(() -> {
            try {
                this.leafDatabase.saveDefaultDataLayers(chunkPosition.world(), yLevels, chunkPosition);
                this.leafDatabase.setChunkLoaded(chunkPosition);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void loadChunkFromDatabase(ChunkPosition chunkPosition, ChunkSnapshot snapshot, UUID worldUUID) {
        final Map<Position, BlockData> chunkBlocks = this.leafDatabase.getBlocksInChunk(chunkPosition, this.leavesConfig);
        ChunkBlockCache chunkBlockCache = this.blockCache.getChunkBlockCache(chunkPosition);
        boolean markClean = chunkBlockCache == null || chunkBlockCache.isClean();
        chunkBlocks.forEach(this.blockCache::addBlockData);
        if (!this.leafDatabase.isLayerLoaded(chunkPosition)) {
            this.leafDatabase.loadAllDefaultPossibleLayersInWorld(worldUUID, chunkPosition);
        }
        final Collection<Integer> layers = this.leafDatabase.getPossibleWorldDefaultLayers(chunkPosition);
        if (!this.plugin.isEnabled()) return;
        final Map<Position, Material> worldMaterials = new HashMap<>();
        for (int y : layers) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    final Position position = Position.at(chunkPosition.world(), chunkPosition.x() * 16 + x, y, chunkPosition.z() * 16 + z);
                    final BlockData current = this.blockCache.getBlockData(position);
                    if (current != BlockData.EMPTY) continue;
                    final org.bukkit.block.data.BlockData bukkitBlockData = snapshot.getBlockData(x, y, z);
                    final BlockData blockData = this.leavesConfig.getDefaultBlockData(bukkitBlockData);
                    if (blockData == null) continue;
                    this.blockCache.addBlockData(position, blockData);
                    worldMaterials.put(position, bukkitBlockData.getMaterial());
                }
            }
        }
        chunkBlockCache = this.blockCache.getChunkBlockCache(chunkPosition);
        if (chunkBlockCache == null) return;
        for (var entry : chunkBlockCache.getBlockDataMap().entrySet()) {
            final Position position = entry.getKey();
            final int positionInChunkX = position.x() & 15;
            final int positionInChunkZ = position.z() & 15;
            final Material material = snapshot.getBlockData(positionInChunkX, position.y(), positionInChunkZ).getMaterial();
            worldMaterials.put(position, material);
        }
        this.sendBlocksToPlayersAlreadyInChunk(chunkPosition, worldMaterials);
        if (!markClean) return;
        chunkBlockCache.markClean();
    }

    private void sendBlocksToPlayersAlreadyInChunk(ChunkPosition chunkPosition, Map<Position, Material> worldMaterials) {
        if (!this.plugin.isEnabled()) return;
        final ChunkBlockCache chunkBlockCache = this.blockCache.getChunkBlockCache(chunkPosition);
        if (chunkBlockCache == null) return;
        PacketUtils.sendMultiBlockChange(
                chunkBlockCache.getChunkPosition(),
                chunkBlockCache.getBlockDataMap(),
                worldMaterials,
                this.plugin.getLeavesPacketListener().getPlayersChunkSentTo(chunkPosition)
                        .stream()
                        .map(Bukkit::getPlayer)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        final World world = event.getWorld();
        if (!this.leavesConfig.isWorldWhitelisted(world)) return;
        final UUID worldUUID = world.getUID();
        final Chunk chunk = event.getChunk();
        final ChunkPosition chunkPosition = ChunkPosition.at(worldUUID, chunk.getX(), chunk.getZ());
        final ChunkBlockCache chunkBlockCache = this.blockCache.removeChunkBlockCache(chunkPosition);
        if (chunkBlockCache == null) return;
        if (chunkBlockCache.isClean()) return;
        this.leafDatabase.doDatabaseWriteAsync(() -> this.leafDatabase.saveBlocksInChunk(chunkBlockCache));
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        final World world = event.getWorld();
        if (!this.leavesConfig.isWorldWhitelisted(world)) return;
        for (Chunk chunk : world.getLoadedChunks()) {
            this.loadChunk(chunk);
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        final World world = event.getWorld();
        if (!this.leavesConfig.isWorldWhitelisted(world)) return;
        final UUID worldUUID = world.getUID();
        final WorldBlockCache worldBlockCache = this.blockCache.getWorldBlockCache(worldUUID);
        if (worldBlockCache == null) return;
        worldBlockCache.clearAll(chunkBlockCache -> {
                    if (chunkBlockCache.isClean()) return;
                    this.leafDatabase.doDatabaseWriteAsync(() -> this.leafDatabase.saveBlocksInChunk(chunkBlockCache));
                }
        );
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        final World world = event.getWorld();
        if (!this.leavesConfig.isWorldWhitelisted(world)) return;
        final UUID worldUUID = world.getUID();
        final WorldBlockCache worldBlockCache = this.blockCache.getWorldBlockCache(worldUUID);
        if (worldBlockCache == null) return;
        for (var entry : worldBlockCache.getBlockCacheMap().entrySet()) {
            final ChunkBlockCache chunkBlockCache = entry.getValue();
            if (chunkBlockCache.isClean()) continue;
            this.leafDatabase.doDatabaseWriteAsync(() -> this.leafDatabase.saveBlocksInChunk(chunkBlockCache));
        }
    }

}
