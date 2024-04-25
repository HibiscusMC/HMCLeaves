package com.hibiscusmc.hmcleaves.block

import com.hibiscusmc.hmcleaves.world.Position
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

    fun toAxis() : BlockAxis {
        return when (this) {
            NORTH, SOUTH -> BlockAxis.X
            EAST, WEST -> BlockAxis.Z
            UP, DOWN -> BlockAxis.Y
        }
    }

}

fun getDirectionTo(first: Position, second: Position) : BlockDirection? {
    val diffX = second.x - first.x
    if (diffX != 0) {
        if (diffX < 0) return BlockDirection.NORTH
        return BlockDirection.SOUTH
    }
    val diffY = second.y - first.y
    if (diffY != 0) {
        if (diffY < 0) return BlockDirection.DOWN
        return BlockDirection.UP
    }
    val diffZ = second.z - first.z
    if (diffZ != 0) {
        if (diffZ < 0) return BlockDirection.WEST
        return BlockDirection.EAST
    }
    return null
}

val BLOCK_DIRECTIONS = BlockDirection.entries