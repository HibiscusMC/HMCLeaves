package com.hibiscusmc.hmcleaves.database

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.world.ChunkPosition
import com.hibiscusmc.hmcleaves.world.WorldManager
import org.bukkit.ChunkSnapshot
import org.bukkit.World

internal class UnsavedLeavesDatabase(
    private val plugin: HMCLeaves,
    private val config: LeavesConfig = plugin.getLeavesConfig(),
    private val worldManager: WorldManager = plugin.getWorldManager()
) : LeavesDatabase {


    private val chunkFirstLoadHandler = ChunkFirstLoadHandler(this.plugin)

    override fun handleChunkLoad(world: World, chunk: ChunkSnapshot) {
        val chunkPos = ChunkPosition(world.uid, chunk.x, chunk.z)
        val leavesChunk = this.worldManager.getOrAdd(world.uid)
            .getOrAdd(chunkPos)
        this.chunkFirstLoadHandler.load(world, chunk, leavesChunk)
    }
}