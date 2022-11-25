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
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import io.github.fisher2911.hmcleaves.Config;
import io.github.fisher2911.hmcleaves.FakeLeafState;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafCache;
import io.github.fisher2911.hmcleaves.util.ChunkUtil;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BlockListener {

    private final HMCLeaves plugin;
    private final LeafCache leafCache;
    private final Config config;

    public BlockListener(HMCLeaves plugin, LeafCache cache) {
        this.plugin = plugin;
        this.leafCache = cache;
        this.config = plugin.config();
    }

    public void register() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
            }

            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (!(event.getPlayer() instanceof final Player player)) return;
                final UUID world = player.getWorld().getUID();
                if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
                    BlockListener.this.handleChunkChange(world, new WrapperPlayServerChunkData(event));
                    return;
                }
                if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
                    BlockListener.this.handleSingleBlockChange(world, event, new WrapperPlayServerBlockChange(event));
                    return;
                }
                if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
                    BlockListener.this.handleMultiBlockChange(world, new WrapperPlayServerMultiBlockChange(event));
                    return;
                }
            }
        });
    }

    private void handleChunkChange(UUID world, WrapperPlayServerChunkData packet) {
        final Column column = packet.getColumn();
        final BaseChunk[] chunks = column.getChunks();
        for (int i = 0; i < chunks.length; i++) {
            final BaseChunk chunk = chunks[i];
            final int worldY = i * 16 - 64;
            final Position2D chunkPos = new Position2D(world, column.getX(), column.getZ());

            for (var entry : this.leafCache.getOrAddChunkData(chunkPos).entrySet()) {
                final Position position = entry.getKey();
                final int y = position.y();
                final int difference = y - worldY;
                if (difference < 0 || difference > 15) continue;
                final FakeLeafState fakeLeafState = entry.getValue();
                final WrappedBlockState state = fakeLeafState.state();
                final int actualY = Math.abs(ChunkUtil.getCoordInChunk(position.y()));
                try {
                    chunk.set(
                            PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                            position.x(),
                            actualY,
                            position.z(),
                            state.getGlobalId()
                    );
                } catch (Exception e) {
                    HMCLeaves.getPlugin(HMCLeaves.class).getLogger().severe("Could not set blocks at " + position.x() + ", " + actualY + ", " + position.z());
                }
            }
        }
    }

    private void handleSingleBlockChange(UUID world, PacketSendEvent event, WrapperPlayServerBlockChange packet) {
        final Vector3i position = packet.getBlockPosition();
        final int x = position.getX();
        final int y = position.getY();
        final int z = position.getZ();
        final Location location = new Location(Bukkit.getWorld(world), x, y, z);
        final WrappedBlockState state = packet.getBlockState();
        this.checkBlock(location, state);
        final ItemType itemType = ItemTypes.getTypePlacingState(state.getType());
        if (itemType == null || !BlockTags.LEAVES.contains(itemType.getPlacedType()) || !(event.getPlayer() instanceof Player)) {
            return;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                event.getPlayer(),
                new WrapperPlayServerBlockChange(packet.getBlockPosition(), state.getGlobalId())
        );
        event.setCancelled(true);
    }

    private void handleMultiBlockChange(UUID world, WrapperPlayServerMultiBlockChange packet) {
        final var blocks = packet.getBlocks();
        for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocks) {
            final Location location = new Location(Bukkit.getWorld(world), block.getX(), block.getY(), block.getZ());
            final WrappedBlockState state = PacketHelper.getState(block);
            this.checkBlock(location, state);
            block.setBlockState(state);
        }
    }

    private void checkBlock(
            Location location,
            WrappedBlockState state
    ) {
        final ChunkSnapshot snapshot = ChunkUtil.getSnapshotAt(location);
        if (snapshot == null) return;
        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();
        final BlockData blockData = snapshot.getBlockData(ChunkUtil.getCoordInChunk(x), y, ChunkUtil.getCoordInChunk(z));
        if (this.leafCache.isLogAt(location)) return;
        if (this.config.isLogBlock(blockData)) return;
        final FakeLeafState fakeLeafState = this.leafCache.getAt(location);
        if (fakeLeafState == null) return;
        if (!BlockTags.LEAVES.contains(state.getType())) return;
        state.setPersistent(fakeLeafState.state().isPersistent());
        state.setDistance(fakeLeafState.state().getDistance());
    }

}
