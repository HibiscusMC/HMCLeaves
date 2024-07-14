package com.hibiscusmc.hmcleaves.world

import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class WorldManager(
    private val worlds: MutableMap<UUID, LeavesWorld> = ConcurrentHashMap()
) {

    fun reload(leavesConfig: LeavesConfig) {
        this.worlds.values.forEach { world -> world.reload(leavesConfig) }
    }

    operator fun get(world: UUID): LeavesWorld? {
        return this.worlds[world]
    }

    fun getOrAdd(world: UUID, supplier: (key: UUID) -> LeavesWorld = { LeavesWorld(it) }): LeavesWorld {
        return this.worlds.computeIfAbsent(world, supplier)
    }

    operator fun get(position: Position): BlockData? {
        return this.worlds[position.world]
            ?.get(position.getChunkPosition())
            ?.get(position.toPositionInChunk())
    }

    operator fun set(position: Position, blockData: BlockData) {
        this.worlds.computeIfAbsent(position.world) { _ -> LeavesWorld(position.world) }
            .getOrAdd(position.getChunkPosition())[position.toPositionInChunk()] = blockData
    }

}