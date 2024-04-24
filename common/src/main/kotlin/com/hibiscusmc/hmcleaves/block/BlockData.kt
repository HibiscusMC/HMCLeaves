package com.hibiscusmc.hmcleaves.block

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.protocol.world.states.enums.Instrument
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes
import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.listener.*
import com.hibiscusmc.hmcleaves.pdc.PDCUtil
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import com.hibiscusmc.hmcleaves.world.PositionInChunk
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.Material
import org.bukkit.event.Event
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

sealed class Property<T>(val key: String, val converter: (String) -> T) {

    abstract fun applyToState(state: WrappedBlockState, value: T)

    data object DISTANCE : Property<Int>("distance", { it.toInt() }) {
        override fun applyToState(state: WrappedBlockState, value: Int) {
            state.distance = value
        }
    }

    data object PERSISTENT : Property<Boolean>("persistent", { it.toBoolean() }) {
        override fun applyToState(state: WrappedBlockState, value: Boolean) {
            state.isPersistent = value
        }
    }

    data object INSTRUMENT : Property<Instrument>("instrument", { Instrument.valueOf(it.uppercase()) }) {
        override fun applyToState(state: WrappedBlockState, value: Instrument) {
            state.instrument = value
            JavaPlugin.getPlugin(HMCLeaves::class.java).logger.info("Applied instrument ${value}, ${state.instrument}")
        }
    }

    data object NOTE : Property<Int>("note", { it.toInt() }) {
        override fun applyToState(state: WrappedBlockState, value: Int) {
            state.note = value
        }
    }

    companion object {
        private val PROPERTY_KEYS by lazy {
            hashMapOf(
                Property.DISTANCE.key to Property.DISTANCE,
                Property.PERSISTENT.key to Property.PERSISTENT,
                Property.INSTRUMENT.key to Property.INSTRUMENT,
                Property.NOTE.key to Property.NOTE
            )
        }

        fun <T> getPropertyByKey(key: String): Property<T>? {
            val property = PROPERTY_KEYS[key] ?: return null
            return property as Property<T>
        }
    }

}

enum class BlockType(
    val blockSupplier: (
        id: String,
        visualMaterial: Material,
        worldMaterial: Material,
        properties: Map<Property<*>, *>
    ) -> BlockData
) {
    LEAVES({ id, visualMaterial, worldMaterial, properties ->
        BlockData.createLeaves(id, visualMaterial, worldMaterial, properties)
    }),
    LOG({ id, visualMaterial, worldMaterial, properties ->
        BlockData.createLog(id, visualMaterial, worldMaterial, properties)
    })
}

class BlockData(
    val id: String,
    private val visualMaterial: Material,
    private val worldMaterial: Material,
    val blockType: BlockType,
    val properties: Map<Property<*>, *>,
    private val listeners: Map<Class<out Event>, BlockListener<*>>,
    val placementRule: PlacementRule,
    private val packetState: WrappedBlockState = run {
        val state = WrappedBlockState.getDefaultState(
            PacketEvents.getAPI().serverManager.version.toClientVersion(),
            SpigotConversionUtil.fromBukkitBlockData(visualMaterial.createBlockData()).type
        )
        for (entry in properties) {
            val property: Property<Any> = entry.key as Property<Any>
            val value = entry.value ?: continue
            property.applyToState(state, value)
        }
        return@run state
    }
) {

    companion object {
        fun createLeaves(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, *>
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.LEAVES,
                properties,
                mapOf(
                    LeavesDecayEvent::class.java to LeavesDecayListener,
                    BlockPistonExtendEvent::class.java to LeavesPistonExtendListener,
                    BlockPistonRetractEvent::class.java to LeavesPistonRetractListener,
                ),
                PlacementRule.Companion.ALLOW
            )
        }

        fun createLog(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, *>
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.LOG,
                properties,
                mapOf(
//                    BlockPlaceEvent::class.java to LeavesPlaceListener
                ),
                PlacementRule.Companion.ALLOW
            )
        }

    }

    fun listen(
        event: Event,
        position: PositionInChunk,
        leavesChunk: LeavesChunk
    ) : ListenResult {
        val listener = this.listeners[event::class.java] ?: return ListenResult.PASS_THROUGH
        return listener.listen(event, position, this, leavesChunk)
    }

    // todo
    fun createItem(): ItemStack {
        val item = ItemStack(worldMaterial)
        PDCUtil.setItemId(item, this.id)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName(id)
        item.setItemMeta(meta)
        return item
    }

    fun applyPropertiesToState(originalState: WrappedBlockState): WrappedBlockState {
        if (originalState.type != this.packetState.type) {
            return this.packetState.clone()
        }
        for (entry in properties) {
            val property: Property<Any> = entry.key as Property<Any>
            val value = entry.value ?: continue
            property.applyToState(originalState, value)
        }
        return originalState
    }

    inline operator fun <reified T> get(property: Property<T>): T? {
        val value = properties[property]
        if (!T::class.java.isInstance(value)) return null
        return T::class.java.cast(value)
    }

}