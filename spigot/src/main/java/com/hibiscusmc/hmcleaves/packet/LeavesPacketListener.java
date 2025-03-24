package com.hibiscusmc.hmcleaves.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.hibiscusmc.hmcleaves.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.util.Constants;
import com.hibiscusmc.hmcleaves.util.PositionUtils;
import com.hibiscusmc.hmcleaves.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.world.LeavesWorldManager;
import com.hibiscusmc.hmcleaves.world.Position;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class LeavesPacketListener extends PacketListenerAbstract {

    private final LeavesWorldManager leavesWorldManager;
    private final LeavesConfig<BlockData> config;

    public LeavesPacketListener(LeavesWorldManager leavesWorldManager, LeavesConfig<BlockData> config) {
        this.leavesWorldManager = leavesWorldManager;
        this.config = config;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof final Player player)) {
            return;
        }
        if (!this.config.isWorldWhitelisted(player.getWorld().getUID())) {
            return;
        }
        final PacketTypeCommon packetType = event.getPacketType();
        switch (packetType) {
            case PacketType.Play.Server.CHUNK_DATA -> this.handleChunkSend(event, player.getWorld().getUID());
            case PacketType.Play.Server.BLOCK_CHANGE -> this.handleBlockChange(event, player.getWorld().getUID());
            case PacketType.Play.Server.MULTI_BLOCK_CHANGE ->
                    this.handleMultiBlockChange(event, player.getWorld().getUID());
            default -> {
            }
        }
    }


    /**
     * Requires {@code event.getPacketType() == PacketType.Play.Server.CHUNK_DATA}
     */
    private void handleChunkSend(PacketSendEvent event, UUID worldId) {
        final WrapperPlayServerChunkData packet = new WrapperPlayServerChunkData(event);
        final var column = packet.getColumn();
        final int chunkX = column.getX();
        final int chunkZ = column.getZ();
        final var world = Bukkit.getWorld(worldId);
        if (world == null) {
            return;
        }
        final var chunks = column.getChunks();
        final int absWorldMinHeight = Math.abs(world.getMinHeight());
        final LeavesWorld leavesWorld = this.leavesWorldManager.getWorld(worldId);
        if (leavesWorld == null) {
            return;
        }
        for (int index = 0; index < chunks.length; index++) {
            final int chunkY = index - (absWorldMinHeight / Constants.CHUNK_HEIGHT);
            final var chunk = chunks[index];
            final var chunkPosition = ChunkPosition.at(worldId, chunkX, chunkY, chunkZ);
            final var leavesChunk = leavesWorld.getChunk(chunkPosition);
            if (leavesChunk == null) {
                continue;
            }
            final var blocks = leavesChunk.getBlocks();
            for (var entry : blocks.entrySet()) {
                final var blockData = entry.getValue();
                final var position = entry.getKey();
                chunk.set(
                        PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                        PositionUtils.coordToCoordInChunk(position.x()),
                        PositionUtils.coordToCoordInChunk(position.y()),
                        PositionUtils.coordToCoordInChunk(position.z()),
                        blockData.blockState().get().getGlobalId()
                );
            }
        }
    }

    /**
     * Requires {@code event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE}
     */
    private void handleBlockChange(PacketSendEvent event, UUID worldId) {
        final var leavesWorld = this.leavesWorldManager.getWorld(worldId);
        if (leavesWorld == null) {
            return;
        }
        final var packet = new WrapperPlayServerBlockChange(event);
        final var positionVector = packet.getBlockPosition();
        final var position = Position.at(worldId, positionVector.x, positionVector.y, positionVector.z);
        final var chunkPos = position.toChunkPosition();
        final var leavesChunk = leavesWorld.getChunk(chunkPos);
        if (leavesChunk == null) {
            return;
        }
        final var blockData = leavesChunk.getBlock(position);
        if (blockData == null) {
            return;
        }
        final var newPacket = new WrapperPlayServerBlockChange(
                packet.getBlockPosition(),
                blockData.blockState().get().getGlobalId()
        );
        event.setCancelled(true);
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(event.getPlayer(), newPacket);
    }

    /**
     * Requires {@code event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE}
     */
    private void handleMultiBlockChange(PacketSendEvent event, UUID worldId) {
        final var leavesWorld = this.leavesWorldManager.getWorld(worldId);
        if (leavesWorld == null) {
            return;
        }
        final var packet = new WrapperPlayServerMultiBlockChange(event);
        for (var block : packet.getBlocks()) {
            final var position = Position.at(worldId, block.getX(), block.getY(), block.getZ());
            final var chunkPos = position.toChunkPosition();
            final var leavesChunk = leavesWorld.getChunk(chunkPos);
            if (leavesChunk == null) {
                continue;
            }
            final var blockData = leavesChunk.getBlock(position);
            if (blockData == null) {
                continue;
            }
            block.setBlockState(blockData.blockState().get());
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
    }


}
