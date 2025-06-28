package com.hibiscusmc.hmcleaves.paper.block;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.google.common.base.Preconditions;
import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.paper.util.WorldUtil;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class LogBlock implements CustomBlock {

    private final String id;
    private final Material worldMaterial;
    private final Map<Axis, CustomBlockState> axisStates;

    public LogBlock(String id, Material worldMaterial, Map<Axis, WrappedBlockState> axisStates) {
        Preconditions.checkArgument(axisStates.size() == Axis.values().length, "LogBlock must have states for all axe: " + Arrays.toString(Axis.values()));
        this.id = id;
        this.worldMaterial = worldMaterial;
        this.axisStates = new HashMap<>();
        for (Map.Entry<Axis, WrappedBlockState> entry : axisStates.entrySet()) {
            this.axisStates.put(entry.getKey(), new CustomBlockState(this, entry.getValue().getGlobalId()));
        }
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public CustomBlockState getBlockStateFromWorldBlock(BlockData blockData) {
        if (!(blockData instanceof final Orientable orientable)) {
            return this.axisStates.get(Axis.Y);
        }
        final Axis axis = orientable.getAxis();
        return this.axisStates.get(axis);
    }

    @Override
    public boolean placeBlock(LeavesConfig leavesConfig, BlockPlaceData placeData, Consumer<CustomBlockState> beforePlaceIfSuccessful) {
        final Location location = placeData.placeLocation();
        final Block block = location.getBlock();
        final Orientable orientable = (Orientable) this.worldMaterial.createBlockData();
        final Axis axis = WorldUtil.axisFromBlockFace(placeData.clickedFace());
        orientable.setAxis(axis);
        beforePlaceIfSuccessful.accept(this.axisStates.get(axis));
        block.setBlockData(orientable, true);
        return true;
    }

    @Override
    public Map<String, Object> getPropertiesByName(WrappedBlockState state) {
        return Map.of(
                "note", state.getNote(),
                "instrument", state.getInstrument(),
                "powered", state.isPowered()
        );
    }

    @Override
    public Material worldMaterial() {
        return this.worldMaterial;
    }

    @Override
    public String toString() {
        return "LogBlock{" +
                "id='" + this.id + '\'' +
                ", worldMaterial=" + this.worldMaterial +
                ", axisStates=" + this.axisStates +
                '}';
    }
}
