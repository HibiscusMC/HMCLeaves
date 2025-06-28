package com.hibiscusmc.hmcleaves.paper.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRemoveEntityEffect;
import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.block.LogBlock;
import com.hibiscusmc.hmcleaves.paper.util.PositionUtils;
import com.hibiscusmc.hmcleaves.paper.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.paper.world.Position;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.logging.Level;

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
                        entry.getValue().getBlockState(),
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

    public static void sendBlockBreakAnimation(Player player, Location location, int entityId, byte damage) {
        final WrapperPlayServerBlockBreakAnimation packet = new WrapperPlayServerBlockBreakAnimation(
                entityId,
                new Vector3i(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                damage
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(
                player,
                packet
        );
    }

    public static void sendBlockBroken(Player player, Position position, int blockId) {
        final WrapperPlayServerWorldEvent packet = new WrapperPlayServerWorldEvent(
                2001,
                new Vector3i(position.x(), position.y(), position.z()),
                blockId,
                false
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(
                player,
                packet
        );
    }

    public static void sendMiningFatigue(Player player) {
        final WrapperPlayServerEntityEffect packet = new WrapperPlayServerEntityEffect(
                player.getEntityId(),
                PotionTypes.MINING_FATIGUE,
                -1,
                Integer.MAX_VALUE,
                (byte) 0
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(
                player,
                packet
        );
    }

    public static void removeMiningFatigue(Player player) {
        final WrapperPlayServerRemoveEntityEffect packet = new WrapperPlayServerRemoveEntityEffect(
                player.getEntityId(),
                PotionTypes.MINING_FATIGUE
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(
                player,
                packet
        );
    }

}
