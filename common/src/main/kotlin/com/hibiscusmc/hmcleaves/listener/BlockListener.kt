package com.hibiscusmc.hmcleaves.listener

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import com.hibiscusmc.hmcleaves.world.PositionInChunk
import org.bukkit.event.Event
import org.bukkit.event.block.BlockPistonEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.plugin.java.JavaPlugin

enum class ListenResult {

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
        leavesChunk: LeavesChunk
    ): ListenResult {
        return this.handle(event as E, position, blockData, leavesChunk)
    }

    protected abstract fun handle(
        event: E,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk
    ): ListenResult

}

data object LeavesDecayListener : BlockListener<LeavesDecayEvent>() {

    override fun handle(
        event: LeavesDecayEvent,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk
    ): ListenResult {
        leavesChunk.remove(position)
        return ListenResult.REMOVED
    }

}

data object LeavesPistonExtendListener : BlockListener<BlockPistonExtendEvent>() {

    override fun handle(
        event: BlockPistonExtendEvent,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk
    ): ListenResult {
        leavesChunk.remove(position)
        return ListenResult.REMOVED
    }

}

data object LeavesPistonRetractListener : BlockListener<BlockPistonRetractEvent>() {

    override fun handle(
        event: BlockPistonRetractEvent,
        position: PositionInChunk,
        blockData: BlockData,
        leavesChunk: LeavesChunk
    ): ListenResult {
        leavesChunk.remove(position)
        return ListenResult.REMOVED
    }

}