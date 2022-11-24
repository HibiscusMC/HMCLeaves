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

import com.jeff_media.customblockdata.CustomBlockData;
import io.github.fisher2911.hmcleaves.Config;
import io.github.fisher2911.hmcleaves.FakeLeafState;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafCache;
import io.github.fisher2911.hmcleaves.util.ChunkUtil;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
                        final Position position = new Position(x, y, z);
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
                                leaves.isPersistent(),
                                leaves.getDistance()
                        );
                        this.cache.addData(chunkPosition, position, actualState);
                        added.put(position, actualState);
                    }
                }
            }
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (!world.isChunkLoaded(chunkX, chunkZ)) return;
                for (var entry : added.entrySet()) {
                    final var position = entry.getKey();
                    final var data = entry.getValue();
                    final PersistentDataContainer blockData = new CustomBlockData(
                            chunk.getBlock(position.x(), position.y(), position.z()),
                            this.plugin
                    );
                    blockData.set(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE, (byte) data.state().getDistance());
                    blockData.set(PDCUtil.PERSISTENCE_KEY, PersistentDataType.BYTE, (byte) (data.state().isPersistent() ? 1 : 0));
                    blockData.set(PDCUtil.ACTUAL_PERSISTENCE_KEY, PersistentDataType.BYTE, (byte) (data.actuallyPersistent() ? 1 : 0));
                    blockData.set(PDCUtil.ACTUAL_DISTANCE_KEY, PersistentDataType.BYTE, (byte) data.actualDistance());
                }
                for (var position : addedLogs) {
                    final PersistentDataContainer blockData = new CustomBlockData(
                            chunk.getBlock(position.x(), position.y(), position.z()),
                            this.plugin
                    );
                    PDCUtil.setLogBlock(blockData);
                }
                PDCUtil.setHasLeafData(chunk.getPersistentDataContainer());
            });
        });
    }

    private void loadPDCData(Chunk chunk) {
        final Position2D chunkPos = new Position2D(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        final var blocks = CustomBlockData.getBlocksWithCustomData(this.plugin, chunk);
        for (var block : blocks) {
            final CustomBlockData blockData = new CustomBlockData(block, this.plugin);
            if (!Tag.LEAVES.isTagged(block.getType()) && blockData.has(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE)) {
                PDCUtil.clearLeafData(blockData);
                continue;
            }
            final boolean isLog = this.config.isLogBlock(block.getBlockData());
            if (!isLog && blockData.has(PDCUtil.LOG_BLOCK_KEY, PersistentDataType.BYTE)) {
                PDCUtil.clearLogData(blockData);
                continue;
            }
            final Position position = new Position(ChunkUtil.getCoordInChunk(block.getX()), block.getY(), ChunkUtil.getCoordInChunk(block.getZ()));
            if (isLog) {
                this.cache.setLogAt(chunkPos, position);
                continue;
            }
            if (!(block.getBlockData() instanceof Leaves leaves)) continue;
            Byte distance = blockData.get(PDCUtil.DISTANCE_KEY, PersistentDataType.BYTE);
            if (distance == null) distance = (byte) leaves.getDistance();
            Byte persistent = blockData.get(PDCUtil.PERSISTENCE_KEY, PersistentDataType.BYTE);
            if (persistent == null) persistent = (byte) (leaves.isPersistent() ? 1 : 0);
            Byte actuallyPersistent = blockData.get(PDCUtil.ACTUAL_PERSISTENCE_KEY, PersistentDataType.BYTE);
            if (actuallyPersistent == null) actuallyPersistent = (byte) (leaves.isPersistent() ? 1 : 0);
            final Byte actualDistance = blockData.get(PDCUtil.ACTUAL_DISTANCE_KEY, PersistentDataType.BYTE);
            final FakeLeafState fakeLeafState = this.plugin.config().getDefaultState(block.getType());
            if (fakeLeafState == null) continue;
            final var state = fakeLeafState.state();
            state.setDistance(distance);
            state.setPersistent(persistent == 1);
            try {
                this.cache.addData(
                        chunkPos,
                        position,
                        new FakeLeafState(state, actuallyPersistent == 1, actualDistance == null ? 7 : actualDistance)
                );
            } catch (Exception e) {
                this.plugin.getLogger().severe("Block threw error: " + block.getX() + ", " + block.getY() + ", " + block.getZ());
                e.printStackTrace();
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        final Chunk chunk = event.getChunk();
        this.cache.remove(new Position2D(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ()));
    }

}
