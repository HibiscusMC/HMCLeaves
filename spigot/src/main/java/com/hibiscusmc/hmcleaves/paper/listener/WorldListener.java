package com.hibiscusmc.hmcleaves.paper.listener;

import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlock;
import com.hibiscusmc.hmcleaves.paper.block.LogBlock;
import com.hibiscusmc.hmcleaves.paper.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.paper.database.LeavesDatabase;
import com.hibiscusmc.hmcleaves.paper.packet.PacketUtil;
import com.hibiscusmc.hmcleaves.paper.util.Constants;
import com.hibiscusmc.hmcleaves.paper.util.PlayerUtils;
import com.hibiscusmc.hmcleaves.paper.util.PositionUtils;
import com.hibiscusmc.hmcleaves.paper.util.WorldUtil;
import com.hibiscusmc.hmcleaves.paper.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.paper.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorldManager;
import com.hibiscusmc.hmcleaves.paper.world.Position;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WorldListener implements Listener {

    private final HMCLeaves plugin;
    private final LeavesConfig leavesConfig;
    private final LeavesWorldManager worldManager;
    private final LeavesDatabase database;

    public WorldListener(
            HMCLeaves plugin
    ) {
        this.plugin = plugin;
        this.leavesConfig = plugin.leavesConfig();
        this.worldManager = plugin.worldManager();
        this.database = plugin.database();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldLoadEvent event) {
        final var world = event.getWorld();
        for (Chunk chunk : world.getLoadedChunks()) {
            this.loadChunk(world, chunk.getChunkSnapshot(true, false, false));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        this.loadChunk(event.getWorld(), event.getChunk().getChunkSnapshot(false, false, false));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        final var chunk = event.getChunk();
        final var world = chunk.getWorld();
        final LeavesWorld leavesWorld = this.worldManager.getOrAddWorld(world.getUID());
        final ChunkPosition chunkPosition = new ChunkPosition(world.getUID(), chunk.getX(), chunk.getZ());
        if (leavesWorld.isLoadingChunk(chunkPosition)) {
            return;
        }
        this.saveChunk(world, leavesWorld, chunk, true);
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        final var world = event.getWorld();
        final LeavesWorld leavesWorld = this.worldManager.getOrAddWorld(world.getUID());
        for (Chunk chunk : world.getLoadedChunks()) {
            this.saveChunk(world, leavesWorld, chunk, false);
        }
    }

    private void saveChunk(World world, LeavesWorld leavesWorld, Chunk chunk, boolean remove) {
        final ChunkPosition chunkPosition = new ChunkPosition(world.getUID(), chunk.getX(), chunk.getZ());
        final LeavesChunk leavesChunk = leavesWorld.getChunk(chunkPosition);
        if (leavesChunk == null) {
            return;
        }
        this.saveChunk(leavesWorld, leavesChunk, remove);
    }

    private void saveChunk(LeavesWorld world, LeavesChunk leavesChunk, boolean remove) {
        if (!leavesChunk.isDirty()) {
            return;
        }
        leavesChunk.setDirty(false);
        this.database.executeWrite(() -> {
            try {
                this.database.saveChunk(leavesChunk);
            } catch (SQLException e) {
                this.plugin.getLogger().severe("Failed to save chunk " + leavesChunk.chunkPosition() + " to database");
                e.printStackTrace();
            }
        });
        if (remove) {
            world.removeChunk(leavesChunk.chunkPosition());
        }
    }


    private void loadChunk(World world, ChunkSnapshot chunk) {
        final UUID worldId = world.getUID();
        final LeavesWorld leavesWorld = this.worldManager.getOrAddWorld(worldId);
        final ChunkPosition chunkPosition = new ChunkPosition(worldId, chunk.getX(), chunk.getZ());
        final Map<Position, CustomBlock> customBlockMap = new HashMap<>();
        this.database.executeRead(() -> {
            try {
                if (leavesWorld.isLoadingChunk(chunkPosition)) {
                    return;
                }
                leavesWorld.setLoadingChunk(chunkPosition);
                if (this.database.hasChunkBeenScanned(worldId, chunkPosition)) {
                    customBlockMap.putAll(this.database.loadChunk(worldId, chunkPosition));
                    if (customBlockMap.isEmpty()) {
                        leavesWorld.removeLoadingChunk(chunkPosition);
                        return;
                    }
                    leavesWorld.removeLoadingChunk(chunkPosition);
                    Bukkit.getScheduler().runTask(this.plugin, () -> this.updateBlockStates(world, leavesWorld, chunkPosition, customBlockMap));
                    return;
                }
                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> this.loadNewChunk(world, leavesWorld, chunk, chunkPosition));
            } catch (SQLException e) {
                this.plugin.getLogger().severe("Failed to check if chunk " + chunkPosition + " has been scanned");
                e.printStackTrace();
            }
        });
    }

    private void loadNewChunk(World world, LeavesWorld leavesWorld, ChunkSnapshot chunk, ChunkPosition chunkPosition) {
        final UUID worldId = world.getUID();
        final int minY = world.getMinHeight();
        final int maxY = world.getMaxHeight();
        final Map<Position, CustomBlock> customBlockMap = new HashMap<>();
        for (int y = minY; y < maxY; y++) {
            for (int x = 0; x <= Constants.CHUNK_MAX_X; x++) {
                for (int z = 0; z <= Constants.CHUNK_MAX_Z; z++) {
                    final var block = chunk.getBlockData(x, y, z);

                    final var customBlock = this.leavesConfig.getCustomBlockFromWorldBlockData(block);
                    if (customBlock == null) {
                        if (Tag.LEAVES.isTagged(block.getMaterial())) {
                            this.plugin.getLogger().warning("Found custom block in chunk " + chunkPosition + " but it is not in the config: " + block.getMaterial());
                        }
                        continue;
                    }
                    final var position = Position.at(
                            worldId,
                            PositionUtils.chunkCoordToCoord(chunkPosition.x(), x),
                            y,
                            PositionUtils.chunkCoordToCoord(chunkPosition.z(), z)
                    );
                    customBlockMap.put(position, customBlock);
//                    leavesWorld.editInsertChunk(
//                            chunkPosition,
//                            leavesChunk -> leavesChunk.setBlock(position, customBlock)
//                    );
                }
            }
        }
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            final LeavesChunk leavesChunk = leavesWorld.getChunk(chunkPosition);
            if (leavesChunk == null) {
                return;
            }
            this.updateBlockStates(world, leavesWorld, chunkPosition, customBlockMap);
        });
        this.database.executeWrite(() -> {
            try {
                this.database.setChunkScanned(worldId, chunkPosition);
                leavesWorld.removeLoadingChunk(chunkPosition);
            } catch (SQLException e) {
                this.plugin.getLogger().severe("Failed to set chunk " + chunkPosition + " as scanned");
                e.printStackTrace();
            }
        });
    }


    private void updateBlockStates(World bukkitWorld, LeavesWorld world, ChunkPosition chunkPosition, Map<Position, CustomBlock> customBlockMap) {
        world.editInsertChunk(chunkPosition, chunk -> customBlockMap.forEach(((position, customBlock) -> {
            final Location location = WorldUtil.convertPosition(bukkitWorld, position);
            chunk.setBlock(position, customBlock.getBlockStateFromWorldBlock(location.getBlock().getBlockData()));
        })));
        final LeavesChunk chunk = world.getChunk(chunkPosition);
        if (chunk == null) {
            return;
        }
        chunk.setDirty(false);
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            final Collection<? extends Player> nearbyPlayers = PlayerUtils.getNearbyPlayers(world.worldId(), chunkPosition);
            PacketUtil.sendMultiBlockChange(chunk, nearbyPlayers);
        });
    }

}
