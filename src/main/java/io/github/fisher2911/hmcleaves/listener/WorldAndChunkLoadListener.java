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
import io.github.fisher2911.hmcleaves.debug.Debugger;
import io.github.fisher2911.hmcleaves.packet.PacketUtils;
import io.github.fisher2911.hmcleaves.world.ChunkPosition;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
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
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class WorldAndChunkLoadListener implements Listener {

    private final HMCLeaves plugin;
    private final BlockCache blockCache;
    private final LeavesConfig leavesConfig;
    private final LeafDatabase leafDatabase;
    private final Executor chunkBlockReadExecutor;

    public WorldAndChunkLoadListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.blockCache = plugin.getBlockCache();
        this.leavesConfig = plugin.getLeavesConfig();
        this.leafDatabase = plugin.getLeafDatabase();
        this.chunkBlockReadExecutor = Executors.newSingleThreadExecutor();
    }

    public void loadDefaultWorlds() {
        for (World world : Bukkit.getWorlds()) {
            if (!this.leavesConfig.isWorldWhitelisted(world)) continue;
            for (Chunk chunk : world.getLoadedChunks()) {
                this.loadChunk(chunk);
//                final UUID worldUUID = world.getUID();
//                if (!(PDCUtil.chunkHasLeafData(chunk.getPersistentDataContainer()))) {
//                    this.loadNewChunkData(chunk);
//                    continue;
//                }
//                this.loadChunkFromDatabase(ChunkPosition.at(worldUUID, chunk.getX(), chunk.getZ()));
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
        Debugger.getInstance().logChunkLoadTaskStart(chunkPosition);
        this.leafDatabase.doDatabaseReadAsync(() -> {
            if (!(this.leafDatabase.isChunkLoaded(chunkPosition))) {
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
        this.sendBlocksToPlayersAlreadyInChunk(world.getUID(), chunkPosition.x(), chunkPosition.z(), worldMaterials);
        this.leafDatabase.doDatabaseWriteAsync(() -> {
            try {
                this.leafDatabase.saveDefaultDataLayers(chunkPosition, yLevels);
                this.leafDatabase.setChunkLoaded(chunkPosition);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void loadChunkFromDatabase(ChunkPosition chunkPosition, ChunkSnapshot snapshot, UUID worldUUID) {
//        this.leafDatabase.doDatabaseReadAsync(() -> {
        final Map<Position, BlockData> chunkBlocks = this.leafDatabase.getBlocksInChunk(chunkPosition, this.leavesConfig);
        final Collection<Integer> layers = this.leafDatabase.getDefaultLayersInChunk(chunkPosition);
        ChunkBlockCache chunkBlockCache = this.blockCache.getChunkBlockCache(chunkPosition);
        boolean markClean = chunkBlockCache == null || chunkBlockCache.isClean();
        chunkBlocks.forEach(this.blockCache::addBlockData);
        chunkBlockCache = this.blockCache.getChunkBlockCache(chunkPosition);
        if (!this.plugin.isEnabled()) return;
        final ChunkBlockCache finalChunkBlockCache = chunkBlockCache;
        final Map<Position, Material> worldMaterials = new HashMap<>();
        Debugger.getInstance().logChunkLayersLoad(chunkPosition, layers.size());
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
        if (finalChunkBlockCache == null) return;
        Debugger.getInstance().logChunkLoadEnd(chunkPosition, finalChunkBlockCache.getBlockDataMap().size());
        for (var entry : finalChunkBlockCache.getBlockDataMap().entrySet()) {
            final Position position = entry.getKey();
            final int positionInChunkX = position.x() & 15;
            final int positionInChunkZ = position.z() & 15;
            final Material material = snapshot.getBlockData(positionInChunkX, position.y(), positionInChunkZ).getMaterial();
            worldMaterials.put(position, material);
        }
        this.sendBlocksToPlayersAlreadyInChunk(worldUUID, chunkPosition.x(), chunkPosition.z(), worldMaterials);
        if (!markClean) return;
        chunkBlockCache.markClean();
//        });
    }

    private void sendBlocksToPlayersAlreadyInChunk(UUID worldUUID, int chunkX, int chunkZ, Map<Position, Material> worldMaterials) {
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            Debugger.getInstance().logChunkBlockSendStart(new ChunkPosition(worldUUID, chunkX, chunkZ));
            final World world = Bukkit.getWorld(worldUUID);
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                return;
            }
            final int renderDistance = Bukkit.getViewDistance();
            final Collection<Player> playersThatCanSee = world.getPlayers()
                    .stream()
                    .filter(player -> {
                        final Location location = player.getLocation();
                        final Chunk chunk = location.getChunk();
                        final int playerChunkX = chunk.getX();
                        final int playerChunkZ = chunk.getZ();
                        return Math.abs(playerChunkX - chunkX) <= renderDistance && Math.abs(playerChunkZ - chunkZ) <= renderDistance;
                    })
                    .collect(Collectors.toList());
            final ChunkBlockCache chunkBlockCache = this.blockCache.getChunkBlockCache(ChunkPosition.at(world.getUID(), chunkX, chunkZ));
            if (chunkBlockCache == null) return;
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                PacketUtils.sendMultiBlockChange(
                        chunkBlockCache.getChunkPosition(),
                        chunkBlockCache.getBlockDataMap(),
                        worldMaterials,
                        playersThatCanSee
                );
                Debugger.getInstance().logChunkBlockSendEnd(new ChunkPosition(worldUUID, chunkX, chunkZ));
            });
        });
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
        final UUID worldUUID = world.getUID();
        for (Chunk chunk : world.getLoadedChunks()) {
            this.loadChunk(chunk);
//            final ChunkPosition chunkPosition = ChunkPosition.at(worldUUID, chunk.getX(), chunk.getZ());
//            this.leafDatabase.doDatabaseReadAsync(() -> {
//                final Map<Position, BlockData> chunkBlocks = this.leafDatabase.getBlocksInChunk(chunkPosition, this.leavesConfig);
//                chunkBlocks.forEach(this.blockCache::addBlockData);
//                final ChunkBlockCache chunkBlockCache = this.blockCache.getChunkBlockCache(chunkPosition);
//                if (chunkBlockCache == null) return;
//                chunkBlockCache.markClean();
//            });
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
