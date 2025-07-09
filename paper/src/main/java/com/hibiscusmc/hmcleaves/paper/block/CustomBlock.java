package com.hibiscusmc.hmcleaves.paper.block;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.hibiscusmc.hmcleaves.paper.config.LeavesConfig;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.Map;
import java.util.function.Consumer;

public interface CustomBlock {

    String id();

    CustomBlockState getBlockStateFromWorldBlock(BlockData blockData);

    /**
     *
     * @return true if the block was placed successfully, false otherwise
     */
    boolean placeBlock(LeavesConfig leavesConfig, BlockPlaceData placeData, Consumer<CustomBlockState> beforePlaceIfSuccessful);

    Map<String, String> getPropertiesByName(WrappedBlockState state);

    Material worldMaterial();

}
