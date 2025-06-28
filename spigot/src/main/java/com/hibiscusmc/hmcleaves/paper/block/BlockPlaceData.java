package com.hibiscusmc.hmcleaves.paper.block;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public record BlockPlaceData(
        @Nullable Player player,
        Location placeLocation,
        Block clickedBlock,
        BlockFace clickedFace
) {

}
