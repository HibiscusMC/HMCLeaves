package com.hibiscusmc.hmcleaves.listener

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.block.BlockDirection
import com.hibiscusmc.hmcleaves.block.BlockSetting
import com.hibiscusmc.hmcleaves.block.SUPPORTING_DIRECTIONS
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.util.getChunkPosition
import com.hibiscusmc.hmcleaves.util.getPositionInChunk
import com.hibiscusmc.hmcleaves.util.parseAsAdventure
import com.hibiscusmc.hmcleaves.util.toBlockDirection
import com.hibiscusmc.hmcleaves.util.toPosition
import com.hibiscusmc.hmcleaves.util.toPositionInChunk
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import com.hibiscusmc.hmcleaves.world.WorldManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.block.BlockPistonEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ItemSpawnEvent
import org.bukkit.event.player.PlayerHarvestBlockEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.EnumSet
import java.util.LinkedList

private val REPLACEABLE_TAG: Tag<Material>? = run {
    val field = Tag::class.java.getField("REPLACEABLE")
    val tag = field.get(null)
    if (tag !is Tag<*>) return@run null
    return@run tag as Tag<Material>
}

private val BLOCKS_PLACEABLE_IN =
    EnumSet.copyOf(Material.entries.filter {
        try {
            Tag.REPLACEABLE_PLANTS.isTagged(it)
        } catch (_: Error) {
            return@filter REPLACEABLE_TAG?.isTagged(it) ?: false
        }

    }.toSet())

class BukkitListeners(
    private val plugin: HMCLeaves,
    private val config: LeavesConfig = plugin.leavesConfig,
    private val worldManager: WorldManager = plugin.worldManager
) : Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onBlockPlace(event: BlockPlaceEvent) {
        val block = event.block
        val world = block.world
        val item = event.itemInHand
        var blockData = this.config.getBlockDataFromItem(item) ?: return
        if (!checkPlaceConditions(world, block.location, blockData)) {
            event.isCancelled = true
            return
        }
        val worldUUID = world.uid
        val position = block.getPositionInChunk()
        val chunkPosition = block.getChunkPosition()
        val leavesChunk = plugin.worldManager.getOrAdd(worldUUID).getOrAdd(chunkPosition)
        val result = blockData.listen(event::class.java, event, world, block.location, position, leavesChunk, config)
        if (result.type == ListenResultType.CANCEL_EVENT) return
        blockData = result.blockData
        leavesChunk[position] = blockData
    }

    private fun checkPlaceConditions(world: World, location: Location, blockData: BlockData): Boolean {
        for (condition in blockData.placeConditions) {
            if (condition.canBePlaced(this.worldManager, world, location)) {
                return true
            }
        }
        return blockData.placeConditions.isEmpty()
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock ?: return
        val itemInHand = event.item ?: return
        if (itemInHand.type == Material.AIR) return

        val player = event.player
        val worldUUID = player.world.uid
        val face = event.blockFace
        val relativeBlock =
            if (BLOCKS_PLACEABLE_IN.contains(clickedBlock.type)) clickedBlock else clickedBlock.getRelative(face)

        val clickedBlockData = this.worldManager[worldUUID]
            ?.get(clickedBlock.getChunkPosition())
            ?.get(clickedBlock.getPositionInChunk())

        if (this.config.isDebugStick(itemInHand)) {

            if (clickedBlockData == null) {
                player.sendMessage("<red>That block does not have custom data.".parseAsAdventure())
            } else {
                player.sendMessage("<green>Actual material: ${clickedBlock.type}, Data: $clickedBlockData".parseAsAdventure())
            }

            event.setUseInteractedBlock(Event.Result.DENY)
            event.setUseItemInHand(Event.Result.DENY)
            event.isCancelled = true
            return
        }

        val data = this.config.getBlockDataFromItem(itemInHand) ?: return

        if (clickedBlockData == null && itemInHand.type.isBlock) {
            return
        }
        if (clickedBlockData == null && clickedBlock.type.isInteractable && !player.isSneaking) {
            return
        }

        val replacedState = relativeBlock.state
        val material = data.worldMaterial

        val world = event.player.world

        if (!data.settings.isEnabled(BlockSetting.PLACEABLE_IN_ENTITIES) && !world.getNearbyEntities(
                relativeBlock.location.clone().add(0.5, 0.5, 0.5),
                0.5,
                0.5,
                0.5
            ) { it is LivingEntity }.isEmpty()
        ) {
            event.setCancelled(true);
            return
        }

        relativeBlock.setType(material, true)

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
            event.setUseInteractedBlock(Event.Result.DENY)
            event.setUseItemInHand(Event.Result.DENY)
            event.isCancelled = true
            return
        }

        if (player.gameMode != GameMode.CREATIVE) {
            val hand = event.hand
            itemInHand.amount -= 1
            hand?.apply {
                player.inventory.setItem(this, itemInHand)
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onBlockDropItem(event: BlockDropItemEvent) {
        val block = event.block
        val world = block.world
        val worldUUID = world.uid
        val position = block.getPositionInChunk()
        val chunkPosition = block.getChunkPosition()
        val leavesChunk = plugin.worldManager[worldUUID]?.get(chunkPosition) ?: return
        var data = leavesChunk.getDataToRemove(position) ?: return
        val result = data.listen(event::class.java, event, world, block.location, position, leavesChunk, this.config)
        if (result.type == ListenResultType.CANCEL_EVENT || result.type == ListenResultType.BLOCKED) return
        data = result.blockData
        data.replaceItemDrops(this.config, event.items)
        leavesChunk.removeFromToRemove(position)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onBlockHarvest(event: PlayerHarvestBlockEvent) {
        val block = event.harvestedBlock
        val world = block.world
        val worldUUID = world.uid
        val position = block.getPositionInChunk()
        val chunkPosition = block.getChunkPosition()
        val leavesChunk = plugin.worldManager[worldUUID]?.get(chunkPosition) ?: return
        var data = leavesChunk[position] ?: return
        val result = data.listen(event::class.java, event, world, block.location, position, leavesChunk, this.config)
        if (result.type == ListenResultType.CANCEL_EVENT || result.type == ListenResultType.BLOCKED) return
        data = result.blockData
        data.replaceItemStackDrops(this.config, event.itemsHarvested)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onItemSpawn(event: ItemSpawnEvent) {
        val block = event.location.block
        val position = block.location.toPosition() ?: return
        val leavesChunk = worldManager[position.world]?.get(position.getChunkPosition()) ?: return
        val positionInChunk = position.toPositionInChunk()
        val world = event.entity.world
        val data = leavesChunk.getDataToRemove(positionInChunk) ?: leavesChunk[positionInChunk] ?: run {
            if (!block.type.isAir) return
            handleRelativeBlockBreak(
                world,
                block.location
            )
            return
        }
        val result =
            data.listen(
                event::class.java,
                event,
                event.entity.world,
                block.location,
                positionInChunk,
                leavesChunk,
                this.config
            )
        if (result.type == ListenResultType.CANCEL_EVENT) {
            val block = event.entity.world.getBlockAt(position.toLocation())
            block.type = data.worldMaterial
            event.isCancelled = true
            return
        }
        val list = mutableListOf(event.entity)
        data.replaceItemDrops(this.config, list)
        if (list.isEmpty()) {
            event.isCancelled = true
            return
        }
        if (!block.type.isAir) {
            return
        }
        leavesChunk.remove(positionInChunk, false)
        leavesChunk.removeFromToRemove(positionInChunk)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onBlockGrow(event: BlockGrowEvent) {
        val state = event.newState
        val world = state.world
        val worldUUID = world.uid
        val position = event.block.getPositionInChunk()
        val chunkPosition = event.block.getChunkPosition()
        var data = this.config.getDefaultBlockData(state.type) ?: return
        val leavesChunk = plugin.worldManager.getOrAdd(worldUUID).getOrAdd(chunkPosition)
        val result = data.listen(event::class.java, event, world, event.block.location, position, leavesChunk, config)
        if (result.type == ListenResultType.CANCEL_EVENT) {
            return
        }
        data = result.blockData
        leavesChunk[position] = data
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onBlockSpread(event: BlockSpreadEvent) {
        val state = event.newState
        val world = state.world
        val worldUUID = world.uid
        val position = event.block.getPositionInChunk()
        val chunkPosition = event.block.getChunkPosition()
        var data = this.config.getDefaultBlockData(state.type) ?: return
        val leavesChunk = plugin.worldManager.getOrAdd(worldUUID).getOrAdd(chunkPosition)
        val result = data.listen(event::class.java, event, world, event.block.location, position, leavesChunk, config)
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
        val leavesChunk = plugin.worldManager[worldUUID]?.get(chunkPosition) ?: return
        val data = leavesChunk[position] ?: run {
            handleRelativeBlockBreak(
                world,
                block.location
            )
            return
        }
        val result = data.listen(event::class.java, event, world, block.location, position, leavesChunk, config)
        if (result.type == ListenResultType.CANCEL_EVENT) return
        leavesChunk.remove(position, true)
        handleRelativeBlockBreak(
            world,
            block.location
        )
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onLeavesDecay(event: LeavesDecayEvent) {
        val block = event.block
        val world = block.world
        val worldUUID = world.uid
        val position = block.getPositionInChunk()
        val chunkPosition = block.getChunkPosition()
        val leavesChunk = plugin.worldManager[worldUUID]?.get(chunkPosition) ?: return
        val data = leavesChunk[position] ?: run {
            handleRelativeBlockBreak(
                world,
                block.location
            )
            return
        }
        val result = data.listen(event::class.java, event, world, block.location, position, leavesChunk, config)
        if (result.type == ListenResultType.CANCEL_EVENT) return
        leavesChunk.remove(position, true)
        handleRelativeBlockBreak(
            world,
            block.location
        )
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onBlockExplode(event: BlockExplodeEvent) {
        val explodedBlock = event.block
        val world = explodedBlock.world
        handleExplosion(event, world, event.blockList())
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private fun onEntityExplode(event: EntityExplodeEvent) {
        val entity = event.entity
        val world = entity.world
        handleExplosion(event, world, event.blockList())
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
        movedListAdder: (LinkedList<Triple<Location, BlockData, LeavesChunk>>, Triple<Location, BlockData, LeavesChunk>) -> Unit
    ) {
        val worldUUID = world.uid
        val minHeight = world.minHeight
        val maxHeight = world.maxHeight
        val moved: LinkedList<Triple<Location, BlockData, LeavesChunk>> = LinkedList()
        loop@ for (block in blocks) {
            val chunkPosition = block.getChunkPosition()
            val leavesChunk = plugin.worldManager[worldUUID]?.get(chunkPosition) ?: continue
            val positionInChunk = block.getPositionInChunk()
            var data = leavesChunk[positionInChunk]
            if (data == null) {
                handleRelativeBlockBreak(
                    world,
                    block.location
                )
                continue
            }
            val result =
                data.listen(event::class.java, event, world, block.location, positionInChunk, leavesChunk, config)
            if (result.type == ListenResultType.CANCEL_EVENT) {
                event.isCancelled = true
                return
            }
            if (result.type == ListenResultType.REMOVED) {
                handleRelativeBlockBreak(
                    world,
                    block.location
                )
                continue
            }
            if (result.type == ListenResultType.BLOCKED) {
                continue
            }
            data = result.blockData
            movedListAdder(moved, Triple(block.location, data, leavesChunk))
        }

        for (triple in moved) {
            val oldPosition = triple.first.toPosition() ?: continue
            val oldChunkPos = oldPosition.getChunkPosition()
            val newPosition = oldPosition.relative(direction, minHeight, maxHeight) ?: continue
            val newChunkPos = newPosition.getChunkPosition()

            val leavesChunk = if (oldChunkPos == newChunkPos) {
                triple.third
            } else {
                worldManager.getOrAdd(worldUUID).getOrAdd(newChunkPos)
            }
            leavesChunk[newPosition.toPositionInChunk()] = triple.second
            triple.third.remove(oldPosition.toPositionInChunk(), true)
        }
    }

    private fun <E : Event> handleExplosion(
        event: E,
        world: World,
        blockList: List<Block>
    ) {
        if (event !is Cancellable) return
        val worldUUID = world.uid
        val toRemove: MutableList<Triple<Location, BlockData, LeavesChunk>> = mutableListOf()
        for (block in blockList) {
            val chunkPosition = block.getChunkPosition()
            val leavesChunk = plugin.worldManager[worldUUID]?.get(chunkPosition) ?: continue
            val position = block.getPositionInChunk()
            var data = leavesChunk[position]
            if (data == null) {
                handleRelativeBlockBreak(
                    world,
                    block.location
                )
                continue
            }
            val result =
                data.listen(event::class.java, event, world, block.location, position, leavesChunk, this.config)
            if (result.type == ListenResultType.CANCEL_EVENT) {
                event.isCancelled = true
                return
            }
            handleRelativeBlockBreak(
                world,
                block.location
            )
            if (result.type == ListenResultType.REMOVED || result.type == ListenResultType.BLOCKED) {
                continue
            }
            data = result.blockData
            toRemove.add(Triple(block.location, data, leavesChunk))
        }

        for (triple in toRemove) {
            val positionInChunk = triple.first.toPositionInChunk() ?: continue
            triple.third.remove(positionInChunk, true)
            handleRelativeBlockBreak(
                world,
                triple.first
            )
        }
    }

    private fun handleRelativeBlockBreak(
        world: World,
        startLocation: Location
    ) {
        val position = startLocation.toPosition() ?: return
        val minHeight = world.minHeight
        val maxHeight = world.maxHeight
        val worldUUID = world.uid
        for (direction in SUPPORTING_DIRECTIONS) {
            val relative = position.relative(direction, minHeight, maxHeight) ?: continue
            val data = this.worldManager[relative] ?: continue
            val leavesChunk = this.worldManager[worldUUID]?.get(relative.getChunkPosition()) ?: continue
            val relativeEvent = RelativeBlockBreakEvent(startLocation, direction)
            data.listen(
                relativeEvent::class.java,
                relativeEvent,
                world,
                relative.toLocation(),
                relative.toPositionInChunk(),
                leavesChunk,
                this.config
            )
        }
    }

}
