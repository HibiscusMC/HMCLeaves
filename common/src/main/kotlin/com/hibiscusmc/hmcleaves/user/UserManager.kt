package com.hibiscusmc.hmcleaves.user

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.packet.sendChunk
import com.hibiscusmc.hmcleaves.world.ChunkPosition
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class UserManager(
    private val plugin: HMCLeaves,
    private val chunksToSend: MutableMap<UUID, MutableSet<ChunkPosition>> = ConcurrentHashMap()
) {

    fun addChunkToSend(player: UUID, chunkPosition: ChunkPosition) {
        val chunks = this.chunksToSend.computeIfAbsent(player) { _ -> hashSetOf() }
    }

    fun sendChunks(player: Player) {
        val uuid = player.uniqueId
        val worldManager = this.plugin.worldManager
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin) {_ ->
            val chunks = this.chunksToSend.remove(uuid) ?: return@runTaskAsynchronously
            for (chunkPosition in chunks) {
                val leavesChunk = worldManager[chunkPosition.world]?.get(chunkPosition) ?: continue
                sendChunk(player, leavesChunk)
            }
        }
    }

}