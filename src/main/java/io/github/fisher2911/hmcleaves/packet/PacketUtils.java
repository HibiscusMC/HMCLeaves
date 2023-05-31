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
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.entity.Player;

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

}
