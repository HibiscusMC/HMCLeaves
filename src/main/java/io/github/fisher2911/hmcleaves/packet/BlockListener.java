package io.github.fisher2911.hmcleaves.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafCache;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import io.github.fisher2911.hmcleaves.util.PositionUtil;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public class BlockListener {

    private final HMCLeaves plugin;
    private final LeafCache leafCache;

    public BlockListener(HMCLeaves plugin, LeafCache cache) {
        this.plugin = plugin;
        this.leafCache = cache;
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
                final WrappedBlockState state = entry.getValue();
                final int actualY = Math.abs(PositionUtil.getCoordInChunk(position.y()));
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
        final Position2D chunkPos = new Position2D(world, x >> 4, z >> 4);
        final Position position2D = new Position(PositionUtil.getCoordInChunk(x), position.getY(), PositionUtil.getCoordInChunk(z));
        var state = this.leafCache.getAt(chunkPos, position2D);
//        final Block b = Bukkit.getWorld(world).getBlockAt(x, y, z);
//        if (!Tag.LEAVES.isTagged(b.getType())) {
//            if (state != null) this.leafCache.remove(chunkPos, position2D);
//            return;
//        }
        final var newState = packet.getBlockState();
        if (!newState.getType().getName().toUpperCase(Locale.ROOT).contains("LEAVES")) {
            this.leafCache.remove(chunkPos, position2D);
            return;
        }
        if (state == null) {
            state = newState.clone();
            if (!state.getType().getName().toUpperCase(Locale.ROOT).contains("LEAVES")) {
                return;
            }
            this.plugin.config().setDefaultState(state);
            this.leafCache.addData(chunkPos, position2D, state.clone());
        } else {
            state = state.clone();
        }
//        packet.getBlockState().setDistance(state.getDistance());
//        packet.getBlockState().setPersistent(state.isPersistent());
        event.setCancelled(true);
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                event.getPlayer(),
                new WrapperPlayServerBlockChange(packet.getBlockPosition(), state.getGlobalId())
        );
    }

    private void registerMultiChangeListener(UUID world, WrapperPlayServerMultiBlockChange packet) {
        final var blocks = packet.getBlocks();
        for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocks) {
            final int x = block.getX();
            final int y = block.getY();
            final int z = block.getZ();

            final Position2D chunkPos = new Position2D(world, packet.getChunkPosition().getX(), packet.getChunkPosition().getZ());
            final Position position = new Position(PositionUtil.getCoordInChunk(x), y, PositionUtil.getCoordInChunk(z));
            WrappedBlockState state = this.leafCache.getAt(chunkPos, position);
//            final Block b = Bukkit.getWorld(world).getBlockAt(x, y, z);
//            if (!Tag.LEAVES.isTagged(b.getType())) {
//                if (state != null) this.leafCache.remove(chunkPos, position);
//                return;
//            }
            final WrappedBlockState newState = block.getBlockState(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion()).clone();
            if (!newState.getType().getName().toUpperCase().contains("LEAVES")) {
                this.leafCache.remove(chunkPos, position);
                continue;
            }
            if (state == null) {
//                state = this.plugin.config().getDefaultState(b.getType()).clone();
                this.plugin.config().setDefaultState(newState);
                state = newState;
                this.leafCache.addData(chunkPos, position, state);
            }
            block.setBlockState(state);
        }
    }

}
