package com.hibiscusmc.hmcleaves.listener

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.packet.sendSound
import com.hibiscusmc.hmcleaves.util.toPosition
import com.hibiscusmc.hmcleaves.world.WorldManager
import org.bukkit.Bukkit
import org.bukkit.GameEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.GenericGameEvent

class SoundListener(
    private val plugin: HMCLeaves,
    private val worldManager: WorldManager = plugin.worldManager
) : Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onGameEvent(event: GenericGameEvent) {
        val gameEvent: GameEvent = event.event;
        if (gameEvent == GameEvent.HIT_GROUND) {
            val player = event.entity as? Player ?: return
            val position = event.location.clone().subtract(0.0, 1.0, 0.0).toPosition() ?: return
            val blockData = worldManager[position] ?: return
            val hitSound = blockData.blockSoundData.hitSound ?: return
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, Runnable {
                sendSound(hitSound, position, listOf(player))
            })
            return
        }
        if (gameEvent == GameEvent.STEP) {
            val player = event.entity as? Player ?: return
            val position = event.location.clone().subtract(0.0, 1.0, 0.0).toPosition() ?: return
            val blockData = worldManager[position] ?: return
            val stepSound = blockData.blockSoundData.stepSound ?: return
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, Runnable {
                sendSound(stepSound, position, listOf(player))
            })
            return;
        }
    }

}