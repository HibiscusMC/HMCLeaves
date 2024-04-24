package com.hibiscusmc.hmcleaves.util

import com.github.retrooper.packetevents.util.Vector3i
import com.hibiscusmc.hmcleaves.block.BlockDirection
import com.hibiscusmc.hmcleaves.block.BlockDirection.*
import com.hibiscusmc.hmcleaves.world.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import java.util.UUID

fun Vector3i.toChunkPosition(world: UUID): ChunkPosition {
    return ChunkPosition(world, this.x, this.z)
}

fun Vector3i.toPosition(world: UUID): Position {
    return Position(world, this.x, this.y, this.z)
}

fun Block.getPosition(): Position {
    return Position(this.world.uid, this.x, this.y, this.z)
}

fun Block.getChunkPosition(): ChunkPosition {
    return ChunkPosition(this.world.uid, convertCoordToChunkCoord(this.x), convertCoordToChunkCoord(this.z))
}

fun Block.getPositionInChunk(): PositionInChunk {
    return PositionInChunk(
        this.world.uid,
        convertCoordToCoordInChunk(this.x),
        this.y,
        convertCoordToCoordInChunk(this.z)
    )
}

fun BlockFace.toBlockDirection(): BlockDirection? {
    return when (this) {
        BlockFace.NORTH -> NORTH
        BlockFace.EAST -> EAST
        BlockFace.SOUTH -> SOUTH
        BlockFace.WEST -> WEST
        BlockFace.UP -> UP
        BlockFace.DOWN -> DOWN
        else -> null
    }

}