package com.hibiscusmc.hmcleaves.world

enum class BlockDirection(
    val xOffset: Int,
    val yOffset: Int,
    val zOffset: Int
    ) {

    NORTH(0, 0, -1),
    EAST(1, 0, 0),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    UP(0, 1, 0),
    DOWN(0, -1, 0)

}

val BLOCK_DIRECTIONS = BlockDirection.entries