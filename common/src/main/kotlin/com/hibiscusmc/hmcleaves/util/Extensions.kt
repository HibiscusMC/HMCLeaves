package com.hibiscusmc.hmcleaves.util

import com.github.retrooper.packetevents.util.Vector3i
import com.hibiscusmc.hmcleaves.world.ChunkPosition
import com.hibiscusmc.hmcleaves.world.Position
import java.util.UUID

fun Vector3i.toChunkPosition(world: UUID) : ChunkPosition {
    return ChunkPosition(world, this.x, this.z)
}

fun Vector3i.toPosition(world: UUID) : Position {
    return Position(world, this.x, this.y, this.z)
}