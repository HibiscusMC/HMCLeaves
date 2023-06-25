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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
//        this.leafDatabase.doDatabaseReadAsync(() -> {
//            if (!(this.leafDatabase.isChunkLoaded(chunkPosition))) {
//                this.loadNewChunkData(snapshot, world);
//                return;
//            }
//        });
        this.loadChunkFromDatabase(ChunkPosition.at(worldUUID, chunk.getX(), chunk.getZ()));
    }

    private void loadNewChunkData(ChunkSnapshot chunkSnapshot, World world) {
//        final UUID worldUUID = world.getUID();
//        final int minY = world.getMinHeight();
//        final int maxY = world.getMaxHeight();
//        final int chunkX = chunkSnapshot.getX();
//        final int chunkZ = chunkSnapshot.getZ();
//        final ChunkPosition chunkPosition = ChunkPosition.at(worldUUID, chunkX, chunkZ);
//        this.leafDatabase.doDatabaseWriteAsync(() -> {
//            this.leafDatabase.deleteChunk(chunkPosition);
//            this.leafDatabase.setChunkLoaded(chunkPosition);
//        });
//        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
//            final Map<Position, Material> worldMaterials = new HashMap<>();
//            ChunkBlockCache chunkBlockCache = this.blockCache.getChunkBlockCache(chunkPosition);
//            boolean markClean = chunkBlockCache == null || chunkBlockCache.isClean();
//            for (int x = 0; x < 16; x++) {
//                for (int y = minY; y < maxY; y++) {
//                    for (int z = 0; z < 16; z++) {
//                        final org.bukkit.block.data.BlockData bukkitBlockData = chunkSnapshot.getBlockData(x, y, z);
//                        final Material material = bukkitBlockData.getMaterial();
//                        final Position position = Position.at(
//                                worldUUID,
//                                chunkX * 16 + x,
//                                y,
//                                chunkZ * 16 + z
//                        );
//                        if (Tag.LOGS.isTagged(material)) {
//                            worldMaterials.put(position, material);
//                            final Axis axis;
//                            if (bukkitBlockData instanceof final Orientable orientable) {
//                                axis = orientable.getAxis();
//                            } else {
//                                axis = Axis.Y;
//                            }
//                            final BlockData blockData;
//                            if (material.toString().contains("STRIPPED")) {
//                                blockData = this.leavesConfig.getDefaultStrippedLogData(material, axis);
//                            } else {
//                                blockData = this.leavesConfig.getDefaultLogData(material, axis);
//                            }
//                            try {
//                                this.blockCache.addBlockData(
//                                        position,
//                                        blockData
//                                );
//                            } catch (NullPointerException e) {
//                                this.plugin.getLogger().severe("NPE when trying to add " + material + " " + LeavesConfig.getDefaultLogStringId(material) + " " + blockData);
//                                this.plugin.getLogger().severe("Position: " + position);
//                            }
//                            continue;
//                        }
//                        if (Tag.CAVE_VINES.isTagged(material)) {
//                            if (!(bukkitBlockData instanceof final CaveVinesPlant caveVines)) continue;
//                            worldMaterials.put(position, material);
//                            this.blockCache.addBlockData(
//                                    position,
//                                    this.leavesConfig.getDefaultCaveVinesData(caveVines.isBerries())
//                            );
//                            continue;
//                        }
//                        if (LeavesConfig.AGEABLE_MATERIALS.contains(material)) {
//                            worldMaterials.put(position, material);
//                            this.blockCache.addBlockData(
//                                    position,
//                                    this.leavesConfig.getDefaultAgeableData(material)
//                            );
//                            continue;
//                        }
//                        if (!(bukkitBlockData instanceof Leaves)) continue;
//                        worldMaterials.put(position, material);
//                        this.blockCache.addBlockData(
//                                position,
//                                this.leavesConfig.getDefaultLeafData(material)
//                        );
//                    }
//                }
//            }
//            this.leafDatabase.doDatabaseWriteAsync(() -> {
//                if (!this.plugin.isEnabled()) return;
//                final ChunkBlockCache newChunkBlockCache = this.blockCache.getChunkBlockCache(chunkPosition);
//                this.leafDatabase.setChunkLoaded(chunkPosition);
//                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () ->
//                        this.sendBlocksToPlayersAlreadyInChunk(world, chunkX, chunkZ, worldMaterials)
//                );
//                if (newChunkBlockCache == null) return;
//                this.leafDatabase.saveBlocksInChunk(newChunkBlockCache);
//                if (markClean) newChunkBlockCache.markClean();
//            });
//        });
    }

    private void loadChunkFromDatabase(ChunkPosition chunkPosition) {
        this.leafDatabase.doDatabaseReadAsync(() -> {
            final Map<Position, BlockData> chunkBlocks = this.leafDatabase.getBlocksInChunk(chunkPosition, this.leavesConfig);
            ChunkBlockCache chunkBlockCache = this.blockCache.getChunkBlockCache(chunkPosition);
            boolean markClean = chunkBlockCache == null || chunkBlockCache.isClean();
            chunkBlocks.forEach(this.blockCache::addBlockData);
            chunkBlockCache = this.blockCache.getChunkBlockCache(chunkPosition);
            if (!this.plugin.isEnabled()) return;
            final ChunkBlockCache finalChunkBlockCache = chunkBlockCache;
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                final World world = Bukkit.getWorld(chunkPosition.world());
                if (world == null) return;
                final Map<Position, Material> worldMaterials = new HashMap<>();
                if (finalChunkBlockCache == null) return;
                for (var entry : finalChunkBlockCache.getBlockDataMap().entrySet()) {
                    final Position position = entry.getKey();
                    final Location location = position.toLocation();
                    final Material material = world.getType(location);
                    worldMaterials.put(position, material);
                }
                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                            this.sendBlocksToPlayersAlreadyInChunk(world, chunkPosition.x(), chunkPosition.z(), worldMaterials);
                        }
                );
            });
            if (chunkBlockCache == null || !markClean) return;
            chunkBlockCache.markClean();
        });
    }

    private void sendBlocksToPlayersAlreadyInChunk(World world, int chunkX, int chunkZ, Map<Position, Material> worldMaterials) {
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
