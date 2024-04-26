package com.hibiscusmc.hmcleaves.listener

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.block.BlockDirection
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.config.MATERIAL_KEY
import com.hibiscusmc.hmcleaves.database.LeavesDatabase
import com.hibiscusmc.hmcleaves.packet.sendSingleBlockChange
import com.hibiscusmc.hmcleaves.util.*
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import com.hibiscusmc.hmcleaves.world.Position
import com.hibiscusmc.hmcleaves.world.PositionInChunk
import com.hibiscusmc.hmcleaves.world.WorldManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Item
import org.bukkit.entity.LivingEntity
import org.bukkit.event.*
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.*

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
        var blockData = this.config.getBlockDataFromItem(item) ?: return
        val worldUUID = world.uid
        val position = block.getPositionInChunk()
        val chunkPosition = block.getChunkPosition()
        val leavesChunk = plugin.getWorldManager().getOrAdd(worldUUID).getOrAdd(chunkPosition)
        val result = blockData.listen(event, position, leavesChunk, config)
        if (result.type == ListenResultType.CANCEL_EVENT) return
        blockData = result.blockData
        leavesChunk[position] = blockData
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock ?: return
        val itemInHand = event.item ?: return
        val player = event.player
        val worldUUID = player.world.uid
        val data = this.config.getBlockDataFromItem(itemInHand) ?: return
        val face = event.blockFace
        val relativeBlock = clickedBlock.getRelative(face)

        val clickedBlockData = this.worldManager[worldUUID]
            ?.get(relativeBlock.getChunkPosition())
            ?.get(relativeBlock.getPositionInChunk())

        if (clickedBlockData == null && itemInHand.type.isBlock) {
            return
        }
        if (clickedBlockData == null && clickedBlock.type.isInteractable && !player.isSneaking) {
            return
        }

        val replacedState = relativeBlock.state
        val material = data.worldMaterial

        val world = event.player.world

        if (!data.placeableInEntities && !world.getNearbyEntities(
                relativeBlock.location.clone().add(0.5, 0.5, 0.5),
                0.5,
                0.5,
                0.5
            ) { it is LivingEntity }.isEmpty()
        ) {
            event.setCancelled(true);
            return;
        }

        relativeBlock.setType(material, false)

        val blockPlaceEvent = BlockPlaceEvent(
            relativeBlock,
            replacedState,
            clickedBlock,
            itemInHand,
            player,
            event.useItemInHand() != Event.Result.DENY,
            event.hand ?: EquipmentSlot.HAND
        )
        Bukkit.getServer().pluginManager.callEvent(blockPlaceEvent)

        if (blockPlaceEvent.isCancelled) {
            relativeBlock.setBlockData(replacedState.blockData, false)
            return
        }

        if (player.gameMode == GameMode.CREATIVE) {

        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onBlockDropItem(event: BlockDropItemEvent) {
        val block = event.block
        val world = block.world
        val worldUUID = world.uid
        val position = block.getPositionInChunk()
        val chunkPosition = block.getChunkPosition()
        val leavesChunk = plugin.getWorldManager()[worldUUID]?.get(chunkPosition) ?: return
        var data = leavesChunk[position] ?: run {
            return
        }
        val result = data.listen(event, position, leavesChunk, this.config)
        if (result.type == ListenResultType.CANCEL_EVENT || result.type == ListenResultType.BLOCKED) return
        data = result.blockData
        data.replaceDrops(this.config, event.items as MutableList<Item>)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onItemSpawn(event: ItemSpawnEvent) {
        val position = event.location.block.location.toPosition() ?: return
        val leavesChunk = worldManager[position.world]?.get(position.getChunkPosition()) ?: return
        val positionInChunk = position.toPositionInChunk()
        val data = leavesChunk[positionInChunk] ?: return
        val result = data.listen(event, positionInChunk, leavesChunk, this.config)
        if (result.type == ListenResultType.CANCEL_EVENT) {
            val block = event.entity.world.getBlockAt(position.toLocation())
            block.type = data.worldMaterial
            event.isCancelled = true
            return
        }
        val list = mutableListOf(event.entity)
        data.replaceDrops(this.config, list)
        if (list.isEmpty()) {
            event.isCancelled = true
            return
        }
        leavesChunk.remove(positionInChunk)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onBlockGrow(event: BlockGrowEvent) {
        val state = event.newState
        val world = state.world
        val worldUUID = world.uid
        val position = event.block.getPositionInChunk()
        val chunkPosition = event.block.getChunkPosition()
        var data = this.config.getDefaultBLockData(state.type) ?: run {
            return
        }
        val leavesChunk = plugin.getWorldManager().getOrAdd(worldUUID).getOrAdd(chunkPosition)
        val result = data.listen(event, position, leavesChunk, config)
        if (result.type == ListenResultType.CANCEL_EVENT) {
            return
        }
        data = result.blockData
        leavesChunk[position] = data
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val world = block.world
        val worldUUID = world.uid
        val position = block.getPositionInChunk()
        val chunkPosition = block.getChunkPosition()
        val leavesChunk = plugin.getWorldManager()[worldUUID]?.get(chunkPosition) ?: return
        val data = leavesChunk[position] ?: return
        val result = data.listen(event, position, leavesChunk, config)
        if (result.type == ListenResultType.CANCEL_EVENT) return
        Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, { _ ->
//            sendSingleBlockChange(block.getPosition(), Material.AIR.createBlockData(), event.player)
            leavesChunk.remove(position) // only want to remove it after it drops items
        }, 1)
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
        val result = data.listen(event, position, leavesChunk, config)
        if (result.type == ListenResultType.CANCEL_EVENT) return
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
        movedListAdder: (LinkedList<Triple<Position, BlockData, LeavesChunk>>, Triple<Position, BlockData, LeavesChunk>) -> Unit
    ) {
        val worldUUID = world.uid
        val moved: LinkedList<Triple<Position, BlockData, LeavesChunk>> = LinkedList()
        for (block in blocks) {
            val chunkPosition = block.getChunkPosition()
            val leavesChunk = plugin.getWorldManager()[worldUUID]?.get(chunkPosition) ?: continue
            val position = block.getPosition()
            val positionInChunk = block.getPositionInChunk()
            var data = leavesChunk[positionInChunk] ?: continue
            val result = data.listen(event, positionInChunk, leavesChunk, config)
            if (result.type == ListenResultType.CANCEL_EVENT) {
                event.isCancelled = true
                return
            }
            if (result.type == ListenResultType.REMOVED || result.type == ListenResultType.BLOCKED) {
                continue
            }
            data = result.blockData
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
            var data = leavesChunk[position] ?: return
            val result = data.listen(event, position, leavesChunk, config)
            if (result.type == ListenResultType.CANCEL_EVENT) {
                event.isCancelled = true
                return
            }
            if (result.type == ListenResultType.REMOVED || result.type == ListenResultType.BLOCKED) {
                continue
            }
            data = result.blockData
            toRemove.add(Triple(position, data, leavesChunk))
        }
        for (triple in toRemove) {
            triple.third.remove(triple.first)
        }
    }

}