package com.hibiscusmc.hmcleaves.packet

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.protocol.potion.PotionTypes
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes
import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.github.retrooper.packetevents.wrapper.play.server.*
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange.EncodedBlock
import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockChecker
import com.hibiscusmc.hmcleaves.block.BlockType
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.packet.wrapper.WrapperPlayServerWorldEvent
import com.hibiscusmc.hmcleaves.util.toChunkPosition
import com.hibiscusmc.hmcleaves.util.toPosition
import com.hibiscusmc.hmcleaves.util.toVector3i
import com.hibiscusmc.hmcleaves.world.*
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.abs


class PacketListener(
    private val plugin: HMCLeaves,
    private val blockChecker: BlockChecker,
    private val config: LeavesConfig = plugin.leavesConfig,
    private val woldManager: WorldManager = plugin.worldManager
) : PacketListenerAbstract() {


    override fun onPacketSend(event: PacketSendEvent) {
        val player = event.player as? Player ?: return
        val packetType = event.packetType

        when (packetType) {
            PacketType.Play.Server.CHUNK_DATA -> {
                handleChunkSend(player, event, player.world)
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
        val player = event.player as? Player ?: return
        val packetType = event.packetType
        if (packetType == PacketType.Play.Client.PLAYER_DIGGING) {
            handlePlayerDigging(event, player.world.uid);
        }
    }

    private fun handleChunkSend(
        player: Player,
        event: PacketSendEvent,
        world: World
    ) {
        editChunk(player, WrapperPlayServerChunkData(event), world.uid, abs(world.minHeight))
    }

    private fun editChunk(
        player: Player,
        packet: WrapperPlayServerChunkData,
        world: UUID,
        heightAdjustment: Int
    ) {
        val column = packet.column
        val chunkX = column.x
        val chunkZ = column.z
        val chunks = column.chunks
        val chunkPos = ChunkPosition(world, chunkX, chunkZ)
        val leavesChunk = this.woldManager[world]?.get(chunkPos) ?: run {
            this.plugin.userManager.addChunkToSend(player.uniqueId, chunkPos)
            return
        }
        if (!leavesChunk.isLoaded()) return
        val blocks = leavesChunk.getBlocks().toMutableMap()
        blocks.putAll(leavesChunk.getDefaultBlocks())
        for (entry in blocks) {
            val position = entry.key
            var chunkLevel = position.y / 16 + heightAdjustment / 16
            chunkLevel = if (position.y < 0) chunkLevel - 1 else chunkLevel
            val chunk = chunks[chunkLevel]
            val data = entry.value ?: continue
            val actualX = position.x
            val actualY = abs(convertCoordToCoordInChunk(position.y))
            val actualZ = position.z
            val state = data.applyPropertiesToState(chunk.get(actualX, actualY, actualZ))
            this.blockChecker.addPositionToCheck(position.toPosition(chunkPos))
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
            val position = Position(worldUUID, block.x, block.y, block.z)
            val positionInChunk = position.toPositionInChunk()
            val blockState = block.getBlockState(PacketEvents.getAPI().serverManager.version.toClientVersion())
            this.blockChecker.addPositionToCheck(position)
            val material = SpigotConversionUtil.toBukkitBlockData(blockState).material
            val defaultData = this.config.getDefaultBlockData(material)
            val data = leavesChunk[positionInChunk] ?: run {
                if (defaultData == null) return@run null
                leavesChunk[positionInChunk] = defaultData
                return@run defaultData
            }
            if (data == null) continue
            val state =
                data.applyPropertiesToState(blockState)
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
        val positionInChunk = position.toPositionInChunk()
        val chunkPos = position.getChunkPosition()
        val material = SpigotConversionUtil.toBukkitBlockData(packet.blockState).material
        val defaultData = this.config.getDefaultBlockData(material)
        var leavesChunk = this.woldManager[worldUUID]?.get(chunkPos)
        if (defaultData == null && leavesChunk == null) return
        if (leavesChunk == null) {
            leavesChunk = this.woldManager.getOrAdd(worldUUID).getOrAdd(chunkPos)
            leavesChunk[positionInChunk] = defaultData!!
        }

        val data = leavesChunk[positionInChunk] ?: return

        this.blockChecker.addPositionToCheck(position)

        event.isCancelled = true

        val state = data.applyPropertiesToState(packet.blockState)

        val newPacket = WrapperPlayServerBlockChange(
            packet.blockPosition,
            state.globalId
        )
        PacketEvents.getAPI().playerManager.sendPacketSilently(event.player, newPacket);
    }

    private fun handlePlayerDigging(event: PacketReceiveEvent, world: UUID) {
        val packet = WrapperPlayClientPlayerDigging(event)
        val player = event.player as? Player ?: return
        if (player.gameMode == GameMode.CREATIVE) return
        val diggingAction = packet.action
        val blockPosition = packet.blockPosition
        val position = Position(
            world,
            blockPosition.getX(),
            blockPosition.getY(),
            blockPosition.getZ()
        )
        val worldManager = this.plugin.worldManager
        val blockData = worldManager[position] ?: return
        if (blockData.blockBreakModifier == null) return
        val blockBreakManager = this.plugin.blockBreakManager

        if (diggingAction == DiggingAction.START_DIGGING) {
            if (blockBreakManager.startBlockBreak(
                    player,
                    position,
                    blockData
                )
            ) {
                sendMiningFatigue(player)
            }
            return
        }
        if (diggingAction == DiggingAction.CANCELLED_DIGGING) {
            blockBreakManager.cancelBlockBreak(player)
            removeMiningFatigue(player)
            return
        }
        if (diggingAction == DiggingAction.FINISHED_DIGGING) {
            blockBreakManager.cancelBlockBreak(player)
            removeMiningFatigue(player)
        }
    }

}

fun sendSingleBlockChange(
    position: Position,
    blockData: BlockData,
    player: Player
) {
    val id = WrappedBlockState.getDefaultState(
        PacketEvents.getAPI().serverManager.version.toClientVersion(),
        SpigotConversionUtil.fromBukkitBlockData(blockData).type
    ).globalId
    val packet = WrapperPlayServerBlockChange(position.toVector3i(), id)
    PacketEvents.getAPI().playerManager.sendPacketSilently(player, packet);
}

fun sendBlocksInChunk(
    chunkPosition: IndexedChunkPosition,
    positions: Map<PositionInChunk, BlockData>,
    players: Collection<Player>
) {
    val blocks = arrayOfNulls<EncodedBlock>(positions.size)
    var index = 0;
    for (entry in positions) {
        val position = entry.key
        blocks[index] =
            EncodedBlock(SpigotConversionUtil.fromBukkitBlockData(entry.value), position.x, position.y, position.z)
        index++
    }
    val playerManager = PacketEvents.getAPI().playerManager
    for (player in players) {
        val packet =
            WrapperPlayServerMultiBlockChange(
                Vector3i(chunkPosition.x, chunkPosition.yIndex, chunkPosition.z),
                true,
                blocks
            )
        playerManager.sendPacketSilently(player, packet)
    }
}

fun sendChunk(
    player: Player,
    leavesChunk: LeavesChunk
) {
    val chunkBlocks = leavesChunk.getBlocks().toMutableMap()
    chunkBlocks.putAll(leavesChunk.getDefaultBlocks())
    val chunkPosition = leavesChunk.position
    val blocks: Array<EncodedBlock?> = chunkBlocks.map { entry ->
        val data = entry.value ?: return@map null
        val position = entry.key.toPosition(chunkPosition)
        return@map EncodedBlock(data.getBlockGlobalId(), position.x, position.y, position.z)
    }.filterNotNull().toTypedArray()
    val packet = WrapperPlayServerMultiBlockChange(chunkPosition.toVector3i(), false, blocks)
    PacketEvents.getAPI().playerManager.sendPacketSilently(player, packet)
}

fun sendBlockBreakAnimation(player: Player, position: Position, entityId: Int, damage: Byte) {
    val packet = WrapperPlayServerBlockBreakAnimation(
        entityId,
        position.toVector3i(),
        damage
    )
    PacketEvents.getAPI().playerManager.sendPacket(
        player,
        packet
    )
}

fun sendBlockBroken(player: Player, position: Position, blockId: Int) {
    val packet = WrapperPlayServerWorldEvent(
        2001,
        Vector3i(position.x, position.y, position.z),
        blockId,
        false
    )
    PacketEvents.getAPI().playerManager.sendPacket(
        player,
        packet
    )
}

fun sendMiningFatigue(player: Player) {
    val packet = WrapperPlayServerEntityEffect(
        player.entityId,
        PotionTypes.MINING_FATIGUE,
        -1,
        Int.MAX_VALUE,
        0.toByte()
    )
    PacketEvents.getAPI().playerManager.sendPacket(
        player,
        packet
    )
}

fun removeMiningFatigue(player: Player) {
    val packet = WrapperPlayServerRemoveEntityEffect(
        player.entityId,
        PotionTypes.MINING_FATIGUE
    )
    PacketEvents.getAPI().playerManager.sendPacket(
        player,
        packet
    )
}