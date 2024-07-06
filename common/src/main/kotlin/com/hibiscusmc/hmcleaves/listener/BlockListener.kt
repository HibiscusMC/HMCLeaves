package com.hibiscusmc.hmcleaves.listener

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BLOCK_DIRECTIONS
import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.block.BlockDirection
import com.hibiscusmc.hmcleaves.block.BlockFamily
import com.hibiscusmc.hmcleaves.block.BlockSetting
import com.hibiscusmc.hmcleaves.block.BlockType
import com.hibiscusmc.hmcleaves.block.getDirectionTo
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.util.getPosition
import com.hibiscusmc.hmcleaves.util.getPositionInChunk
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import com.hibiscusmc.hmcleaves.world.PositionInChunk
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.event.Event
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.meta.Damageable
import org.bukkit.plugin.java.JavaPlugin
import java.util.EnumSet

data class ListenResult(val blockData: BlockData, val type: ListenResultType)

enum class ListenResultType {

    // If nothing should happen
    PASS_THROUGH,

    // if the block data was removed
    REMOVED,

    // if the event should be prevented from acting on the block
    BLOCKED,

    // if the event as a whole should be cancelled
    CANCEL_EVENT

}

sealed class BlockListener<E> {

    val plugin: HMCLeaves by lazy { JavaPlugin.getPlugin(HMCLeaves::class.java) }

    fun listen(
        event: E,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        return this.handle(event, world, startLocation, position, blockData, leavesChunk, config)
    }

    protected abstract fun handle(
        event: E,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult

}

data object LeavesDecayListener : BlockListener<LeavesDecayEvent>() {

    override fun handle(
        event: LeavesDecayEvent,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        return ListenResult(blockData, ListenResultType.PASS_THROUGH)
    }

}

data object LeavesPistonExtendListener : BlockListener<BlockPistonExtendEvent>() {

    override fun handle(
        event: BlockPistonExtendEvent,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        leavesChunk.remove(position, true)
        return ListenResult(blockData, ListenResultType.REMOVED)
    }

}

data object LeavesPistonRetractListener : BlockListener<BlockPistonRetractEvent>() {

    override fun handle(
        event: BlockPistonRetractEvent,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        leavesChunk.remove(position, true)
        return ListenResult(blockData, ListenResultType.REMOVED)
    }

}

data object LogPlaceListener : BlockListener<BlockPlaceEvent>() {

    override fun handle(
        event: BlockPlaceEvent,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        val firstPos = event.block.getPosition()
        val secondPos = event.blockAgainst.getPosition()
        val axis = getDirectionTo(firstPos, secondPos)?.toAxis() ?: return ListenResult(
            blockData,
            ListenResultType.PASS_THROUGH
        )

        val newData = config.getDirectionalBlockData(blockData, axis) ?: return ListenResult(
            blockData,
            ListenResultType.PASS_THROUGH
        )
        return ListenResult(newData, ListenResultType.PASS_THROUGH)

    }

}

data object LogStripListener : BlockListener<PlayerInteractEvent>() {

    private val AXES = Material.entries.filter { it.name.contains("AXE") }

    override fun handle(
        event: PlayerInteractEvent,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return ListenResult(blockData, ListenResultType.PASS_THROUGH)
        }
        if (blockData.blockType != BlockType.LOG) return ListenResult(blockData, ListenResultType.PASS_THROUGH)
        event.clickedBlock ?: return ListenResult(blockData, ListenResultType.PASS_THROUGH)
        val player = event.player;
        val itemInHand = player.inventory.getItem(EquipmentSlot.HAND)
        if (!AXES.contains(itemInHand.type)) {
            return ListenResult(blockData, ListenResultType.PASS_THROUGH)
        }
        val strippedData =
            config.getStrippedBlockData(blockData) ?: return ListenResult(blockData, ListenResultType.PASS_THROUGH)

        return ListenResult(strippedData, ListenResultType.PASS_THROUGH)
    }

}

data object SugarCanePlaceListener : BlockListener<BlockPlaceEvent>() {

    private val scanDirections: List<BlockDirection> = BLOCK_DIRECTIONS.stream().filter { d ->
        d != BlockDirection.UP && d != BlockDirection.DOWN
    }.toList()

    private val soilMaterials = Tag.DIRT.values + Tag.SAND.values

    override fun handle(
        event: BlockPlaceEvent,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        val block = event.block
        val under = block.getRelative(BlockFace.DOWN)
        val relativeData = leavesChunk[under.getPositionInChunk()]
        val onTopOfOtherCane = relativeData != null && relativeData.id == blockData.id
        if (!onTopOfOtherCane && !soilMaterials.contains(under.type)) {
            event.isCancelled = true
            return ListenResult(blockData, ListenResultType.CANCEL_EVENT)
        }
        var allowed = onTopOfOtherCane
        if (!allowed) {
            for (direction in scanDirections) {
                val relative = under.getRelative(direction.bukkitBlockFace)
                if (relative.type == Material.WATER) {
                    allowed = true
                    break
                }
            }
        }
        if (!allowed) {
            event.isCancelled = true
            return ListenResult(blockData, ListenResultType.CANCEL_EVENT)
        }
        return ListenResult(blockData, ListenResultType.PASS_THROUGH)
    }
}

data object SaplingPlaceListener : BlockListener<BlockPlaceEvent>() {

    private val allowedSoil = EnumSet.of(
        Material.DIRT,
        Material.GRASS_BLOCK,
        Material.COARSE_DIRT,
        Material.PODZOL,
        Material.ROOTED_DIRT
    )

    override fun handle(
        event: BlockPlaceEvent,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        val block = event.block
        val under = block.getRelative(BlockFace.DOWN)
        if (!this.allowedSoil.contains(under.type)) {
            event.isCancelled = true
            return ListenResult(blockData, ListenResultType.CANCEL_EVENT)
        }
        return ListenResult(blockData, ListenResultType.PASS_THROUGH)
    }
}


data object SugarCaneGrowListener : BlockListener<BlockGrowEvent>() {

    override fun handle(
        event: BlockGrowEvent,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        val block = event.block
        val under = block.getRelative(BlockFace.DOWN)
        val relativeData =
            leavesChunk[under.getPositionInChunk()]
                ?: return ListenResult(blockData, ListenResultType.PASS_THROUGH)
        return ListenResult(relativeData, ListenResultType.PASS_THROUGH)
    }
}

data object PlantFacingUpPlaceListener : PlantFacingPlaceListener(BlockDirection.DOWN)
data object PlantFacingDownPlaceListener : PlantFacingPlaceListener(BlockDirection.UP)

data object PlantFacingUpGrowListener : PlantGrowListener(BlockDirection.DOWN)
data object PlantFacingDownGrowListener : PlantGrowListener(BlockDirection.UP)

data object ConnectedBlockFacingUpBlockBreakListener : ConnectedBlockFacingUpDestroyListener<BlockBreakEvent>()
data object ConnectedBlockFacingDownBlockBreakListener : ConnectedBlockFacingDownDestroyListener<BlockBreakEvent>()
data object ConnectedBlockFacingUpPistonExtendBreakListener :
    ConnectedBlockFacingUpDestroyListener<BlockPistonExtendEvent>()

data object ConnectedBlockFacingDownPistonExtendBreakListener :
    ConnectedBlockFacingDownDestroyListener<BlockPistonExtendEvent>()

data object ConnectedBlockFacingUpPistonRetractBreakListener :
    ConnectedBlockFacingUpDestroyListener<BlockPistonRetractEvent>()

data object ConnectedBlockFacingDownPistonRetractBreakListener :
    ConnectedBlockFacingDownDestroyListener<BlockPistonRetractEvent>()

data object ConnectedBlockFacingUpBlockExplodeBreakListener : ConnectedBlockFacingUpDestroyListener<BlockExplodeEvent>()
data object ConnectedBlockFacingDownBlockExplodeBreakListener :
    ConnectedBlockFacingDownDestroyListener<BlockExplodeEvent>()

data object ConnectedBlockFacingUpEntityExplodeBreakListener :
    ConnectedBlockFacingUpDestroyListener<EntityExplodeEvent>()

data object ConnectedBlockFacingDownEntityExplodeBreakListener :
    ConnectedBlockFacingDownDestroyListener<EntityExplodeEvent>()

data object PlantFacingUpRelativeBreakListener : PlantRelativeBreakListener(BlockDirection.UP)
data object PlantFacingDownRelativeBreakListener : PlantRelativeBreakListener(BlockDirection.DOWN)

sealed class ConnectedBlockFacingUpDestroyListener<T : Event> : BlockListener<T>() {
    override fun handle(
        event: T,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        val supportingBlockData = config.getNonPlant(blockData)
            ?: return ListenResult(blockData, ListenResultType.PASS_THROUGH)
        markConnectedBlocksRemoved(
            startLocation,
            supportingBlockData,
            world,
            BlockDirection.UP,
            position,
            leavesChunk,
            blockData
        )
        return ListenResult(blockData, ListenResultType.PASS_THROUGH)
    }
}

sealed class ConnectedBlockFacingDownDestroyListener<T : Event> : BlockListener<T>() {
    override fun handle(
        event: T,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        val supportingBlockData = config.getNonPlant(blockData)
            ?: return ListenResult(blockData, ListenResultType.PASS_THROUGH)
        markConnectedBlocksRemoved(
            startLocation,
            supportingBlockData,
            world,
            BlockDirection.DOWN,
            position,
            leavesChunk,
            blockData
        )
        return ListenResult(blockData, ListenResultType.PASS_THROUGH)
    }
}

sealed class PlantFacingPlaceListener(private val direction: BlockDirection) : BlockListener<BlockPlaceEvent>() {
    override fun handle(
        event: BlockPlaceEvent,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        val relative = position.relative(this.direction, world.minHeight, world.maxHeight)
            ?: return ListenResult(blockData, ListenResultType.PASS_THROUGH)
        val relativeData = leavesChunk[relative]

        val relativeBlock = event.block.getRelative(this.direction.bukkitBlockFace)
        if (relativeData == null) {
            if (!relativeBlock.type.isSolid) {
                event.isCancelled = true
                return ListenResult(blockData, ListenResultType.CANCEL_EVENT)
            }
            val newData = config.getNonPlant(blockData) ?: blockData
            return ListenResult(newData, ListenResultType.PASS_THROUGH)
        }
        if (!blockData.canConnectTo(relativeData)) {
            event.isCancelled = true
            return ListenResult(blockData, ListenResultType.CANCEL_EVENT)
        }
        val plantData = config.getPlant(relativeData)
            ?: return ListenResult(blockData, ListenResultType.PASS_THROUGH)
        leavesChunk[relative] = plantData
        return ListenResult(blockData, ListenResultType.PASS_THROUGH)
    }
}

sealed class PlantRelativeBreakListener(
    private val direction: BlockDirection
) :
    BlockListener<RelativeBlockBreakEvent>() {

    override fun handle(
        event: RelativeBlockBreakEvent,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        if (event.direction != this.direction) {
            return ListenResult(blockData, ListenResultType.PASS_THROUGH)
        }
        val supportingBlockData = config.getNonPlant(blockData)
            ?: return ListenResult(blockData, ListenResultType.PASS_THROUGH)
        leavesChunk.remove(position, true)
        markConnectedBlocksRemoved(
            event.startLocation,
            supportingBlockData,
            world,
            direction,
            position,
            leavesChunk,
            blockData
        )
        return ListenResult(blockData, ListenResultType.PASS_THROUGH)
    }
}

sealed class PlantGrowListener(
    private val supportDirection: BlockDirection
) :
    BlockListener<BlockSpreadEvent>() {

    override fun handle(
        event: BlockSpreadEvent,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        val supportingBlock = event.block.getRelative(supportDirection.bukkitBlockFace)
        val supportingData = leavesChunk[supportingBlock.getPositionInChunk()]
            ?: return ListenResult(blockData, ListenResultType.PASS_THROUGH)

        val plant = config.getPlant(supportingData)
            ?: return ListenResult(blockData, ListenResultType.PASS_THROUGH)
        Bukkit.broadcastMessage("Plant: $plant")
        leavesChunk[supportingBlock.getPositionInChunk()] = plant
        return ListenResult(supportingData, ListenResultType.PASS_THROUGH)
    }
}

private fun markConnectedBlocksRemoved(
    startLocation: Location,
    supportingBlockData: BlockData,
    world: World,
    direction: BlockDirection,
    position: PositionInChunk,
    leavesChunk: LeavesChunk,
    data: BlockData
) {
    val supporting = startLocation.block.getRelative(direction.opposite().bukkitBlockFace)
    val currentSupportingBlockData = leavesChunk[supporting.getPositionInChunk()]
    if (currentSupportingBlockData != null && data.canConnectTo(supportingBlockData)) {
        leavesChunk[supporting.getPositionInChunk()] = supportingBlockData
    }
    markConnectedBlocksRemoved(
        world,
        direction,
        position,
        leavesChunk,
        data
    )
}

private fun markConnectedBlocksRemoved(
    world: World,
    direction: BlockDirection,
    position: PositionInChunk,
    leavesChunk: LeavesChunk,
    data: BlockData
) {

    val relative = position.relative(direction, world.minHeight, world.maxHeight) ?: return
    val relativeData = leavesChunk[relative] ?: return
    if (!relativeData.canConnectTo(data)) return
    leavesChunk.remove(relative, true)
    markConnectedBlocksRemoved(
        world,
        direction,
        relative,
        leavesChunk,
        relativeData
    )
}

/**
 * For when a block broken is relative to another block
 */
data class RelativeBlockBreakEvent(
    val startLocation: Location,
    val direction: BlockDirection
)