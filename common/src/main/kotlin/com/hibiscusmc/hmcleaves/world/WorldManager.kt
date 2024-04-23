package com.hibiscusmc.hmcleaves.world

import java.util.UUID

class WorldManager(
    private val worlds: MutableMap<UUID, LeavesWorld> = hashMapOf()
) {

    operator fun get(world: UUID): LeavesWorld? {
        return this.worlds[world]
    }

    fun getOrAdd(world: UUID, supplier: (key: UUID) -> LeavesWorld = { LeavesWorld(it) }): LeavesWorld {
        return this.worlds.computeIfAbsent(world, supplier)
    }

}