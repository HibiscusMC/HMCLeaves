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
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.cache.BlockCache;
import io.github.fisher2911.hmcleaves.cache.ChunkBlockCache;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.data.MineableData;
import io.github.fisher2911.hmcleaves.util.ChunkUtil;
import io.github.fisher2911.hmcleaves.util.ItemUtil;
import io.github.fisher2911.hmcleaves.util.LeafDropUtil;
import io.github.fisher2911.hmcleaves.world.ChunkPosition;
import io.github.fisher2911.hmcleaves.world.Position;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LeavesPacketListener extends PacketListenerAbstract {

    private final LeavesConfig leavesConfig;
    private final BlockCache blockCache;
    private final BlockBreakManager blockBreakManager;
    private final HMCLeaves plugin;
    private final Multimap<ChunkPosition, UUID> sentChunks;

//    private final Multimap<ChunkPosition, ChunkPacket> chunkPackets;

    public LeavesPacketListener(PacketListenerPriority priority, HMCLeaves plugin) {
        super(priority);
        this.plugin = plugin;
        this.leavesConfig = this.plugin.getLeavesConfig();
        this.blockCache = this.plugin.getBlockCache();
        this.blockBreakManager = this.plugin.getBlockBreakManager();
        this.sentChunks = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
//        this.chunkPackets = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
    }

    public LeavesPacketListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.leavesConfig = this.plugin.getLeavesConfig();
        this.blockCache = this.plugin.getBlockCache();
        this.blockBreakManager = this.plugin.getBlockBreakManager();
        this.sentChunks = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
//        this.chunkPackets = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
    }

//    private static record ChunkPacket(
//            WrapperPlayServerChunkData chunkData,
//            UUID player,
//            LocalDateTime timeSent,
//            UUID world,
//            int heightAdjustment
//    ) {
//
//    }

    private static final int HEIGHT_BELOW_ZERO = 64;

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof final Player player)) return;
        if (!this.leavesConfig.isWorldWhitelisted(player.getWorld())) return;
        final PacketTypeCommon packetType = event.getPacketType();
        if (packetType == PacketType.Play.Server.CHUNK_DATA) {
            final World world = player.getWorld();
            final int heightAdjustment = world.getMinHeight();
            this.handleChunkSend(event, world.getUID(), heightAdjustment < 0 ? HEIGHT_BELOW_ZERO : 0, player.getUniqueId());
            return;
        }
        if (packetType == PacketType.Play.Server.BLOCK_CHANGE) {
            this.handleBlockChange(event, player.getWorld().getUID());
            return;
        }
        if (packetType == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            this.handleMultiBlockChange(event, player.getWorld().getUID());
            return;
        }
        if (packetType == PacketType.Play.Server.PARTICLE) {
            this.handleFallParticles(event);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof final Player player)) return;
        if (!this.leavesConfig.isWorldWhitelisted(player.getWorld())) return;
        final PacketTypeCommon packetType = event.getPacketType();
        if (packetType == PacketType.Play.Client.PLAYER_DIGGING) {
            this.handlePlayerDigging(event, player.getWorld().getUID());
        }
    }

    private void handleChunkSend(PacketSendEvent event, UUID world, int heightAdjustment, UUID player) {
        final WrapperPlayServerChunkData packet = new WrapperPlayServerChunkData(event);
        final Column column = packet.getColumn();
        final int chunkX = column.getX();
        final int chunkZ = column.getZ();
        final ChunkPosition chunkPos = ChunkPosition.at(world, chunkX, chunkZ);
        this.sentChunks.put(chunkPos, player);
        ChunkBlockCache chunkCache = this.blockCache.getChunkBlockCache(chunkPos);
        if (chunkCache == null) {
//            this.chunkPackets.put(chunkPos, new ChunkPacket(packet, player, LocalDateTime.now(), world, heightAdjustment));
//            event.setCancelled(true);
            return;
//            chunkCache = this.blockCache.addChunkCache(chunkPos);
        }
        this.editChunkPacket(packet, world, heightAdjustment);
    }

    private void editChunkPacket(WrapperPlayServerChunkData packet, UUID world, int heightAdjustment) {
        final Column column = packet.getColumn();
        final int chunkX = column.getX();
        final int chunkZ = column.getZ();
        final BaseChunk[] chunks = column.getChunks();
        final ChunkPosition chunkPos = ChunkPosition.at(world, chunkX, chunkZ);
        final ChunkBlockCache chunkCache = this.blockCache.getChunkBlockCache(chunkPos);
        if (chunkCache == null) return;
        for (var entry : chunkCache.getBlockDataMap().entrySet()) {
            final var position = entry.getKey();
            final int chunkLevel = position.y() / 16 + heightAdjustment / 16;
            final BaseChunk chunk = chunks[position.y() < 0 ? chunkLevel - 1 : chunkLevel];
            final var blockData = entry.getValue();
            final int actualX = ChunkUtil.getCoordInChunk(position.x());
            final int actualY = Math.abs(ChunkUtil.getCoordInChunk(position.y()));
            final int actualZ = ChunkUtil.getCoordInChunk(position.z());
            final var actualState = chunk.get(actualX, actualY, actualZ);
            final Material worldMaterial = SpigotConversionUtil.toBukkitBlockData(actualState).getMaterial();
            chunk.set(
                    PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                    actualX,
                    actualY,
                    actualZ,
                    blockData.getNewState(worldMaterial).getGlobalId()
            );
        }
    }

    public Collection<UUID> getPlayersChunkSentTo(ChunkPosition chunkPosition) {
        return this.sentChunks.get(chunkPosition);
    }

    public void removeSentChunks(UUID playerUUID) {
        this.sentChunks.values().remove(playerUUID);
    }

//    public void resendChunk(ChunkPosition chunkPos) {
//        LocalDateTime timeSent = null;
//        for (ChunkPacket chunkPacket : this.chunkPackets.get(chunkPos)) {
//            if (chunkPacket == null) continue;
//            this.editChunkPacket(chunkPacket.chunkData, chunkPacket.world, chunkPacket.heightAdjustment);
//            final Player player = Bukkit.getPlayer(chunkPacket.player);
//            if (player == null) continue;
//            timeSent = chunkPacket.timeSent;
//            PacketEvents.getAPI().getPlayerManager().sendPacket(player, chunkPacket.chunkData);
//        }
//        if (timeSent != null) {
//            Debugger.getInstance().logTimeBetweenChunkLoadAndSend(chunkPos, timeSent, LocalDateTime.now());
//        }
//        this.chunkPackets.removeAll(chunkPos);
//    }

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
            final Material worldMaterial = SpigotConversionUtil.toBukkitBlockData(packet.getBlockState()).getMaterial();
            if (blockData == BlockData.EMPTY) return;
            final WrappedBlockState sendState = blockData.getNewState(worldMaterial);
//            final Material sendMaterial = SpigotConversionUtil.toBukkitBlockData(sendState).getMaterial();
            if (!blockData.isWorldTypeSame(worldMaterial) /*worldMaterial != sendMaterial && blockData.worldBlockType() != worldMaterial*//*&& !worldMaterial.isAir()*/ && worldMaterial != Material.MOVING_PISTON) {
//                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
//                    if (worldMaterial.isAir()) return;
//                    if (!blockData.isWorldTypeSame(worldMaterial)) {
                this.blockCache.removeBlockData(position);
                LeafDropUtil.addToDropPositions(this.blockCache, position, blockData);
//                    }
//                }, 1);
                return;
            }
            final WrapperPlayServerBlockChange newPacket = new WrapperPlayServerBlockChange(
                    packet.getBlockPosition(),
                    sendState.getGlobalId()
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
                final WrappedBlockState sendState = blockData.getNewState(worldMaterial);
                if (!blockData.isWorldTypeSame(worldMaterial) && worldMaterial != Material.MOVING_PISTON) {
//                    if (worldMaterial.isAir()) continue;
//                    Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
//                        if (!blockData.isWorldTypeSame(worldMaterial)) {
                    this.blockCache.removeBlockData(position);
                    LeafDropUtil.addToDropPositions(this.blockCache, position, blockData);
//                        }
//                    }, 1);
                    continue;
                }
                block.setBlockState(sendState);
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
        if (!(blockData instanceof final MineableData mineableData) || mineableData.blockBreakModifier() == null)
            return;
        if (diggingAction == DiggingAction.START_DIGGING) {
            PacketUtils.sendMiningFatigue(player);
            this.blockBreakManager.startBlockBreak(
                    player,
                    position,
                    (MineableData & BlockData) mineableData
            );
            return;
        }
        final Material heldMaterial = player.getInventory().getItemInMainHand().getType();
        if (diggingAction == DiggingAction.CANCELLED_DIGGING) {
            this.blockBreakManager.cancelBlockBreak(player);
            if (!ItemUtil.isQuickMiningTool(heldMaterial)) {
                PacketUtils.removeMiningFatigue(player);
            }
            return;
        }
        if (diggingAction == DiggingAction.FINISHED_DIGGING) {
            this.blockBreakManager.cancelBlockBreak(player);
            if (!ItemUtil.isQuickMiningTool(heldMaterial)) {
                PacketUtils.removeMiningFatigue(player);
            }
        }
    }

    private void handleFallParticles(PacketSendEvent event) {
//        final WrapperPlayServerParticle packet = new WrapperPlayServerParticle(event);
//        final Particle particle = packet.getParticle();
//        if (particle.getType() != ParticleTypes.BLOCK) return;
//        if (!(event.getPlayer() instanceof final Player player)) return;
//        final Vector3d position = packet.getPosition();
//        final Vector3i below = position.subtract(0, 0.1, 0).toVector3i();
//        final World world = player.getWorld();
//        final BlockData blockData = this.blockCache.getBlockData(Position.at(
//                world.getUID(),
//                below.x,
//                below.y,
//                below.z
//        ));
//        if (!(blockData instanceof MineableData)) return;
//        final ParticleBlockStateData particleBlockStateData = new ParticleBlockStateData(
//                blockData.getNewState(null)
//        );
//        particle.setData(particleBlockStateData);
    }

}
