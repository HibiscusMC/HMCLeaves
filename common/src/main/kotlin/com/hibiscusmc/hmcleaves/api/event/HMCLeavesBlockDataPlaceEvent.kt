package com.hibiscusmc.hmcleaves.api.event

import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.world.Position
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.block.BlockPlaceEvent


class HMCLeavesBlockDataPlaceEvent(
    var position: Position,
    val previousData: BlockData?,
    var blockData: BlockData,
    private var cancelled: Boolean = false
) : Event(), Cancellable {

    companion object {

        @JvmStatic
        private val handlers: HandlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return handlers
        }
    }

    override fun getHandlers(): HandlerList {
        return HMCLeavesBlockDataPlaceEvent.handlers
    }

    override fun isCancelled(): Boolean {
        return this.cancelled
    }

    override fun setCancelled(cancel: Boolean) {
        this.cancelled = true
    }
}