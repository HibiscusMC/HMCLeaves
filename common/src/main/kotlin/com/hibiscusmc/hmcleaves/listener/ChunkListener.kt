package com.hibiscusmc.hmcleaves.listener

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.database.LeavesDatabase
import com.hibiscusmc.hmcleaves.world.ChunkPosition
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldSaveEvent
import org.bukkit.event.world.WorldUnloadEvent

class ChunkListener(
    private val plugin: HMCLeaves,
    private val database: LeavesDatabase = plugin.getDatabase()
) : Listener {

    @EventHandler
    private fun onChunkLoad(event: ChunkLoadEvent) {
        val world = event.world
        val chunk = event.chunk.chunkSnapshot
        plugin.getLeavesLogger().info("Starting chunk load: chunkX=${chunk.x}, chunkZ=${chunk.z}")
        val chunkPosition = ChunkPosition(world.uid, chunk.x, chunk.z)
        this.database.databaseExecutor.executeRead {
            this.database.handleChunkLoad(world, chunk, chunkPosition, true)
        }
    }

    @EventHandler
    private fun onWorldLoad(event: WorldLoadEvent) {
        val world = event.world
        plugin.getLeavesLogger().info("Starting world load: ${world.uid}")
        this.database.databaseExecutor.executeRead {
            this.database.loadWorld(world, true)
        }
    }

    @EventHandler
    private fun onChunkUnload(event: ChunkUnloadEvent) {
        val world = event.world
        val chunk = event.chunk.chunkSnapshot
        plugin.getLeavesLogger().info("Starting chunk unload: chunkX=${chunk.x}, chunkZ=${chunk.z}")
        this.database.databaseExecutor.executeWrite {
            this.database.saveChunk(chunk, world, true)
        }
    }

    @EventHandler
    private fun onWorldUnload(event: WorldUnloadEvent) {
        val world = event.world
        plugin.getLeavesLogger().info("Starting world unload: ${world.uid}")
        val chunks = event.world.loadedChunks.map { it.chunkSnapshot }
        chunks.forEach {
            this.database.databaseExecutor.executeWrite {
                this.database.saveChunk(it, world, true)
            }
        }
    }

    @EventHandler
    private fun worldSaveEvent(event: WorldSaveEvent) {
        val world = event.world
        plugin.getLeavesLogger().info("Starting world save: ${world.uid}")
        this.database.databaseExecutor.executeWrite {
            this.database.saveWorld(world, false)
        }
    }
}