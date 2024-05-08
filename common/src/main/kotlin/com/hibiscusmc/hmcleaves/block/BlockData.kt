package com.hibiscusmc.hmcleaves.block

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.protocol.world.states.enums.Instrument
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.item.BlockDrops
import com.hibiscusmc.hmcleaves.item.ItemSupplier
import com.hibiscusmc.hmcleaves.item.LogDropReplacement
import com.hibiscusmc.hmcleaves.item.SingleBlockDropReplacement
import com.hibiscusmc.hmcleaves.listener.*
import com.hibiscusmc.hmcleaves.packet.mining.BlockBreakModifier
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import com.hibiscusmc.hmcleaves.world.PositionInChunk
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.entity.Item
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.inventory.ItemStack
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
        }
    }

    data object NOTE : Property<Int>("note", { it.toInt() }) {
        override fun applyToState(state: WrappedBlockState, value: Int) {
            state.note = value
        }
    }

    data object POWERED : Property<Boolean>("powered", { it.toBoolean() }) {
        override fun applyToState(state: WrappedBlockState, value: Boolean) {
            state.isPowered = value
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
                AGE.key to AGE,
                POWERED.key to POWERED
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
    val defaultSettings: BlockSettings,
    val blockSupplier: (
        id: String,
        visualMaterial: Material,
        worldMaterial: Material,
        properties: Map<Property<*>, *>,
        itemSupplier: ItemSupplier,
        blockDrops: BlockDrops,
        connectsTo: Set<String>,
        blockBreakModifier: BlockBreakModifier?,
        settings: BlockSettings
    ) -> BlockData
) {
    LEAVES(
        SingleBlockDropReplacement(),
        BlockSettings.EMPTY,
        { id, visualMaterial, _, properties, itemSupplier, blockDrops, _, modifier, settings ->
            BlockData.createLeaves(id, visualMaterial, properties, itemSupplier, blockDrops, modifier, settings)
        }),
    LOG(
        LogDropReplacement(),
        BlockSettings.EMPTY,
        { id, visualMaterial, worldMaterial, properties, itemSupplier, blockDrops, _, modifier, settings ->
            BlockData.createLog(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                itemSupplier,
                blockDrops,
                modifier,
                settings
            )
        }),
    SUGAR_CANE(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, _, properties, itemSupplier, blockDrops, _, _, settings ->
            BlockData.createSugarcane(id, visualMaterial, properties, itemSupplier, blockDrops, settings)
        }),
    SAPLING(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, _, properties, itemSupplier, blockDrops, _, _, settings ->
            BlockData.createSapling(id, visualMaterial, properties, itemSupplier, blockDrops, settings)
        }),
    CAVE_VINES(
        SingleBlockDropReplacement(),
        BlockSettings.ALL,
        { id, visualMaterial, worldMaterial, properties, itemSupplier, blockDrops, connectsTo, _, settings ->
            BlockData.createCaveVines(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                itemSupplier,
                blockDrops,
                connectsTo,
                settings
            )
        }),
    CAVE_VINES_PLANT(
        SingleBlockDropReplacement(),
        BlockSettings.ALL,
        { id, visualMaterial, worldMaterial, properties, itemSupplier, blockDrops, connectsTo, _, settings ->
            BlockData.createCaveVinesPlant(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                itemSupplier,
                blockDrops,
                connectsTo,
                settings
            )
        }),
    WEEPING_VINES(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, worldMaterial, properties, itemSupplier, blockDrops, connectsTo, _, settings ->
            BlockData.createWeepingVines(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                itemSupplier,
                blockDrops,
                connectsTo,
                settings
            )
        }),
    WEEPING_VINES_PLANT(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, worldMaterial, properties, itemSupplier, blockDrops, connectsTo, _, settings ->
            BlockData.createWeepingVinesPlant(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                itemSupplier,
                blockDrops,
                connectsTo,
                settings
            )
        }),
    TWISTING_VINES(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, worldMaterial, properties, itemSupplier, blockDrops, connectsTo, _, settings ->
            BlockData.createTwistingVines(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                itemSupplier,
                blockDrops,
                connectsTo,
                settings
            )
        }),
    TWISTING_VINES_PLANT(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, worldMaterial, properties, itemSupplier, blockDrops, connectsTo, _, settings ->
            BlockData.createTwistingVinesPlant(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                itemSupplier,
                blockDrops,
                connectsTo,
                settings
            )
        }),
    KELP(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, worldMaterial, properties, itemSupplier, blockDrops, connectsTo, _, settings ->
            BlockData.createKelp(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                itemSupplier,
                blockDrops,
                connectsTo,
                settings
            )
        }),
    KELP_PLANT(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, worldMaterial, properties, itemSupplier, blockDrops, connectsTo, _, settings ->
            BlockData.createKelpPlant(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                itemSupplier,
                blockDrops,
                connectsTo,
                settings
            )
        }),
    SERVER_SIDE_BLOCK(
        SingleBlockDropReplacement(),
        BlockSettings.EMPTY,
        { id, visualMaterial, worldMaterial, properties, itemSupplier, blockDrops, _, modifier, settings ->
            BlockData.createServerSideBlock(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                itemSupplier,
                blockDrops,
                modifier,
                settings
            )
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
    private val listeners: Map<Class<*>, BlockListener<*>>,
    val blockBreakModifier: BlockBreakModifier? = null,
    private val connectsTo: Set<String> = setOf(),
    val settings: BlockSettings,
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
    },
    private val propertyApplier: (BlockData, WrappedBlockState) -> WrappedBlockState = applier@{ blockData, originalState ->
        if (originalState.type != blockData.packetState.type) {
            return@applier blockData.packetState.clone()
        }
        for (entry in properties) {
            val property: Property<Any> = entry.key as Property<Any>
            val value = entry.value ?: continue
            property.applyToState(originalState, value)
        }
        return@applier originalState
    }
) {

    companion object {
        fun createLeaves(
            id: String,
            visualMaterial: Material,
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops,
            blockBreakModifier: BlockBreakModifier?,
            settings: BlockSettings
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
                ),
                blockBreakModifier = blockBreakModifier,
                settings = settings
            )
        }

        fun createLog(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops,
            blockBreakModifier: BlockBreakModifier?,
            settings: BlockSettings
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
                blockBreakModifier = blockBreakModifier,
                settings = settings
            )
        }

        fun createSugarcane(
            id: String,
            visualMaterial: Material,
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops,
            settings: BlockSettings
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
                blockBreakModifier = null,
                settings = settings
            )
        }

        fun createSapling(
            id: String,
            visualMaterial: Material,
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops,
            settings: BlockSettings
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                visualMaterial,
                BlockType.SAPLING,
                properties,
                itemSupplier,
                blockDrops,
                Collections.unmodifiableMap(
                    hashMapOf(
                        BlockPlaceEvent::class.java to SaplingPlaceListener
                    )
                ),
                blockBreakModifier = null,
                settings = settings
            )
        }

        fun createCaveVines(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops,
            connectsTo: Set<String>,
            settings: BlockSettings,
            propertyApplier: (BlockData, WrappedBlockState) -> WrappedBlockState = applier@{ blockData, originalState ->
                if (originalState.type != blockData.packetState.type) {
                    val cloned = blockData.packetState.clone()
                    if (originalState.type == StateTypes.CAVE_VINES || originalState.type == StateTypes.CAVE_VINES_PLANT) {
                        cloned.isBerries = originalState.isBerries
                    }
                    return@applier cloned
                }
                for (entry in properties) {
                    val property: Property<Any> = entry.key as Property<Any>
                    val value = entry.value ?: continue
                    property.applyToState(originalState, value)
                }
                return@applier originalState
            }
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.CAVE_VINES,
                properties,
                itemSupplier,
                blockDrops,
                mapOf(
                    BlockBreakEvent::class.java to ConnectedBlockFacingDownBlockBreakListener,
                    BlockPistonExtendEvent::class.java to ConnectedBlockFacingDownPistonExtendBreakListener,
                    BlockPistonRetractEvent::class.java to ConnectedBlockFacingDownPistonRetractBreakListener,
                    BlockExplodeEvent::class.java to ConnectedBlockFacingDownBlockExplodeBreakListener,
                    EntityExplodeEvent::class.java to ConnectedBlockFacingDownEntityExplodeBreakListener,
                    BlockPlaceEvent::class.java to PlantFacingDownPlaceListener,
                    RelativeBlockBreakEvent::class.java to PlantFacingDownRelativeBreakListener,
                    BlockSpreadEvent::class.java to PlantFacingDownGrowListener
                ),
                blockBreakModifier = null,
                connectsTo = connectsTo,
                settings = settings,
                propertyApplier = propertyApplier
            )
        }

        fun createCaveVinesPlant(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops,
            connectsTo: Set<String>,
            settings: BlockSettings,
            propertyApplier: (BlockData, WrappedBlockState) -> WrappedBlockState = applier@{ blockData, originalState ->
                if (originalState.type != blockData.packetState.type) {
                    val cloned = blockData.packetState.clone()
                    if (originalState.type == StateTypes.CAVE_VINES || originalState.type == StateTypes.CAVE_VINES_PLANT) {
                        cloned.isBerries = originalState.isBerries
                    }
                    return@applier cloned
                }
                for (entry in properties) {
                    val property: Property<Any> = entry.key as Property<Any>
                    val value = entry.value ?: continue
                    property.applyToState(originalState, value)
                }
                return@applier originalState
            }
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.CAVE_VINES_PLANT,
                properties,
                itemSupplier,
                blockDrops,
                mapOf(
                    BlockBreakEvent::class.java to ConnectedBlockFacingDownBlockBreakListener,
                    BlockPistonExtendEvent::class.java to ConnectedBlockFacingDownPistonExtendBreakListener,
                    BlockPistonRetractEvent::class.java to ConnectedBlockFacingDownPistonRetractBreakListener,
                    BlockExplodeEvent::class.java to ConnectedBlockFacingDownBlockExplodeBreakListener,
                    EntityExplodeEvent::class.java to ConnectedBlockFacingDownEntityExplodeBreakListener,
                    BlockPlaceEvent::class.java to PlantFacingDownPlaceListener,
                    RelativeBlockBreakEvent::class.java to PlantFacingDownRelativeBreakListener,
                    BlockSpreadEvent::class.java to PlantFacingDownGrowListener
                ),
                blockBreakModifier = null,
                connectsTo = connectsTo,
                settings = settings,
                propertyApplier = propertyApplier
            )
        }

        fun createWeepingVines(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops,
            connectsTo: Set<String>,
            settings: BlockSettings
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.WEEPING_VINES,
                properties,
                itemSupplier,
                blockDrops,
                mapOf(
                    BlockBreakEvent::class.java to ConnectedBlockFacingDownBlockBreakListener,
                    BlockPistonExtendEvent::class.java to ConnectedBlockFacingDownPistonExtendBreakListener,
                    BlockPistonRetractEvent::class.java to ConnectedBlockFacingDownPistonRetractBreakListener,
                    BlockExplodeEvent::class.java to ConnectedBlockFacingDownBlockExplodeBreakListener,
                    EntityExplodeEvent::class.java to ConnectedBlockFacingDownEntityExplodeBreakListener,
                    BlockPlaceEvent::class.java to PlantFacingDownPlaceListener,
                    RelativeBlockBreakEvent::class.java to PlantFacingDownRelativeBreakListener,
                    BlockSpreadEvent::class.java to PlantFacingDownGrowListener
                ),
                blockBreakModifier = null,
                connectsTo = connectsTo,
                settings = settings
            )
        }

        fun createWeepingVinesPlant(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops,
            connectsTo: Set<String>,
            settings: BlockSettings
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.WEEPING_VINES_PLANT,
                properties,
                itemSupplier,
                blockDrops,
                mapOf(
                    BlockBreakEvent::class.java to ConnectedBlockFacingDownBlockBreakListener,
                    BlockPistonExtendEvent::class.java to ConnectedBlockFacingDownPistonExtendBreakListener,
                    BlockPistonRetractEvent::class.java to ConnectedBlockFacingDownPistonRetractBreakListener,
                    BlockExplodeEvent::class.java to ConnectedBlockFacingDownBlockExplodeBreakListener,
                    EntityExplodeEvent::class.java to ConnectedBlockFacingDownEntityExplodeBreakListener,
                    BlockPlaceEvent::class.java to PlantFacingDownPlaceListener,
                    RelativeBlockBreakEvent::class.java to PlantFacingDownRelativeBreakListener,
                    BlockSpreadEvent::class.java to PlantFacingDownGrowListener
                ),
                blockBreakModifier = null,
                connectsTo = connectsTo,
                settings = settings
            )
        }

        fun createTwistingVines(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops,
            connectsTo: Set<String>,
            settings: BlockSettings
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.TWISTING_VINES,
                properties,
                itemSupplier,
                blockDrops,
                mapOf(
                    BlockBreakEvent::class.java to ConnectedBlockFacingUpBlockBreakListener,
                    BlockPistonExtendEvent::class.java to ConnectedBlockFacingUpPistonExtendBreakListener,
                    BlockPistonRetractEvent::class.java to ConnectedBlockFacingUpPistonRetractBreakListener,
                    BlockExplodeEvent::class.java to ConnectedBlockFacingUpBlockExplodeBreakListener,
                    EntityExplodeEvent::class.java to ConnectedBlockFacingUpEntityExplodeBreakListener,
                    BlockPlaceEvent::class.java to PlantFacingUpPlaceListener,
                    RelativeBlockBreakEvent::class.java to PlantFacingUpRelativeBreakListener,
                    BlockSpreadEvent::class.java to PlantFacingUpGrowListener
                ),
                blockBreakModifier = null,
                connectsTo = connectsTo,
                settings = settings
            )
        }

        fun createTwistingVinesPlant(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops,
            connectsTo: Set<String>,
            settings: BlockSettings
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.TWISTING_VINES_PLANT,
                properties,
                itemSupplier,
                blockDrops,
                mapOf(
                    BlockBreakEvent::class.java to ConnectedBlockFacingUpBlockBreakListener,
                    BlockPistonExtendEvent::class.java to ConnectedBlockFacingUpPistonExtendBreakListener,
                    BlockPistonRetractEvent::class.java to ConnectedBlockFacingUpPistonRetractBreakListener,
                    BlockExplodeEvent::class.java to ConnectedBlockFacingUpBlockExplodeBreakListener,
                    EntityExplodeEvent::class.java to ConnectedBlockFacingUpEntityExplodeBreakListener,
                    BlockPlaceEvent::class.java to PlantFacingUpPlaceListener,
                    RelativeBlockBreakEvent::class.java to PlantFacingUpRelativeBreakListener,
                    BlockSpreadEvent::class.java to PlantFacingUpGrowListener
                ),
                blockBreakModifier = null,
                connectsTo = connectsTo,
                settings = settings
            )
        }

        fun createKelp(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops,
            connectsTo: Set<String>,
            settings: BlockSettings
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.KELP,
                properties,
                itemSupplier,
                blockDrops,
                mapOf(
                    BlockBreakEvent::class.java to ConnectedBlockFacingUpBlockBreakListener,
                    BlockPistonExtendEvent::class.java to ConnectedBlockFacingUpPistonExtendBreakListener,
                    BlockPistonRetractEvent::class.java to ConnectedBlockFacingUpPistonRetractBreakListener,
                    BlockExplodeEvent::class.java to ConnectedBlockFacingUpBlockExplodeBreakListener,
                    EntityExplodeEvent::class.java to ConnectedBlockFacingUpEntityExplodeBreakListener,
                    BlockPlaceEvent::class.java to PlantFacingUpPlaceListener,
                    RelativeBlockBreakEvent::class.java to PlantFacingUpRelativeBreakListener,
                    BlockSpreadEvent::class.java to PlantFacingUpGrowListener
                ),
                blockBreakModifier = null,
                connectsTo = connectsTo,
                settings = settings
            )
        }

        fun createKelpPlant(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops,
            connectsTo: Set<String>,
            settings: BlockSettings
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.KELP_PLANT,
                properties,
                itemSupplier,
                blockDrops,
                mapOf(
                    BlockBreakEvent::class.java to ConnectedBlockFacingUpBlockBreakListener,
                    BlockPistonExtendEvent::class.java to ConnectedBlockFacingUpPistonExtendBreakListener,
                    BlockPistonRetractEvent::class.java to ConnectedBlockFacingUpPistonRetractBreakListener,
                    BlockExplodeEvent::class.java to ConnectedBlockFacingUpBlockExplodeBreakListener,
                    EntityExplodeEvent::class.java to ConnectedBlockFacingUpEntityExplodeBreakListener,
                    BlockPlaceEvent::class.java to PlantFacingUpPlaceListener,
                    RelativeBlockBreakEvent::class.java to PlantFacingUpRelativeBreakListener,
                    BlockSpreadEvent::class.java to PlantFacingUpGrowListener
                ),
                blockBreakModifier = null,
                connectsTo = connectsTo,
                settings = settings
            )
        }

        fun createServerSideBlock(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, *>,
            itemSupplier: ItemSupplier,
            blockDrops: BlockDrops,
            blockBreakModifier: BlockBreakModifier?,
            settings: BlockSettings
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.SERVER_SIDE_BLOCK,
                properties,
                itemSupplier,
                blockDrops,
                mapOf(),
                blockBreakModifier = blockBreakModifier,
                settings = settings
            )
        }
    }

    fun <E> listen(
        clazz: Class<*>,
        event: E,
        world: World,
        startLocation: Location,
        position: PositionInChunk,
        leavesChunk: LeavesChunk,
        config: LeavesConfig
    ): ListenResult {
        val listener = this.listeners[clazz]
            ?: return ListenResult(this, ListenResultType.PASS_THROUGH)

        listener as BlockListener<E>

        return listener.listen(event, world, startLocation, position, this, leavesChunk, config)
    }

    fun canConnectTo(blockData: BlockData): Boolean {
        return this.id == blockData.id || this.connectsTo.contains(blockData.id)
    }

    fun createItem(): ItemStack? {
        return this.itemSupplier.createItem()
    }

    fun replaceItemDrops(config: LeavesConfig, items: MutableList<Item>) {
        this.blockDrops.replaceItems(config, this, items)
    }

    fun replaceItemStackDrops(config: LeavesConfig, items: MutableList<ItemStack>) {
        this.blockDrops.replaceItemStacks(config, this, items)
    }

    fun getDrops(config: LeavesConfig): List<ItemStack> {
        return this.blockDrops.getDrops(config, this)
    }

    fun applyPropertiesToState(originalState: WrappedBlockState): WrappedBlockState {
        return this.propertyApplier(this, originalState)
//        if (originalState.type != this.packetState.type) {
//            return this.packetState.clone()
//        }
//        for (entry in properties) {
//            val property: Property<Any> = entry.key as Property<Any>
//            val value = entry.value ?: continue
//            property.applyToState(originalState, value)
//        }
//        return originalState
    }

    fun getBlockGlobalId(): Int {
        return this.packetState.globalId
    }

    inline operator fun <reified T> get(property: Property<T>): T? {
        val value = properties[property]
        if (!T::class.java.isInstance(value)) return null
        return T::class.java.cast(value)
    }

    override fun toString(): String {
        return "BlockData(id=$id, " +
                "visualMaterial=$visualMaterial, " +
                "worldMaterial=$worldMaterial, " +
                "blockType=$blockType, " +
                "properties={${
                    properties.map { entry ->
                        "${entry.key}=${entry.value}"
                    }.joinToString()
                }}"
    }

}