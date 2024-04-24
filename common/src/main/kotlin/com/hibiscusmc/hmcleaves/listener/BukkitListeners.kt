package com.hibiscusmc.hmcleaves.listener

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.Property
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.database.LeavesDatabase
import com.hibiscusmc.hmcleaves.util.getChunkPosition
import com.hibiscusmc.hmcleaves.util.getPosition
import com.hibiscusmc.hmcleaves.util.getPositionInChunk
import com.hibiscusmc.hmcleaves.world.WorldManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.LeavesDecayEvent
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onBlockPlace(event: BlockPlaceEvent) {
        val block = event.block
        val world = block.world
        val item = event.itemInHand
        val blockData = this.config.getBlockDataFromItem(item) ?: return
        val placeData = blockData.placementRule.canPlace(
            blockData,
            event.player,
            world,
            block.getPosition()
        )
        if (!placeData.allowed) {
            event.isCancelled = true
            return
        }
        val worldUUID = world.uid
        val position = block.getPositionInChunk()
        val chunkPosition = block.getChunkPosition()
        val leavesChunk = plugin.getWorldManager().getOrAdd(worldUUID).getOrAdd(chunkPosition)
        leavesChunk[position] = blockData
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val world = block.world
        val worldUUID = world.uid
        val position = block.getPositionInChunk()
        val chunkPosition = block.getChunkPosition()
        val leavesChunk = plugin.getWorldManager().getOrAdd(worldUUID).getOrAdd(chunkPosition)
        leavesChunk.remove(position)
    }

}