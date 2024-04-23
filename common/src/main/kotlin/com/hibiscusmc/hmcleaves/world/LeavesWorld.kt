package com.hibiscusmc.hmcleaves.world

import java.util.UUID

class LeavesWorld(
    val world: UUID,
    private val chunks: MutableMap<ChunkPosition, LeavesChunk?> = hashMapOf()
) {

    operator fun get(position: ChunkPosition): LeavesChunk? {
        return this.chunks[position]
    }

    fun getOrAdd(
        position: ChunkPosition,
        supplier: (key: ChunkPosition) -> LeavesChunk = { LeavesChunk(it) }
    ): LeavesChunk {
        return this.chunks.computeIfAbsent(position, supplier)!!
    }

}