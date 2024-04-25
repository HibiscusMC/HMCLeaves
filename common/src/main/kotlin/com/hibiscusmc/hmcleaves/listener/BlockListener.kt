package com.hibiscusmc.hmcleaves.listener

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.block.getDirectionTo
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.util.asBlockAxis
import com.hibiscusmc.hmcleaves.util.getPosition
import com.hibiscusmc.hmcleaves.util.toBlockDirection
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import com.hibiscusmc.hmcleaves.world.PositionInChunk
import org.bukkit.Bukkit
import org.bukkit.block.data.Orientable
import org.bukkit.event.Event
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.plugin.java.JavaPlugin

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

sealed class BlockListener<E : Event> {

    val plugin: HMCLeaves by lazy { JavaPlugin.getPlugin(HMCLeaves::class.java) }

    fun listen(
        event: Event,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        return this.handle(event as E, position, blockData, leavesChunk, config)
    }

    protected abstract fun handle(
        event: E,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult

}

data object LeavesDecayListener : BlockListener<LeavesDecayEvent>() {

    override fun handle(
        event: LeavesDecayEvent,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        leavesChunk.remove(position)
        return ListenResult(blockData, ListenResultType.REMOVED)
    }

}

data object LeavesPistonExtendListener : BlockListener<BlockPistonExtendEvent>() {

    override fun handle(
        event: BlockPistonExtendEvent,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        leavesChunk.remove(position)
        return ListenResult(blockData, ListenResultType.REMOVED)
    }

}

data object LeavesPistonRetractListener : BlockListener<BlockPistonRetractEvent>() {

    override fun handle(
        event: BlockPistonRetractEvent,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        leavesChunk.remove(position)
        return ListenResult(blockData, ListenResultType.REMOVED)
    }

}

data object LogPlaceListener : BlockListener<BlockPlaceEvent>() {

    override fun handle(
        event: BlockPlaceEvent,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        val firstPos = event.block.getPosition()
        val secondPos = event.blockAgainst.getPosition()
        val axis = getDirectionTo(firstPos, secondPos)?.toAxis() ?: run {
            return ListenResult(
                blockData,
                ListenResultType.PASS_THROUGH
            )
        }

        val newData = config.getDirectionalBlockData(blockData, axis) ?: return ListenResult(
            blockData,
            ListenResultType.PASS_THROUGH
        )
        return ListenResult(newData, ListenResultType.PASS_THROUGH)

    }

}