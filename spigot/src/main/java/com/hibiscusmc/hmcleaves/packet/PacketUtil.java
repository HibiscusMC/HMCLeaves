package com.hibiscusmc.hmcleaves.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.hibiscusmc.hmcleaves.util.PositionUtils;
import com.hibiscusmc.hmcleaves.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.world.LeavesChunk;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class PacketUtil {

    private PacketUtil() {
    }

    public static void sendMultiBlockChange(
            ChunkPosition chunkPosition,
            LeavesChunk leavesChunk,
            Collection<? extends Player> players
    ) {
        if (players.isEmpty()) {
            return;
        }

        final var blocks = leavesChunk.getBlocks();
        final var encodedBlocks = new WrapperPlayServerMultiBlockChange.EncodedBlock[blocks.size()];
        int i = 0;
        for (var entry : blocks.entrySet()) {
            final var position = entry.getKey();
            encodedBlocks[i++] = new WrapperPlayServerMultiBlockChange.EncodedBlock(
                    entry.getValue().blockState().get(),
                    PositionUtils.coordToCoordInChunk(position.x()),
                    PositionUtils.coordToCoordInChunk(position.y()),
                    PositionUtils.coordToCoordInChunk(position.z())
            );
        }
        for (Player player : players) {
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                    player,
                    new WrapperPlayServerMultiBlockChange(
                            new Vector3i(chunkPosition.x(), chunkPosition.y(), chunkPosition.z()),
                            false,
                            encodedBlocks
                    )
            );
        }
    }
}
