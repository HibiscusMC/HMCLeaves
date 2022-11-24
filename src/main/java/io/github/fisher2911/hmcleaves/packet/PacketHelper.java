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
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public class PacketHelper {

    public static void sendBlock(UUID world, int x, int y, int z, WrappedBlockState state, Collection<? extends Player> players) {
        sendBlock(world, x, y, z, state, players.toArray(new Player[0]));
    }

    public static void sendBlock(UUID world, int x, int y, int z, WrappedBlockState state, Player... players) {
        for (Player player : players) {
            if (player.getWorld().getUID() != world) continue;
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                    player,
                    new WrapperPlayServerBlockChange(
                            new Vector3i(x, y, z),
                            state.getGlobalId()
                    )
            );
        }
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

    public static WrappedBlockState getState(WrapperPlayServerMultiBlockChange.EncodedBlock block) {
        return block.getBlockState(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion());
    }

}
