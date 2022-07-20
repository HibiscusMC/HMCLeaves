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
import io.github.fisher2911.hmcleaves.LeafCache;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BlockListener {

    private final LeafCache leafCache;

    public BlockListener(LeafCache cache) {
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
                final int actualY = position.y() % 16;
                chunk.set(
                        PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                        position.x(),
                        actualY,
                        position.z(),
                        state.getGlobalId()
                );
            }
        }
    }

    private void registerSingleChangeListener(UUID world, PacketSendEvent event, WrapperPlayServerBlockChange packet) {
        final Vector3i position = packet.getBlockPosition();
        final int x = position.getX();
        final int z = position.getZ();
        final Position2D chunkPos = new Position2D(world, x >> 4, z >> 4);
        final Position position2D = new Position(x % 16, position.getY(), z % 16);
        final var state = this.leafCache.getAt(chunkPos, position2D);
        if (state == null) return;
        event.setCancelled(true);
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                event.getPlayer(),
                new WrapperPlayServerBlockChange(packet.getBlockPosition(), state.clone().getGlobalId())
        );
    }

    private void registerMultiChangeListener(UUID world, WrapperPlayServerMultiBlockChange packet) {
        final var blocks = packet.getBlocks();
        for (WrapperPlayServerMultiBlockChange.EncodedBlock block : blocks) {
            final int x = block.getX();
            final int y = block.getY();
            final int z = block.getZ();
            final int chunkX = x >> 4;
            final int chunkZ = z >> 4;
            final Position2D chunkPos = new Position2D(world, chunkX, chunkZ);
            final Position position = new Position(x % 16, y, z % 16);
            final WrappedBlockState state = this.leafCache.getAt(chunkPos, position);
            if (state == null) continue;
            block.setBlockState(state);
        }
    }
}
