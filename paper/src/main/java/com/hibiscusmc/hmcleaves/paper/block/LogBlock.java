package com.hibiscusmc.hmcleaves.paper.block;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.google.common.base.Preconditions;
import com.hibiscusmc.hmcleaves.paper.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.paper.util.WorldUtil;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class LogBlock implements CustomBlock {

    private static final Set<Material> STRIPPED_LOGS = EnumSet.copyOf(Arrays.stream(Material.values())
            .filter(Tag.LOGS::isTagged)
            .filter(log -> log.name().contains("STRIPPED_"))
            .collect(Collectors.toSet()));

    private final String id;
    private final Material worldMaterial;
    private final Map<Axis, CustomBlockState> axisStates;
    private final Map<Axis, CustomBlockState> strippedAxisStates;

    public LogBlock(
            String id,
            Material worldMaterial,
            Map<Axis, WrappedBlockState> axisStates,
            Map<Axis, WrappedBlockState> strippedAxisStates
    ) {
        Preconditions.checkArgument(axisStates.size() == Axis.values().length, "LogBlock must have states for all axe: " + Arrays.toString(Axis.values()));
        Preconditions.checkArgument(strippedAxisStates.size() == Axis.values().length, "LogBlock must have stripped states for all axes: " + Arrays.toString(Axis.values()));
        this.id = id;
        this.worldMaterial = worldMaterial;
        this.axisStates = new HashMap<>();
        for (Map.Entry<Axis, WrappedBlockState> entry : axisStates.entrySet()) {
            this.axisStates.put(entry.getKey(), new CustomBlockState(this, entry.getValue().getGlobalId()));
        }
        this.strippedAxisStates = new HashMap<>();
        for (Map.Entry<Axis, WrappedBlockState> entry : strippedAxisStates.entrySet()) {
            this.strippedAxisStates.put(entry.getKey(), new CustomBlockState(this, entry.getValue().getGlobalId()));
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
        final boolean stripped = STRIPPED_LOGS.contains(blockData.getMaterial());
        final Axis axis = orientable.getAxis();
        if (stripped) {
            return this.strippedAxisStates.getOrDefault(axis, this.strippedAxisStates.get(Axis.Y));
        }
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
    public Map<String, String> getPropertiesByName(WrappedBlockState state) {
        return Map.of(
                "note", String.valueOf(state.getNote()),
                "instrument", state.getInstrument().name(),
                "powered", String.valueOf(state.isPowered())
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
