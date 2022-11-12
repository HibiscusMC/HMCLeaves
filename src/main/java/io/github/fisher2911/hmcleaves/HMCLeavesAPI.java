package io.github.fisher2911.hmcleaves;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import io.github.fisher2911.hmcleaves.packet.PacketHelper;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import io.github.fisher2911.hmcleaves.util.PositionUtil;
import org.bukkit.Material;
import org.bukkit.World;

public class HMCLeavesAPI {

    private final HMCLeaves plugin;

    private HMCLeavesAPI(final HMCLeaves plugin) {
        this.plugin = plugin;
    }

    private static final HMCLeavesAPI INSTANCE = new HMCLeavesAPI(HMCLeaves.getPlugin(HMCLeaves.class));

    public static HMCLeavesAPI getInstance() {
        return INSTANCE;
    }

    public void setLeafAt(World world, int x, int y, int z, String id, boolean sendToPlayers) {
        final LeafItem leafItem = this.plugin.config().getItem(id);
        if (leafItem == null) return;
        final Position2D chunkPos = new Position2D(world.getUID(), x >> 4, z >> 4);
        final Position position = new Position(PositionUtil.getCoordInChunk(x), y, PositionUtil.getCoordInChunk(z));
        final LeafData leafData = leafItem.leafData();
        final var state = this.plugin.config().getDefaultState(leafData.material()).clone();
        state.setDistance(leafData.distance());
        state.setPersistent(leafData.persistent());
        this.plugin.getLeafCache().addData(chunkPos, position, state);
        world.getBlockAt(x, y, z).setType(leafData.material());
        if (sendToPlayers) {
            PacketHelper.sendLeaf(world.getUID(), x, y, z, state);
        }
    }

    public void removeLeafAt(World world, int x, int y, int z, boolean sendToPlayers) {
        final Position2D chunkPos = new Position2D(world.getUID(), x >> 4, z >> 4);
        final Position position = new Position(PositionUtil.getCoordInChunk(x), y, PositionUtil.getCoordInChunk(z));
        this.plugin.getLeafCache().remove(chunkPos, position);
        world.getBlockAt(x, y, z).setType(Material.AIR);
        if (sendToPlayers) {
            PacketHelper.sendBlock(world.getUID(), x, y, z, WrappedBlockState.getDefaultState(
                    PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(),
                    StateTypes.getByName(Material.AIR.toString().toLowerCase())
            ));
        }
    }

}
