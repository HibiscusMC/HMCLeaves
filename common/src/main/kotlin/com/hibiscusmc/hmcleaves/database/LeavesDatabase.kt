package com.hibiscusmc.hmcleaves.database

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.world.ChunkPosition
import org.bukkit.ChunkSnapshot
import org.bukkit.World

interface LeavesDatabase {

    companion object {

        fun createDatabase(plugin: HMCLeaves, type: DatabaseType): LeavesDatabase {
            return when(type) {
                DatabaseType.SQLITE -> SQLiteDatabase(plugin)
                else -> throw UnsupportedOperationException("$type is not a supported database type")
            }
        }

    }

    val databaseExecutor: DatabaseExecutor

    fun init()

    fun handleChunkLoad(
        world: World,
        chunk: ChunkSnapshot,
        chunkPosition: ChunkPosition,
        async: Boolean
    )

    fun loadWorld(world: World, async: Boolean)

    fun saveChunk(chunk: ChunkSnapshot, world: World, removeChunk: Boolean)

    fun saveWorld(world: World, removeChunks: Boolean)

}