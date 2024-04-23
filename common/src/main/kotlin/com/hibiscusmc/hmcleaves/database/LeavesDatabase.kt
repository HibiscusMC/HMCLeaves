package com.hibiscusmc.hmcleaves.database

import com.hibiscusmc.hmcleaves.world.LeavesChunk
import org.bukkit.ChunkSnapshot
import org.bukkit.World

interface LeavesDatabase {

    fun handleChunkLoad(
        world: World,
        chunk: ChunkSnapshot
    )

}