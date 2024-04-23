package com.hibiscusmc.hmcleaves.listener

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.database.LeavesDatabase
import com.hibiscusmc.hmcleaves.world.WorldManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent

class BukkitListeners(
    private val plugin: HMCLeaves,
    private val config: LeavesConfig = plugin.getLeavesConfig(),
    private val worldManager: WorldManager = plugin.getWorldManager(),
    private val database: LeavesDatabase = plugin.getDatabase(),
) : Listener {


    @EventHandler
    private fun onChunkLoad(event: ChunkLoadEvent) {
//        if (!event.isNewChunk) return
        val world = event.world
        val chunk = event.chunk.chunkSnapshot
        database.handleChunkLoad(world, chunk)
    }

}