package com.hibiscusmc.hmcleaves.listener;

import com.hibiscusmc.hmcleaves.HMCLeavesPlugin;
import com.hibiscusmc.hmcleaves.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.packet.PacketUtil;
import com.hibiscusmc.hmcleaves.util.Constants;
import com.hibiscusmc.hmcleaves.util.PlayerUtils;
import com.hibiscusmc.hmcleaves.util.PositionUtils;
import com.hibiscusmc.hmcleaves.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.world.LeavesWorldManager;
import com.hibiscusmc.hmcleaves.world.Position;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class WorldListener implements Listener {

    private final HMCLeavesPlugin plugin;
    private final LeavesConfig<BlockData> leavesConfig;
    private final LeavesWorldManager worldManager;
    private final Function<UUID, LeavesWorld> leavesWorldCreator;
    private final Function<ChunkPosition, LeavesChunk> leavesChunkCreator;

    public WorldListener(
            HMCLeavesPlugin plugin,
            Function<UUID, LeavesWorld> leavesWorldCreator,
            Function<ChunkPosition, LeavesChunk> leavesChunkCreator
    ) {
        this.plugin = plugin;
        this.leavesConfig = plugin.leavesConfig();
        this.worldManager = plugin.worldManager();
        this.leavesWorldCreator = leavesWorldCreator;
        this.leavesChunkCreator = leavesChunkCreator;
    }

    public WorldListener(HMCLeavesPlugin plugin) {
        this(plugin, worldId -> new LeavesWorld(worldId, new HashMap<>()), chunkPosition -> new LeavesChunk(chunkPosition, new HashMap<>()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldLoadEvent event) {
        final var world = event.getWorld();
        for (Chunk chunk : world.getLoadedChunks()) {
            this.loadChunk(world, chunk);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        this.loadChunk(event.getWorld(), event.getChunk());
    }

    private void loadChunk(World world, Chunk chunk) {
        final UUID worldId = world.getUID();
        final LeavesWorld leavesWorld = this.worldManager.getOrAddWorld(worldId, this::createLeavesWorld);
        final int minY = world.getMinHeight();
        final int maxY = world.getMaxHeight();
        final Collection<Player> nearbyPlayers = Bukkit.getOnlinePlayers().stream()
                .filter(player -> PlayerUtils.playerInViewDistanceOfChunk(player, worldId, chunk.getX(), chunk.getZ()))
                .collect(Collectors.toSet());
        for (int y = minY; y <= maxY; y++) {
            final ChunkPosition chunkPosition = new ChunkPosition(worldId, chunk.getX(), PositionUtils.coordToChunkCoord(y), chunk.getZ());
            for (int x = 0; x <= Constants.CHUNK_MAX_X; x++) {
                for (int z = 0; z <= Constants.CHUNK_MAX_Z; z++) {
                    final var block = chunk.getBlock(x, y, z);
                    final var leavesBlock = this.leavesConfig.getLeavesBlockFromWorldBlockData(block.getBlockData());
                    if (leavesBlock == null) {
                        continue;
                    }
                    final var position = Position.at(
                            worldId,
                            PositionUtils.chunkCoordToCoord(chunkPosition.x(), x),
                            y,
                            PositionUtils.chunkCoordToCoord(chunkPosition.z(), z)
                    );
                    leavesWorld.editInsertLeavesChunk(
                            chunkPosition,
                            leavesChunk -> leavesChunk.setBlock(position, leavesBlock),
                            this::createLeavesChunk
                    );
                }
            }
            if (y != world.getMinHeight() && y % Constants.CHUNK_HEIGHT == 0) {
                final LeavesChunk leavesChunk = leavesWorld.getChunk(chunkPosition);
                if (leavesChunk == null) {
                    continue;
                }
                PacketUtil.sendMultiBlockChange(chunkPosition, leavesChunk, nearbyPlayers);
            }
        }
    }

    private LeavesWorld createLeavesWorld(UUID worldId) {
        return this.leavesWorldCreator.apply(worldId);
    }

    private LeavesChunk createLeavesChunk(ChunkPosition chunkPosition) {
        return this.leavesChunkCreator.apply(chunkPosition);
    }

}
