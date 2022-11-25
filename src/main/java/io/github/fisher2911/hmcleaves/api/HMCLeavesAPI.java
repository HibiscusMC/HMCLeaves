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

package io.github.fisher2911.hmcleaves.api;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.jeff_media.customblockdata.CustomBlockData;
import io.github.fisher2911.hmcleaves.FakeLeafState;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafData;
import io.github.fisher2911.hmcleaves.packet.PacketHelper;
import io.github.fisher2911.hmcleaves.util.ChunkUtil;
import io.github.fisher2911.hmcleaves.util.LeafUpdater;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.fisher2911.hmcleaves.util.Position;
import io.github.fisher2911.hmcleaves.util.Position2D;
import org.bukkit.Location;
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
            Location location,
            LeafData serverData
    ) {
        setLeafAt(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), serverData);
    }

    public void setLeafAt(
            World world,
            int x,
            int y,
            int z,
            LeafData serverData
    ) {
        final Position2D chunkPos = new Position2D(world.getUID(), x >> 4, z >> 4);
        final Position position = new Position(ChunkUtil.getCoordInChunk(x), y, ChunkUtil.getCoordInChunk(z));
        final FakeLeafState fakeLeafState = this.plugin.config().getDefaultState(serverData.material());
        final var state = fakeLeafState.state();
        state.setDistance(serverData.distance());
        state.setPersistent(serverData.persistent());
        final FakeLeafState newState = new FakeLeafState(state, serverData.actuallyPersistent(), 7);
        this.plugin.getLeafCache().addData(chunkPos, position, newState);
        final Block block = world.getBlockAt(x, y, z);
        block.setType(serverData.material(), false);
        final CustomBlockData customBlockData = new CustomBlockData(block, plugin);
        PDCUtil.setPersistent(customBlockData, serverData.persistent());
        PDCUtil.setDistance(customBlockData, (byte) serverData.distance());
        PDCUtil.setActualPersistent(customBlockData, serverData.actuallyPersistent());
        PDCUtil.setActualDistance(customBlockData, (byte) 7);
        LeafUpdater.scheduleTick(new Location(world, x, y, z));

        if (block.getBlockData() instanceof Leaves leaves) {
            leaves.setPersistent(serverData.persistent() || serverData.distance() < 7);
            block.setBlockData(leaves, false);
        }
    }

    public void removeLeafAt(World world, int x, int y, int z, boolean sendToPlayers) {
        final Position2D chunkPos = new Position2D(world.getUID(), x >> 4, z >> 4);
        final Position position = new Position(ChunkUtil.getCoordInChunk(x), y, ChunkUtil.getCoordInChunk(z));
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
