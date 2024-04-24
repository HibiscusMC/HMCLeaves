package com.hibiscusmc.hmcleaves.listener

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.util.getChunkPosition
import com.hibiscusmc.hmcleaves.util.getPositionInChunk
import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.plugin.java.JavaPlugin

sealed class BlockListener<E : Event> : Listener {

    val plugin: HMCLeaves by lazy { JavaPlugin.getPlugin(HMCLeaves::class.java) }

    protected abstract fun handle(event: E)

}

data object LeavesDecayListener : BlockListener<LeavesDecayEvent>() {

    @EventHandler
    override fun handle(event: LeavesDecayEvent) {
        val config = this.plugin.getLeavesConfig()
        val block = event.block
        val world = block.world
        val worldUUID = world.uid
        val chunkPosition = block.getChunkPosition()
        val leavesChunk = plugin.getWorldManager()[worldUUID]?.get(chunkPosition) ?: return
        val position = block.getPositionInChunk()
        leavesChunk.remove(position)
    }

}