package io.github.fisher2911.hmcleaves.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import org.bukkit.entity.Player;

public class PacketHelper {

    public static void sendLeaf(int x, int y, int z, WrappedBlockState state, Player... players) {
        for (Player player : players) {
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                    player,
                    new WrapperPlayServerBlockChange(
                            new Vector3i(x, y, z),
                            state.getGlobalId()
                    )
            );
        }
    }

    public static void sendBlock(int x, int y, int z, WrappedBlockState state, Player... players) {
        for (Player player : players) {
            PacketEvents.getAPI().getPlayerManager().sendPacketSilently(
                    player,
                    new WrapperPlayServerBlockChange(
                            new Vector3i(x, y, z),
                            state.getGlobalId()
                    )
            );
        }
    }

}
