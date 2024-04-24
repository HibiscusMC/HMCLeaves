package com.hibiscusmc.hmcleaves.block

import org.bukkit.block.BlockFace

enum class BlockDirection(
    val xOffset: Int,
    val yOffset: Int,
    val zOffset: Int,
    val bukkitBlockFace: BlockFace
    ) {

    NORTH(0, 0, -1, BlockFace.NORTH),
    EAST(1, 0, 0, BlockFace.EAST),
    SOUTH(0, 0, 1, BlockFace.SOUTH),
    WEST(-1, 0, 0, BlockFace.WEST),
    UP(0, 1, 0, BlockFace.UP),
    DOWN(0, -1, 0, BlockFace.DOWN);

}

val BLOCK_DIRECTIONS = BlockDirection.entries