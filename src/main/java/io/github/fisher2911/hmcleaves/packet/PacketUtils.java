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
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRemoveEntityEffect;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;

public class PacketUtils {

    public static WrappedBlockState getState(WrapperPlayServerMultiBlockChange.EncodedBlock block) {
        return block.getBlockState(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
    }

    public static void sendBlockData(BlockData blockData, Position position, Player player) {
        final WrappedBlockState state = blockData.getNewState();
        final WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(
                new Vector3i(position.x(), position.y(), position.z()),
                state.getGlobalId()
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
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

    public static void sendArmSwing(Player playerToSwing, Collection<? extends Player> players) {
        for (Player player : players) {
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                    player,
                    new WrapperPlayServerEntityAnimation(
                            playerToSwing.getEntityId(),
                            WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM
                    )
            );
        }
    }

}
