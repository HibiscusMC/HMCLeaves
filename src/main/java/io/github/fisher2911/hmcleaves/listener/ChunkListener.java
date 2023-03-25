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
import io.github.fisher2911.hmcleaves.data.DataManager;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChunkListener implements Listener {

    private final HMCLeaves plugin;
    private final Config config;
    private final LeafCache cache;

    public ChunkListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.config = plugin.config();
        this.cache = plugin.getLeafCache();
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                this.handleChunkLoad(chunk, world);
            }
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        final World world = event.getWorld();
        for (Chunk c : world.getLoadedChunks()) {
            this.handleChunkLoad(c, world);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        final Chunk chunk = event.getChunk();
        this.handleChunkLoad(chunk, event.getWorld());
    }

    private void handleChunkLoad(Chunk chunk, World world) {
        final PersistentDataContainer container = chunk.getPersistentDataContainer();
        if (PDCUtil.hasLeafData(container)) {
            this.loadPDCData(chunk);
            return;
        }
        final ChunkSnapshot snapshot = chunk.getChunkSnapshot();
        final int chunkX = chunk.getX();
        final int chunkZ = chunk.getZ();
        final Position2D chunkPosition = new Position2D(world.getUID(), chunkX, chunkZ);
        final Map<Position, FakeLeafState> added = new HashMap<>();
        final Set<Position> addedLogs = new HashSet<>();
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            for (int x = 0; x < 16; x++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    for (int z = 0; z < 16; z++) {
                        final Material material = snapshot.getBlockType(x, y, z);
                        final Position position = new Position(world.getUID(), x, y, z);
                        if (!Tag.LEAVES.isTagged(material)) {
                            if (this.config.isLogBlock(snapshot.getBlockData(x, y, z))) {
                                addedLogs.add(position);
                                this.cache.setLogAt(chunkPosition, position);
                            }
                            continue;
                        }
                        final var defaultState = this.config.getDefaultState(material);
                        if (defaultState == null) continue;
                        if (!(snapshot.getBlockData(x, y, z) instanceof Leaves leaves)) continue;
                        final FakeLeafState actualState = new FakeLeafState(
                                defaultState.state(),
                                material,
                                leaves.isPersistent(),
                                leaves.getDistance()
                        );
                        this.cache.addData(chunkPosition, position, actualState);
                        added.put(position, actualState);
                    }
                }
            }
            if (!world.isChunkLoaded(chunkX, chunkZ)) return;
            final DataManager dataManager = this.plugin.getDataManager();
            dataManager.saveLeaves(added);
            dataManager.saveLogs(addedLogs);
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                PDCUtil.setHasLeafData(chunk.getPersistentDataContainer());
            });
        });
    }

    private void loadPDCData(Chunk chunk) {
        final World world = chunk.getWorld();
        final UUID worldUUID = world.getUID();
        final Position2D chunkPos = new Position2D(worldUUID, chunk.getX(), chunk.getZ());
        final Set<Position> leavesToDelete = new HashSet<>();
        final Set<Position> logsToDelete = new HashSet<>();
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
//            final var blocks = CustomBlockData.getBlocksWithCustomData(this.plugin, chunk);
            final DataManager dataManager = this.plugin.getDataManager();
            final Map<Position, FakeLeafState> leafStates = dataManager.loadLeavesInChunk(chunkPos);
            final var logs = dataManager.loadLogsInChunk(chunkPos);
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                for (var entry : leafStates.entrySet()) {
                    final Position position = entry.getKey();
                    final FakeLeafState state = entry.getValue();
//                final CustomBlockData blockData = new CustomBlockData(block, this.plugin);
                    final Material leafMaterial = state.material();
                    if (!Tag.LEAVES.isTagged(leafMaterial)/* && blockData.has(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE)*/) {
//                    PDCUtil.clearLeafData(blockData);
                        leavesToDelete.add(entry.getKey());
                        continue;
                    }
//                final Position position = new Position(ChunkUtil.getCoordInChunk(block.getX()), block.getY(), ChunkUtil.getCoordInChunk(block.getZ()));
                    final Block block = world.getBlockAt(position.x(), position.y(), position.z());
                    if (!(block.getBlockData() instanceof Leaves leaves)) continue;
//                    Byte distance = blockData.get(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE);
//                    if (distance == null) distance = (byte) leaves.getDistance();
//                    Byte persistent = blockData.get(PDCUtil.PERSISTENCE_KEY, PersistentDataType.BYTE);
//                    if (persistent == null) persistent = (byte) (leaves.isPersistent() ? 1 : 0);
//                    Byte actuallyPersistent = blockData.get(PDCUtil.ACTUAL_PERSISTENCE_KEY, PersistentDataType.BYTE);
//                    if (actuallyPersistent == null) actuallyPersistent = (byte) (leaves.isPersistent() ? 1 : 0);
//                    final Byte actualDistance = blockData.get(PDCUtil.ACTUAL_DISTANCE_KEY, PersistentDataType.BYTE);
//                    final FakeLeafState fakeLeafState = this.plugin.config().getDefaultState(block.getType());
//                    if (fakeLeafState == null) continue;
//                    final var state = fakeLeafState.state();
//                    state.setDistance(distance);
//                    state.setPersistent(persistent == 1);
                    try {
                        this.cache.addData(
                                chunkPos,
                                position,
                                state
//                                new FakeLeafState(state, actuallyPersistent == 1, actualDistance == null ? 7 : actualDistance)
                        );
                    } catch (Exception e) {
                        this.plugin.getLogger().severe("Block threw error: " + block.getX() + ", " + block.getY() + ", " + block.getZ());
                        e.printStackTrace();
                    }
                }
                for (var position : logs) {
                    final Block block = world.getBlockAt(position.x(), position.y(), position.z());
                    final Material material = block.getType();
                    if (!Tag.LOGS.isTagged(material)) {
                        logsToDelete.add(position);
                        continue;
                    }
                    this.cache.setLogAt(chunkPos, position);
                }

                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                    if (!leavesToDelete.isEmpty()) dataManager.deleteLeaves(leavesToDelete);
                    if (!logsToDelete.isEmpty()) dataManager.deleteLogs(logsToDelete);
                });
            });
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        final Chunk chunk = event.getChunk();
        this.cache.remove(new Position2D(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ()));
    }

}
