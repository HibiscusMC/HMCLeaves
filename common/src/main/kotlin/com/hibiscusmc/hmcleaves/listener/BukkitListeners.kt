package com.hibiscusmc.hmcleaves.listener

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.block.BlockDirection
import com.hibiscusmc.hmcleaves.block.Property
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.database.LeavesDatabase
import com.hibiscusmc.hmcleaves.util.getChunkPosition
import com.hibiscusmc.hmcleaves.util.getPosition
import com.hibiscusmc.hmcleaves.util.getPositionInChunk
import com.hibiscusmc.hmcleaves.util.toBlockDirection
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import com.hibiscusmc.hmcleaves.world.Position
import com.hibiscusmc.hmcleaves.world.PositionInChunk
import com.hibiscusmc.hmcleaves.world.WorldManager
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.event.*
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPistonEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.world.ChunkLoadEvent
import java.util.LinkedList
import java.util.UUID

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
        blockData.listen(event, position, leavesChunk)
        if (event.isCancelled) return
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
        val data = leavesChunk[position] ?: return
        data.listen(event, position, leavesChunk)
        if (event.isCancelled) return
        leavesChunk.remove(position)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onLeavesDecay(event: LeavesDecayEvent) {
        val block = event.block
        val world = block.world
        val worldUUID = world.uid
        val position = block.getPositionInChunk()
        val chunkPosition = block.getChunkPosition()
        val leavesChunk = plugin.getWorldManager()[worldUUID]?.get(chunkPosition) ?: return
        val data = leavesChunk[position] ?: return
        data.listen(event, position, leavesChunk)
        if (event.isCancelled) return
        leavesChunk.remove(position)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onBlockExplode(event: BlockExplodeEvent) {
        val explodedBlock = event.block
        val world = explodedBlock.world
        val worldUUID = world.uid
        handleExplosion(event, event.blockList(), worldUUID)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onEntityExplode(event: EntityExplodeEvent) {
        val entity = event.entity
        val world = entity.world
        val worldUUID = world.uid
        handleExplosion(event, event.blockList(), worldUUID)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onPistonExtend(event: BlockPistonExtendEvent) {
        val world = event.block.world
        val worldUUID = world.uid
        val direction = event.direction
        handlePistonMove(
            event,
            event.blocks,
            world,
            direction.toBlockDirection() ?: return
        ) { list, triple -> list.addFirst(triple) }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onPistonRetract(event: BlockPistonRetractEvent) {
        val world = event.block.world
        val worldUUID = world.uid
        val direction = event.direction
        handlePistonMove(
            event,
            event.blocks,
            world,
            direction.toBlockDirection() ?: return
        ) { list, triple -> list.addFirst(triple) }
    }


    /**
     * @param movedListAdder used to add each element to the list of triples at a specific position to ensure
     * the replaced blocks don't prevent other block data from being moved
     */
    private fun handlePistonMove(
        event: BlockPistonEvent,
        blocks: List<Block>,
        world: World,
        direction: BlockDirection,
        movedListAdder: (MutableList<Triple<Position, BlockData, LeavesChunk>>, Triple<Position, BlockData, LeavesChunk>) -> Unit
    ) {
        val worldUUID = world.uid
        val moved: MutableList<Triple<Position, BlockData, LeavesChunk>> = LinkedList()
        for (block in blocks) {
            val chunkPosition = block.getChunkPosition()
            val leavesChunk = plugin.getWorldManager()[worldUUID]?.get(chunkPosition) ?: continue
            val position = block.getPosition()
            val positionInChunk = block.getPositionInChunk()
            val data = leavesChunk[positionInChunk] ?: continue
            val result = data.listen(event, positionInChunk, leavesChunk)
            if (result == ListenResult.CANCEL_EVENT) {
                event.isCancelled = true
                return
            }
            if (result == ListenResult.REMOVED || result == ListenResult.BLOCKED) {
                continue
            }
            movedListAdder(moved, Triple(position, data, leavesChunk))
        }
        val minHeight = world.minHeight
        val maxHeight = world.maxHeight

        for (triple in moved) {
            val oldPosition = triple.first
            val oldChunkPos = oldPosition.getChunkPosition()
            val newPosition = oldPosition.relative(direction, minHeight, maxHeight) ?: continue
            val newChunkPos = newPosition.getChunkPosition()

            val leavesChunk = if (oldChunkPos == newChunkPos) {
                triple.third
            } else {
                worldManager.getOrAdd(worldUUID).getOrAdd(newChunkPos)
            }
            Bukkit.broadcastMessage("Replacing (${oldPosition.x}, ${oldPosition.y}, ${oldPosition.z}) with " +
                    "(${newPosition.x}, ${newPosition.y}, ${newPosition.z})")
            leavesChunk[newPosition.toPositionInChunk()] = triple.second
            triple.third.remove(oldPosition.toPositionInChunk())
        }
    }

    private fun <E : Event> handleExplosion(
        event: E,
        blockList: List<Block>,
        worldUUID: UUID
    ) {
        if (event !is Cancellable) return
        val toRemove: MutableList<Triple<PositionInChunk, BlockData, LeavesChunk>> = mutableListOf()
        for (block in blockList) {
            val chunkPosition = block.getChunkPosition()
            val leavesChunk = plugin.getWorldManager()[worldUUID]?.get(chunkPosition) ?: continue
            val position = block.getPositionInChunk()
            val data = leavesChunk[position] ?: return
            data.listen(event, position, leavesChunk)
            if (event.isCancelled) return
            toRemove.add(Triple(position, data, leavesChunk))
        }
        for (triple in toRemove) {
            triple.third.remove(triple.first)
        }
    }

}