package com.hibiscusmc.hmcleaves.world

import java.util.*

class LeavesChunk(
    private val position: ChunkPosition,
    val world: UUID = position.world,
    private val blocks: MutableMap<PositionInChunk, BlockData?> = hashMapOf(),
    private var dirty: Boolean = false,
    private var loaded: Boolean = false
) {

    operator fun set(position: PositionInChunk, data: BlockData) {
        this.blocks[position] = data
        this.dirty = true
    }

    operator fun get(position: PositionInChunk) : BlockData? {
        return this.blocks[position]
    }

    fun getBlocks(): Map<PositionInChunk, BlockData?> {
        return Collections.unmodifiableMap(this.blocks)
    }

    fun isDirty() = this.dirty

    fun markDirty() {
        this.dirty = true
    }

    fun markClean() {
        this.dirty = false
    }

    fun setLoaded(loaded: Boolean) {
        this.loaded = loaded
    }

}