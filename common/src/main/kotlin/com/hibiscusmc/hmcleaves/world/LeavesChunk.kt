package com.hibiscusmc.hmcleaves.world

import com.hibiscusmc.hmcleaves.block.BlockData
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class LeavesChunk(
    private val position: ChunkPosition,
    val world: UUID = position.world,
    private val blocks: MutableMap<PositionInChunk, BlockData?> = ConcurrentHashMap(),
    private var dirty: Boolean = false,
    private var loaded: Boolean = false
) {

    operator fun set(position: PositionInChunk, data: BlockData) {
        this.blocks[position] = data
        this.dirty = true
    }

    fun setIfNull(position: PositionInChunk, data: BlockData) {
        this.blocks.putIfAbsent(position, data)
        this.dirty = true
    }

    operator fun get(position: PositionInChunk) : BlockData? {
        return this.blocks[position]
    }

    fun remove(position: PositionInChunk) : BlockData? {
        return this.blocks.remove(position)
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