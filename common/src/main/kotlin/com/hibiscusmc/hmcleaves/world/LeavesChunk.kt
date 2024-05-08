package com.hibiscusmc.hmcleaves.world

import com.hibiscusmc.hmcleaves.block.BlockData
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LeavesChunk(
    val position: ChunkPosition,
    val world: UUID = position.world,
    private val blocks: MutableMap<PositionInChunk, BlockData?> = ConcurrentHashMap(),
    private val defaultBlocks: MutableMap<PositionInChunk, BlockData> = HashMap(),
    private val blocksToRemove: MutableMap<PositionInChunk, BlockData> = ConcurrentHashMap(),
    private var dirty: Boolean = false,
    private var loaded: Boolean = false
) {

    fun setDefaultBlock(position: PositionInChunk, data: BlockData) {
        this.defaultBlocks[position] = data
    }

    operator fun set(position: PositionInChunk, data: BlockData) {
        this.defaultBlocks.remove(position)
        this.blocks[position] = data
        this.dirty = true
    }

    operator fun get(position: PositionInChunk) : BlockData? {
        return this.blocks[position] ?: this.defaultBlocks[position]
    }

    fun remove(position: PositionInChunk, addToRemoved: Boolean) : BlockData? {
        val removed = this.blocks.remove(position) ?: this.defaultBlocks.remove(position)
        if (addToRemoved && removed != null) {
            this.blocksToRemove[position] = removed
        }
        this.dirty = true
        return removed
    }

    fun getBlocks(): Map<PositionInChunk, BlockData?> {
        return Collections.unmodifiableMap(this.blocks)
    }

    fun getDefaultBlocks(): Map<PositionInChunk, BlockData?> {
        return Collections.unmodifiableMap(this.defaultBlocks)
    }

    fun getDataToRemove(position: PositionInChunk): BlockData? {
        return this.blocksToRemove[position]
    }

    fun removeFromToRemove(position: PositionInChunk): BlockData? {
        return this.blocksToRemove.remove(position)
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

    fun isLoaded(): Boolean {
        return this.loaded
    }

}