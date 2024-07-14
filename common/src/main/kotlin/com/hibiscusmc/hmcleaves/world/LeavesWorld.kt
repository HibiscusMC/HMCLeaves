package com.hibiscusmc.hmcleaves.world

import com.hibiscusmc.hmcleaves.config.LeavesConfig
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LeavesWorld(
    val world: UUID,
    private val chunks: MutableMap<ChunkPosition, LeavesChunk> = ConcurrentHashMap()
) {

    fun reload(leavesConfig: LeavesConfig) {
        this.chunks.values.forEach { chunk -> chunk.reload(leavesConfig) }
    }

    operator fun get(position: ChunkPosition): LeavesChunk? {
        return this.chunks[position]
    }

    fun getOrAdd(
        position: ChunkPosition,
        supplier: (key: ChunkPosition) -> LeavesChunk = { LeavesChunk(it) }
    ): LeavesChunk {
        return this.chunks.computeIfAbsent(position, supplier)
    }

    fun remove(position: ChunkPosition): LeavesChunk? {
        return this.chunks.remove(position)
    }

}