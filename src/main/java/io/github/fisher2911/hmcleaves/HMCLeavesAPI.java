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
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;

public class HMCLeavesAPI {

    private final HMCLeaves plugin;

    private HMCLeavesAPI(final HMCLeaves plugin) {
        this.plugin = plugin;
    }

    private static final HMCLeavesAPI INSTANCE = new HMCLeavesAPI(HMCLeaves.getPlugin(HMCLeaves.class));

    public static HMCLeavesAPI getInstance() {
        return INSTANCE;
    }

    public void setLeafAt(
            World world,
            int x,
            int y,
            int z,
            String id,
            LeafData serverData,
            boolean sendToPlayers
    ) {
        final LeafItem leafItem = this.plugin.config().getItem(id);
        if (leafItem == null) return;
        final Position2D chunkPos = new Position2D(world.getUID(), x >> 4, z >> 4);
        final Position position = new Position(PositionUtil.getCoordInChunk(x), y, PositionUtil.getCoordInChunk(z));
        final LeafData leafData = leafItem.leafData();
        final FakeLeafState fakeLeafState = this.plugin.config().getDefaultState(leafData.material());
        final var state = fakeLeafState.state();
        state.setDistance(leafData.distance());
        state.setPersistent(leafData.persistent());
        this.plugin.getLeafCache().addData(chunkPos, position, new FakeLeafState(state, fakeLeafState.actuallyPersistent(), 7));
        final Block block = world.getBlockAt(x, y, z);
        block.setType(serverData.material());
        if (block.getBlockData() instanceof Leaves leaves) {
            leaves.setDistance(serverData.distance());
            leaves.setPersistent(serverData.persistent());
            block.setBlockData(leaves);
        }
        if (sendToPlayers) {
            PacketHelper.sendBlock(world.getUID(), x, y, z, state);
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
