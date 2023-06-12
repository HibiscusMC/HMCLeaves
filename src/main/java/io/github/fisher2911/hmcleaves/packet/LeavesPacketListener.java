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

package io.github.fisher2911.hmcleaves.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.cache.BlockCache;
import io.github.fisher2911.hmcleaves.cache.ChunkBlockCache;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.data.LogData;
import io.github.fisher2911.hmcleaves.util.ChunkUtil;
import io.github.fisher2911.hmcleaves.world.ChunkPosition;
import io.github.fisher2911.hmcleaves.world.Position;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.data.type.TechnicalPiston;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LeavesPacketListener extends PacketListenerAbstract {

    private final BlockCache blockCache;
    private final BlockBreakManager blockBreakManager;

    public LeavesPacketListener(PacketListenerPriority priority, HMCLeaves plugin) {
        super(priority);
        this.blockCache = plugin.getBlockCache();
        this.blockBreakManager = plugin.getBlockBreakManager();
    }

    public LeavesPacketListener(HMCLeaves plugin) {
        this.blockCache = plugin.getBlockCache();
        this.blockBreakManager = plugin.getBlockBreakManager();
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof final Player player)) return;
        final PacketTypeCommon packetType = event.getPacketType();
        if (packetType == PacketType.Play.Server.CHUNK_DATA) {
            this.handleChunkSend(event, player.getWorld().getUID());
            return;
        }
        if (packetType == PacketType.Play.Server.BLOCK_CHANGE) {
            this.handleBlockChange(event, player.getWorld().getUID());
            return;
        }
        if (packetType == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            this.handleMultiBlockChange(event, player.getWorld().getUID());
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof final Player player)) return;
        final PacketTypeCommon packetType = event.getPacketType();
        if (packetType == PacketType.Play.Client.PLAYER_DIGGING) {
            this.handlePlayerDigging(event, player.getWorld().getUID());
        }
    }

    private static final int HEIGHT_BELOW_ZERO = 64;

    private void handleChunkSend(PacketSendEvent event, UUID world) {
        final WrapperPlayServerChunkData packet = new WrapperPlayServerChunkData(event);
        final Column column = packet.getColumn();
        final int chunkX = column.getX();
        final int chunkZ = column.getZ();
        final BaseChunk[] chunks = column.getChunks();
        for (int i = 0; i < chunks.length; i++) {
            final BaseChunk chunk = chunks[i];
            final int worldY = i * 16 - HEIGHT_BELOW_ZERO;
            final ChunkPosition chunkPos = ChunkPosition.at(world, chunkX, chunkZ);
            final ChunkBlockCache chunkCache = this.blockCache.getChunkBlockCache(chunkPos);
            if (chunkCache == null) continue;
            for (var entry : chunkCache.getBlockDataMap().entrySet()) {
                final var position = entry.getKey();
                final int difference = position.y() - worldY;
                if (difference < 0 || difference > 15) continue;
                final var blockData = entry.getValue();
                final int actualX = ChunkUtil.getCoordInChunk(position.x());
                final int actualY = Math.abs(ChunkUtil.getCoordInChunk(position.y()));
                final int actualZ = ChunkUtil.getCoordInChunk(position.z());
                chunk.set(
                        PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                        actualX,
                        actualY,
                        actualZ,
                        blockData.getNewState().getGlobalId()
                );
            }
        }
    }

    private void handleBlockChange(PacketSendEvent event, UUID world) {
        try {
            final WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(event);
            final Vector3i blockPosition = packet.getBlockPosition();
            final var position = Position.at(
                    world,
                    blockPosition.getX(),
                    blockPosition.getY(),
                    blockPosition.getZ()
            );
            final BlockData blockData = this.blockCache.getBlockData(position);
            if (blockData == BlockData.EMPTY) return;
            final Material worldMaterial = SpigotConversionUtil.toBukkitBlockData(packet.getBlockState()).getMaterial();
            if (blockData.worldBlockType() != worldMaterial && !worldMaterial.isAir() && worldMaterial != Material.MOVING_PISTON) {
                this.blockCache.removeBlockData(position);
                return;
            }
            final WrapperPlayServerBlockChange newPacket = new WrapperPlayServerBlockChange(
                    packet.getBlockPosition(),
                    blockData.getNewState().getGlobalId()
            );
            event.setCancelled(true);
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(event.getPlayer(), newPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMultiBlockChange(PacketSendEvent event, UUID world) {
        try {
            final WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(event);
            final var blocks = packet.getBlocks();
            for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocks) {
                final Position position = Position.at(
                        world,
                        block.getX(),
                        block.getY(),
                        block.getZ()
                );
                final BlockData blockData = this.blockCache.getBlockData(position);
                if (blockData == BlockData.EMPTY) continue;
                final Material worldMaterial = SpigotConversionUtil.toBukkitBlockData(PacketUtils.getState(block)).getMaterial();
                if (blockData.worldBlockType() != worldMaterial && !worldMaterial.isAir() && worldMaterial != Material.MOVING_PISTON) {
                    this.blockCache.removeBlockData(position);
                    continue;
                }
                block.setBlockState(blockData.getNewState());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePlayerDigging(PacketReceiveEvent event, UUID world) {
        final WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
        if (!(event.getPlayer() instanceof final Player player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        final DiggingAction diggingAction = packet.getAction();
        final Vector3i blockPosition = packet.getBlockPosition();
        final Position position = Position.at(
                world,
                blockPosition.getX(),
                blockPosition.getY(),
                blockPosition.getZ()
        );
        final BlockData blockData = this.blockCache.getBlockData(position);
        if (!(blockData instanceof LogData)) return;
        if (diggingAction == DiggingAction.START_DIGGING) {
            PacketUtils.sendMiningFatigue(player);
            this.blockBreakManager.startBlockBreak(
                    player,
                    position,
                    blockData
            );
            return;
        }
        if (diggingAction == DiggingAction.CANCELLED_DIGGING) {
            this.blockBreakManager.cancelBlockBreak(player);
            PacketUtils.removeMiningFatigue(player);
            return;
        }
        if (diggingAction == DiggingAction.FINISHED_DIGGING) {
            this.blockBreakManager.cancelBlockBreak(player);
            PacketUtils.removeMiningFatigue(player);
        }
    }

}
