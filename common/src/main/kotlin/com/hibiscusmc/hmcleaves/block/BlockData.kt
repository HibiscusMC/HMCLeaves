package com.hibiscusmc.hmcleaves.block

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.protocol.world.states.enums.Instrument
import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.item.BlockDrops
import com.hibiscusmc.hmcleaves.item.ItemSupplier
import com.hibiscusmc.hmcleaves.item.LogDropReplacement
import com.hibiscusmc.hmcleaves.item.SingleBlockDropReplacement
import com.hibiscusmc.hmcleaves.listener.*
import com.hibiscusmc.hmcleaves.packet.mining.BlockBreakManager
import com.hibiscusmc.hmcleaves.packet.mining.BlockBreakModifier
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import com.hibiscusmc.hmcleaves.world.PositionInChunk
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.event.Event
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

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

    data object AGE : Property<Int>("age", { it.toInt() }) {
        override fun applyToState(state: WrappedBlockState, value: Int) {
            state.age = value
        }
    }

    companion object {
        private val PROPERTY_KEYS by lazy {
            hashMapOf(
                DISTANCE.key to DISTANCE,
                PERSISTENT.key to PERSISTENT,
                INSTRUMENT.key to INSTRUMENT,
                NOTE.key to NOTE,
                AGE.key to AGE
            )
        }

        fun <T> getPropertyByKey(key: String): Property<T>? {
            val property = PROPERTY_KEYS[key] ?: return null
            return property as Property<T>
        }
    }

}

enum class BlockType(
    val defaultBlockDrops: BlockDrops,
    val blockSupplier: (
        id: String,
        visualMaterial: Material,
        worldMaterial: Material,
        properties: Map<Property<*>, *>,
        itemSupplier: ItemSupplier,
        blockDrops: BlockDrops
    ) -> BlockData
) {
    LEAVES(
        SingleBlockDropReplacement(),
        { id, visualMaterial, _, properties, itemSupplier, blockDrops ->
            BlockData.createLeaves(id, visualMaterial, properties, itemSupplier, blockDrops)
        }),
    LOG(
        LogDropReplacement(),
        { id, visualMaterial, worldMaterial, properties, itemSupplier, blockDrops ->
            BlockData.createLog(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                itemSupplier,
                blockDrops
            )
        }),
    SUGAR_CANE(
        SingleBlockDropReplacement(),
        { id, visualMaterial, _, properties, itemSupplier, blockDrops ->
            BlockData.createSugarcane(id, visualMaterial, properties, itemSupplier, blockDrops)
        })
}

class BlockData(
    val id: String,
    val visualMaterial: Material,
    val worldMaterial: Material,
    val blockType: BlockType,
    val properties: Map<Property<*>, *>,
    private val itemSupplier: ItemSupplier,
    private val blockDrops: BlockDrops,
    private val listeners: Map<Class<out Event>, BlockListener<*>>,
    val placeableInEntities: Boolean = false,
    val blockBreakModifier: BlockBreakModifier? = null,
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
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                visualMaterial,
                BlockType.LEAVES,
                properties,
                itemSupplier,
                blockDrops,
                hashMapOf(
                    LeavesDecayEvent::class.java to LeavesDecayListener,
                    BlockPistonExtendEvent::class.java to LeavesPistonExtendListener,
                    BlockPistonRetractEvent::class.java to LeavesPistonRetractListener,
                )
            )
        }

        fun createLog(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.LOG,
                properties,
                itemSupplier,
                blockDrops,
                hashMapOf(
                    BlockPlaceEvent::class.java to LogPlaceListener
                ),
                blockBreakModifier = BlockBreakManager.LOG_BREAK_MODIFIER
            )
        }

        fun createSugarcane(
            id: String,
            visualMaterial: Material,
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                visualMaterial,
                BlockType.SUGAR_CANE,
                properties,
                itemSupplier,
                blockDrops,
                Collections.unmodifiableMap(
                    hashMapOf(
                        BlockPlaceEvent::class.java to SugarCanePlaceListener,
                        BlockGrowEvent::class.java to SugarCaneGrowListener
                    )
                ),
                blockBreakModifier = null
            )
        }
    }

    fun listen(
        event: Event,
        position: PositionInChunk,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        val listener = this.listeners[event::class.java] ?: return ListenResult(this, ListenResultType.PASS_THROUGH)
        return listener.listen(event, position, this, leavesChunk, config)
    }

    fun createItem(): ItemStack? {
        return this.itemSupplier.createItem()
    }

    fun replaceDrops(config: LeavesConfig, items: MutableList<Item>) {
        this.blockDrops.replaceItems(config, this, items)
    }

    fun getDrops(config: LeavesConfig): List<ItemStack> {
        return this.blockDrops.getDrops(config, this)
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

    fun getBlockGlobalId(): Int {
        return this.packetState.globalId
    }

    inline operator fun <reified T> get(property: Property<T>): T? {
        val value = properties[property]
        if (!T::class.java.isInstance(value)) return null
        return T::class.java.cast(value)
    }

}