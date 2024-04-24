package com.hibiscusmc.hmcleaves.packet

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange
import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockType
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.util.toChunkPosition
import com.hibiscusmc.hmcleaves.util.toPosition
import com.hibiscusmc.hmcleaves.world.*
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.abs

class PacketListener(
    private val plugin: HMCLeaves,
    private val config: LeavesConfig = plugin.getLeavesConfig(),
    private val woldManager: WorldManager = plugin.getWorldManager()
) : PacketListenerAbstract() {


    override fun onPacketSend(event: PacketSendEvent) {
        val player = event.player as? Player ?: return
        val packetType = event.packetType

        when (packetType) {
            PacketType.Play.Server.CHUNK_DATA -> {
                handleChunkSend(event, player.world)
                return
            }

            PacketType.Play.Server.MULTI_BLOCK_CHANGE -> {
                handleMultiBlockChange(event, player.world)
                return
            }

            PacketType.Play.Server.BLOCK_CHANGE -> {
                handleSingeBlockChange(event, player.world)
            }
        }
    }

    override fun onPacketReceive(event: PacketReceiveEvent) {

    }

    private fun handleChunkSend(
        event: PacketSendEvent,
        world: World
    ) {
        editChunk(WrapperPlayServerChunkData(event), world.uid, abs(world.minHeight))
    }

    private fun editChunk(
        packet: WrapperPlayServerChunkData,
        world: UUID,
        heightAdjustment: Int
    ) {
        val column = packet.column
        val chunkX = column.x
        val chunkZ = column.z
        val chunks = column.chunks
        val chunkPos = ChunkPosition(world, chunkX, chunkZ)
        val leavesChunk = this.woldManager[world]?.get(chunkPos) ?: return
        for (entry in leavesChunk.getBlocks()) {
            val position = entry.key
            var chunkLevel = position.y / 16 + heightAdjustment / 16
            chunkLevel = if (position.y < 0) chunkLevel - 1 else chunkLevel
            val chunk = chunks[chunkLevel]
            val data = entry.value ?: continue
            val actualX = position.x
            val actualY = abs(convertCoordToCoordInChunk(position.y))
            val actualZ = position.z
            val state = data.applyPropertiesToState(chunk.get(actualX, actualY, actualZ))

            chunk.set(
                PacketEvents.getAPI().serverManager.version.toClientVersion(),
                actualX,
                actualY,
                actualZ,
                state.globalId
            )
        }
    }

    private fun handleMultiBlockChange(
        event: PacketSendEvent,
        world: World
    ) {
        val worldUUID = world.uid
        val packet = WrapperPlayServerMultiBlockChange(event)
        val chunkPos = packet.chunkPosition.toChunkPosition(worldUUID)

        val leavesChunk = this.woldManager[worldUUID]?.get(chunkPos) ?: return

        val blocks = packet.blocks

        for (block in blocks) {
            val position = Position(worldUUID, block.x, block.y, block.z).toPositionInChunk()
            val data = leavesChunk[position] ?: continue
            val state =
                data.applyPropertiesToState(
                    block.getBlockState(PacketEvents.getAPI().serverManager.version.toClientVersion())
                )
            block.setBlockState(state)
        }
    }

    private fun handleSingeBlockChange(
        event: PacketSendEvent,
        world: World
    ) {
        val worldUUID = world.uid
        val packet = WrapperPlayServerBlockChange(event)
        val position = packet.blockPosition.toPosition(worldUUID)
        val chunkPos = position.getChunkPosition()

        val leavesChunk = this.woldManager[worldUUID]?.get(chunkPos) ?: return
        val data = leavesChunk[position.toPositionInChunk()] ?: return
        event.isCancelled = true

        val state = data.applyPropertiesToState(packet.blockState)

        if (data.blockType == BlockType.LEAVES) {
            plugin.logger.info("Leaves: " +
                    "(${position.x}, ${position.y}, ${position.z}) " +
                    "(${position.toPositionInChunk().x} ${position.toPositionInChunk().y}, ${position.toPositionInChunk().z}) " +
                    "${state.distance}, ${state.isPersistent}")
        }

        val newPacket = WrapperPlayServerBlockChange(
            packet.blockPosition,
            state.globalId
        )
        PacketEvents.getAPI().playerManager.sendPacketSilently(event.player, newPacket);
    }

}