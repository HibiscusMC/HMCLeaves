package com.hibiscusmc.hmcleaves.world

import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.database.LeavesDatabase
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LeavesWorld(
    val database: LeavesDatabase,
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
        supplier: (key: ChunkPosition) -> LeavesChunk = { LeavesChunk(this.database, it) }
    ): LeavesChunk {
        return this.chunks.computeIfAbsent(position, supplier)
    }

    fun remove(position: ChunkPosition): LeavesChunk? {
        return this.chunks.remove(position)
    }

}