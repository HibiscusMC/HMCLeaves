package com.hibiscusmc.hmcleaves.world

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.Material

sealed class Property<T>(val key: String) {

    data object DISTANCE : Property<Int>("distance")
    data object PERSISTENT : Property<Int>("persistent")

}

enum class BlockType {
    LEAVES,
    LOG
}

class BlockData(
    val id: String,
    private val material: Material,
    blockType: BlockType,
    val properties: Map<Property<*>, *>,
    private val packetState: WrappedBlockState = WrappedBlockState.getDefaultState(
        PacketEvents.getAPI().serverManager.version.toClientVersion(),
        SpigotConversionUtil.fromBukkitBlockData(material.createBlockData()).type
    )
) {

    companion object {
        fun createLeaves(
            id: String,
            material: Material,
            properties: Map<Property<*>, *>
        ): BlockData {
            return BlockData(
                id,
                material,
                BlockType.LEAVES,
                properties
            )
        }
    }

    fun getPacketState() : WrappedBlockState {
        return this.packetState.clone()
    }

    fun getBlockDataGlobalId() : Int {
        return this.packetState.globalId
    }

    inline operator fun <reified T> get(property: Property<T>): T? {
        val value = properties[property]
        if (!T::class.java.isInstance(value)) return null
        return T::class.java.cast(value)
    }

}