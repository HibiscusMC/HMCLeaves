package com.hibiscusmc.hmcleaves.paper.block;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.hibiscusmc.hmcleaves.paper.config.LeavesConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Consumer;

public final class LeavesBlock implements CustomBlock {

    private final String id;
    private final Material worldMaterial;
    private final CustomBlockState blockState;
    private final CustomBlockState waterloggedState;

    public LeavesBlock(String id, Material worldMaterial, WrappedBlockState defaultState) {
        this.id = id;
        this.worldMaterial = worldMaterial;
        final WrappedBlockState state = defaultState.clone();
        this.blockState = new CustomBlockState(this, state.getGlobalId());
        state.setWaterlogged(true);
        this.waterloggedState = new CustomBlockState(this, state.getGlobalId());
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public CustomBlockState getBlockStateFromWorldBlock(BlockData blockData) {
        if (!(blockData instanceof final Leaves leaves)) {
            return this.blockState;
        }
        if (leaves.isWaterlogged()) {
            return this.waterloggedState;
        }
        return this.blockState;
    }

    @Override
    public boolean placeBlock(LeavesConfig leavesConfig, BlockPlaceData placeData, Consumer<CustomBlockState> beforePlaceIfSuccessful) {
        final Location location = placeData.placeLocation();
        final Block block = location.getBlock();
        final boolean waterlog = block.getType() == Material.WATER;
        final Leaves leavesData = (Leaves) this.worldMaterial.createBlockData();
        leavesData.setWaterlogged(waterlog);
        final Player player = placeData.player();
        leavesData.setPersistent(player == null || !leavesConfig.isPlacingDecayable(player));
        if (waterlog) {
            beforePlaceIfSuccessful.accept(this.waterloggedState);
        } else {
            beforePlaceIfSuccessful.accept(this.blockState);
        }
        block.setBlockData(leavesData, true);
        return true;
    }

    @Override
    public Map<String, String> getPropertiesByName(WrappedBlockState state) {
        return Map.of(
                "distance", String.valueOf(state.getDistance()),
                "persistent", String.valueOf(state.isPersistent()),
                "waterlogged", String.valueOf(state.isWaterlogged())
        );
    }

    @Override
    public Material worldMaterial() {
        return this.worldMaterial;
    }

    @Override
    public String toString() {
        return "LeavesBlock{" +
                "id='" + this.id + '\'' +
                ", worldMaterial=" + this.worldMaterial +
                ", blockState=" + this.blockState +
                ", waterloggedState=" + this.waterloggedState +
                '}';
    }
}
