package com.hibiscusmc.hmcleaves.listener

import org.bukkit.event.Event
import org.bukkit.event.block.BlockPlaceEvent

interface BlockListener<E: Event> {

    fun listen(event: E)

}

object LEAVES_PLACE_LISTENER : BlockListener<BlockPlaceEvent> {

    override fun listen(event: BlockPlaceEvent) {
        TODO("Not yet implemented")
    }

}