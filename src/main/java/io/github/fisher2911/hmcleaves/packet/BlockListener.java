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
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
                    BlockListener.this.registerChunkListener(world, new WrapperPlayServerChunkData(event));
                    return;
                }
                if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
                    BlockListener.this.registerSingleChangeListener(world, event, new WrapperPlayServerBlockChange(event));
                    return;
                }
                if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
                    BlockListener.this.registerMultiChangeListener(world, new WrapperPlayServerMultiBlockChange(event));
                    return;
                }
            }
        });
    }

    private void registerChunkListener(UUID world, WrapperPlayServerChunkData packet) {
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
//                final WrappedBlockState state = entry.getValue();
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

    private void registerSingleChangeListener(UUID world, PacketSendEvent event, WrapperPlayServerBlockChange packet) {
        final Vector3i position = packet.getBlockPosition();
        final int x = position.getX();
        final int y = position.getY();
        final int z = position.getZ();
        final Location location = new Location(Bukkit.getWorld(world), x, y, z);
        final WrappedBlockState state = packet.getBlockState();
        final Map<Location, BlockData> toUpdate = new HashMap<>();
        final Set<Location> addedLogs = new HashSet<>();
        final Set<Location> removedLogs = new HashSet<>();
        this.checkBlock(location, state, toUpdate, addedLogs, removedLogs);
        final ItemType itemType = ItemTypes.getTypePlacingState(state.getType());
        if (!toUpdate.isEmpty()) {
//        Bukkit.broadcastMessage("Removed logs size: " + removedLogs.size());
//        Bukkit.broadcastMessage("Added logs size: " + addedLogs.size());
//        LeafUpdater.doUpdate(toUpdate, addedLogs, removedLogs);
//            for (Location loc : toUpdate.keySet()) {
//                LeafUpdater3.scheduleTick(loc);
//            }
        }
        if (itemType == null || !BlockTags.LEAVES.contains(itemType.getPlacedType()) || !(event.getPlayer() instanceof final Player player)) {
            return;
        }
//        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                event.getPlayer(),
                new WrapperPlayServerBlockChange(packet.getBlockPosition(), state.getGlobalId())
        );
        event.setCancelled(true);
//        });
    }

    private void registerMultiChangeListener(UUID world, WrapperPlayServerMultiBlockChange packet) {
        final var blocks = packet.getBlocks();
        final Map<Location, BlockData> toUpdate = new HashMap<>();
        final Set<Location> addedLogs = new HashSet<>();
        final Set<Location> removedLogs = new HashSet<>();
        for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocks) {
            final Location location = new Location(Bukkit.getWorld(world), block.getX(), block.getY(), block.getZ());
            final WrappedBlockState state = PacketHelper.getState(block);
            this.checkBlock(location, state, toUpdate, addedLogs, removedLogs);
            block.setBlockState(state);
        }
        if (toUpdate.isEmpty()) return;
//        Bukkit.broadcastMessage("Removed logs size: " + removedLogs.size());
//        Bukkit.broadcastMessage("Added logs size: " + addedLogs.size());
//        LeafUpdater.doUpdate(toUpdate, addedLogs, removedLogs);
//        for (Location loc : toUpdate.keySet()) {
//            LeafUpdater3.scheduleTick(loc);
//        }
    }

    private void checkBlock(
            Location location,
            WrappedBlockState state,
            Map<Location, BlockData> toUpdate,
            Set<Location> addedLogs,
            Set<Location> removedLogs
    ) {
        final ChunkSnapshot snapshot = ChunkUtil.getSnapshotAt(location);
        if (snapshot == null) return;
        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();
        final ItemType itemType = ItemTypes.getTypePlacingState(state.getType());
        final BlockData blockData = snapshot.getBlockData(ChunkUtil.getCoordInChunk(x), y, ChunkUtil.getCoordInChunk(z));
        if (this.leafCache.isLogAt(location)) {
            if (this.config.isLogBlock(blockData)) return;
//            this.leafCache.removeLogAt(location);
//            removedLogs.add(location);
            toUpdate.put(location, blockData);
            return;
        }
        if (this.config.isLogBlock(blockData)) {
//                if (leafCache.isLogAt(location)) continue;
//            this.leafCache.setLogAt(location);
            toUpdate.put(location, blockData);
//            addedLogs.add(location);
            return;
        }
        final FakeLeafState fakeLeafState = this.leafCache.getAt(location);
        if (fakeLeafState == null) {
            if (!BlockTags.LEAVES.contains(state.getType())) return;
            final FakeLeafState newState = this.leafCache.createAtOrGetAndSet(
                    location,
                    SpigotConversionUtil.toBukkitItemMaterial(itemType)
            );
            if (blockData instanceof final Leaves leaves) {
                newState.actuallyPersistent(leaves.isPersistent());
                newState.actualDistance(leaves.getDistance());
            }
//            state.setPersistent(newState.state().isPersistent());
//            state.setDistance(newState.state().getDistance());
            toUpdate.put(location, blockData);
            return;
        }
        if (!BlockTags.LEAVES.contains(state.getType())) {
//            this.leafCache.remove(location);
            // we only really care about checking for updates if it can
            // save another leaf from decaying
            if (fakeLeafState.actualDistance() < 6) {
                toUpdate.put(location, blockData);
            }
            return;
        }
        state.setPersistent(fakeLeafState.state().isPersistent());
        state.setDistance(fakeLeafState.state().getDistance());
    }

}
