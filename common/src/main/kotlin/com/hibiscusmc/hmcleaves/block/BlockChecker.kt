package com.hibiscusmc.hmcleaves.block

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.packet.sendBlocksInChunk
import com.hibiscusmc.hmcleaves.world.IndexedChunkPosition
import com.hibiscusmc.hmcleaves.world.Position
import com.hibiscusmc.hmcleaves.world.PositionInChunk
import com.hibiscusmc.hmcleaves.world.WorldManager
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class BlockChecker(
    private val plugin: HMCLeaves,
    private val worldManager: WorldManager
) {

    private val blocksToTick = Collections.newSetFromMap<Position>(ConcurrentHashMap())
    private lateinit var ticker: BukkitTask

    fun start() {
        if (this::ticker.isInitialized) {
            throw IllegalStateException("Already started")
        }
        val blocksToSend =
            hashMapOf<IndexedChunkPosition, MutableMap<PositionInChunk, org.bukkit.block.data.BlockData>>()
        this.ticker = Bukkit.getScheduler().runTaskTimer(this.plugin, Runnable {
            this.blocksToTick.removeIf { position ->
                val indexedChunkPosition = position.getIndexedChunkPosition()
                val chunkPosition = position.getChunkPosition()
                val chunk = worldManager[position.world]?.get(chunkPosition) ?: return@removeIf true
                val data = worldManager[position] ?: return@removeIf true
                val blockData = position.toLocation().block.blockData
                if (data.worldMaterial != blockData.material) {
                    chunk.remove(position.toPositionInChunk(), false)
                    val toSend = blocksToSend.computeIfAbsent(indexedChunkPosition) { _ -> hashMapOf() }
                    toSend[position.toPositionInChunk()] = blockData
                }
                return@removeIf true
            }

            for (entry in blocksToSend) {
                sendBlocksInChunk(entry.key, entry.value, Bukkit.getOnlinePlayers())
            }

        }, 1, 1)
    }

    fun stop() {
        this.ticker.cancel()
    }

    fun addPositionToCheck(position: Position) {
        this.blocksToTick.add(position)
    }

}