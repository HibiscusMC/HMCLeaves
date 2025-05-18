package com.hibiscusmc.hmcleaves.paper.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.hibiscusmc.hmcleaves.common.block.LeavesBlock;
import com.hibiscusmc.hmcleaves.common.util.PositionUtils;
import com.hibiscusmc.hmcleaves.common.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.common.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.common.world.Position;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class PacketUtil {

    private PacketUtil() {
        throw new UnsupportedOperationException();
    }


    public static void sendMultiBlockChange(
            LeavesChunk leavesChunk,
            Collection<? extends Player> players
    ) {
        if (players.isEmpty()) {
            return;
        }

        final var sections = leavesChunk.getSections().values();
        for (var section : sections) {
            final var blocks = section.getBlocks();
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
            final var chunkSectionPosition = section.sectionPosition();
            for (Player player : players) {
                PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                        player,
                        new WrapperPlayServerMultiBlockChange(
                                new Vector3i(chunkSectionPosition.x(), chunkSectionPosition.y(), chunkSectionPosition.z()),
                                false,
                                encodedBlocks
                        )
                );
            }
        }
    }

    public static void sendSingleBlockChange(
            WrappedBlockState state,
            Position position,
            Collection<? extends Player> players
    ) {
        if (players.isEmpty()) {
            return;
        }

        for (Player player : players) {
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                    player,
                    new WrapperPlayServerBlockChange(
                            new Vector3i(position.x(), position.y(), position.z()),
                            state.getGlobalId()
                    )
            );
        }
    }

    public static void sendArmSwing(Player player) {
        PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                player,
                new WrapperPlayServerEntityAnimation(
                        player.getEntityId(),
                        WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                )
        );
    }

}
