package com.hibiscusmc.hmcleaves.block

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.protocol.world.states.enums.East
import com.github.retrooper.packetevents.protocol.world.states.enums.Instrument
import com.github.retrooper.packetevents.protocol.world.states.enums.North
import com.github.retrooper.packetevents.protocol.world.states.enums.South
import com.github.retrooper.packetevents.protocol.world.states.enums.West
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.item.BlockDrops
import com.hibiscusmc.hmcleaves.item.LogDropReplacement
import com.hibiscusmc.hmcleaves.item.SingleBlockDropReplacement
import com.hibiscusmc.hmcleaves.listener.BlockListener
import com.hibiscusmc.hmcleaves.listener.ConnectedBlockFacingDownBlockBreakListener
import com.hibiscusmc.hmcleaves.listener.ConnectedBlockFacingDownBlockExplodeBreakListener
import com.hibiscusmc.hmcleaves.listener.ConnectedBlockFacingDownEntityExplodeBreakListener
import com.hibiscusmc.hmcleaves.listener.ConnectedBlockFacingDownPistonExtendBreakListener
import com.hibiscusmc.hmcleaves.listener.ConnectedBlockFacingDownPistonRetractBreakListener
import com.hibiscusmc.hmcleaves.listener.ConnectedBlockFacingUpBlockBreakListener
import com.hibiscusmc.hmcleaves.listener.ConnectedBlockFacingUpBlockExplodeBreakListener
import com.hibiscusmc.hmcleaves.listener.ConnectedBlockFacingUpEntityExplodeBreakListener
import com.hibiscusmc.hmcleaves.listener.ConnectedBlockFacingUpPistonExtendBreakListener
import com.hibiscusmc.hmcleaves.listener.ConnectedBlockFacingUpPistonRetractBreakListener
import com.hibiscusmc.hmcleaves.listener.LeavesDecayListener
import com.hibiscusmc.hmcleaves.listener.LeavesPistonExtendListener
import com.hibiscusmc.hmcleaves.listener.LeavesPistonRetractListener
import com.hibiscusmc.hmcleaves.listener.LeavesPlaceListener
import com.hibiscusmc.hmcleaves.listener.ListenResult
import com.hibiscusmc.hmcleaves.listener.ListenResultType
import com.hibiscusmc.hmcleaves.listener.LogPlaceListener
import com.hibiscusmc.hmcleaves.listener.LogStripListener
import com.hibiscusmc.hmcleaves.listener.PlantFacingDownGrowListener
import com.hibiscusmc.hmcleaves.listener.PlantFacingDownPlaceListener
import com.hibiscusmc.hmcleaves.listener.PlantFacingDownRelativeBreakListener
import com.hibiscusmc.hmcleaves.listener.PlantFacingUpGrowListener
import com.hibiscusmc.hmcleaves.listener.PlantFacingUpPlaceListener
import com.hibiscusmc.hmcleaves.listener.PlantFacingUpRelativeBreakListener
import com.hibiscusmc.hmcleaves.listener.RelativeBlockBreakEvent
import com.hibiscusmc.hmcleaves.listener.SaplingGrowListener
import com.hibiscusmc.hmcleaves.listener.SaplingPlaceListener
import com.hibiscusmc.hmcleaves.listener.SugarCaneGrowListener
import com.hibiscusmc.hmcleaves.listener.SugarCanePlaceListener
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import com.hibiscusmc.hmcleaves.world.PositionInChunk
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Item
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.StructureGrowEvent
import org.bukkit.inventory.ItemStack
import java.util.Collections

sealed class Property<T>(val key: String, val converter: (String) -> T) {

    abstract fun applyToState(state: WrappedBlockState, value: T)

    abstract fun getFromState(state: WrappedBlockState): T

    data object DISTANCE : Property<Int>("distance", { it.toInt() }) {
        override fun applyToState(state: WrappedBlockState, value: Int) {
            state.distance = value
        }

        override fun getFromState(state: WrappedBlockState): Int {
            return state.distance
        }
    }

    data object PERSISTENT : Property<Boolean>("persistent", { it.toBoolean() }) {
        override fun applyToState(state: WrappedBlockState, value: Boolean) {
            state.isPersistent = value
        }

        override fun getFromState(state: WrappedBlockState): Boolean {
            return state.isPersistent
        }
    }

    data object INSTRUMENT : Property<Instrument>("instrument", { Instrument.valueOf(it.uppercase()) }) {
        override fun applyToState(state: WrappedBlockState, value: Instrument) {
            state.instrument = value
        }

        override fun getFromState(state: WrappedBlockState): Instrument {
            return state.instrument
        }
    }

    data object NOTE : Property<Int>("note", { it.toInt() }) {
        override fun applyToState(state: WrappedBlockState, value: Int) {
            state.note = value
        }

        override fun getFromState(state: WrappedBlockState): Int {
            return state.note
        }
    }

    data object POWERED : Property<Boolean>("powered", { it.toBoolean() }) {
        override fun applyToState(state: WrappedBlockState, value: Boolean) {
            state.isPowered = value
        }

        override fun getFromState(state: WrappedBlockState): Boolean {
            return state.isPowered
        }
    }

    data object AGE : Property<Int>("age", { it.toInt() }) {
        override fun applyToState(state: WrappedBlockState, value: Int) {
            state.age = value
        }

        override fun getFromState(state: WrappedBlockState): Int {
            return state.age
        }
    }

    data object STAGE : Property<Int>("stage", { it.toInt() }) {
        override fun applyToState(state: WrappedBlockState, value: Int) {
            state.stage = value
        }

        override fun getFromState(state: WrappedBlockState): Int {
            return state.stage
        }
    }

    data object NORTH : Property<North>("north", { North.valueOf(it.uppercase()) }) {
        override fun applyToState(state: WrappedBlockState, value: North) {
            state.north = value
        }

        override fun getFromState(state: WrappedBlockState): North {
            return state.north
        }
    }

    data object SOUTH : Property<South>("east", { South.valueOf(it.uppercase()) }) {
        override fun applyToState(state: WrappedBlockState, value: South) {
            state.south = value
        }

        override fun getFromState(state: WrappedBlockState): South {
            return state.south
        }
    }

    data object EAST : Property<East>("east", { East.valueOf(it.uppercase()) }) {
        override fun applyToState(state: WrappedBlockState, value: East) {
            state.east = value
        }

        override fun getFromState(state: WrappedBlockState): East {
            return state.east
        }
    }

    data object WEST : Property<West>("west", { West.valueOf(it.uppercase()) }) {
        override fun applyToState(state: WrappedBlockState, value: West) {
            state.west = value
        }

        override fun getFromState(state: WrappedBlockState): West {
            return state.west
        }
    }

    data object DISARMED : Property<Boolean>("disarmed", { it.toBoolean() }) {
        override fun applyToState(state: WrappedBlockState, value: Boolean) {
            state.isDisarmed = value
        }

        override fun getFromState(state: WrappedBlockState): Boolean {
            return state.isDisarmed
        }
    }

    data object ATTACHED : Property<Boolean>("attached", { it.toBoolean() }) {
        override fun applyToState(state: WrappedBlockState, value: Boolean) {
            state.isAttached = value
        }

        override fun getFromState(state: WrappedBlockState): Boolean {
            return state.isAttached
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
                POWERED.key to POWERED,
                STAGE.key to STAGE,
                NORTH.key to NORTH,
                SOUTH.key to SOUTH,
                EAST.key to EAST,
                WEST.key to WEST,
                DISARMED.key to DISARMED,
                ATTACHED.key to ATTACHED
            )
        }

        fun <T : Any> getPropertyByKey(key: String): Property<T>? {
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
        properties: Map<Property<*>, Any>,
        blockMechanics: BlockMechanics,
        connectsTo: Set<String>,
        propertyApplier: BlockData.PropertyApplier?
    ) -> BlockData
) {
    LEAVES(
        SingleBlockDropReplacement(),
        BlockSettings.EMPTY,
        { id, visualMaterial, _, properties, blockMechanics, _, _ ->
            BlockData.createLeaves(
                id,
                visualMaterial,
                properties,
                blockMechanics
            )
        }),
    LOG(
        LogDropReplacement(),
        BlockSettings.EMPTY,
        { id, visualMaterial, worldMaterial, properties, blockMechanics, _, propertyApplier ->
            BlockData.createLog(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                blockMechanics,
                propertyApplier ?: BlockData.DEFAULT_PROPERTY_APPLIER
            )
        }),
    STRIPPED_LOG(
        LogDropReplacement(),
        BlockSettings.EMPTY,
        { id, visualMaterial, worldMaterial, properties, blockMechanics, _, propertyApplier ->
            BlockData.createStrippedLog(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                blockMechanics,
                propertyApplier ?: BlockData.DEFAULT_PROPERTY_APPLIER
            )
        }),
    SUGAR_CANE(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, worldMaterial, properties, blockMechanics, _, _ ->
            BlockData.createSugarcane(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                blockMechanics
            )
        }),
    SAPLING(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, _, properties, blockMechanics, _, _ ->
            BlockData.createSapling(
                id,
                visualMaterial,
                properties,
                blockMechanics
            )
        }),
    CAVE_VINES(
        SingleBlockDropReplacement(),
        BlockSettings.ALL,
        { id, visualMaterial, worldMaterial, properties, blockMechanics, connectsTo, _ ->
            BlockData.createCaveVines(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                blockMechanics,
                connectsTo
            )
        }),
    CAVE_VINES_PLANT(
        SingleBlockDropReplacement(),
        BlockSettings.ALL,
        { id, visualMaterial, worldMaterial, properties, blockMechanics, connectsTo, _ ->
            BlockData.createCaveVinesPlant(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                blockMechanics,
                connectsTo
            )
        }),
    WEEPING_VINES(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, worldMaterial, properties, blockMechanics, connectsTo, _ ->
            BlockData.createWeepingVines(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                blockMechanics,
                connectsTo
            )
        }),
    WEEPING_VINES_PLANT(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, worldMaterial, properties, blockMechanics, connectsTo, _ ->
            BlockData.createWeepingVinesPlant(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                blockMechanics,
                connectsTo
            )
        }),
    TWISTING_VINES(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, worldMaterial, properties, blockMechanics, connectsTo, _ ->
            BlockData.createTwistingVines(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                blockMechanics,
                connectsTo
            )
        }),
    TWISTING_VINES_PLANT(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, worldMaterial, properties, blockMechanics, connectsTo, _ ->
            BlockData.createTwistingVinesPlant(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                blockMechanics,
                connectsTo
            )
        }),
    KELP(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, worldMaterial, properties, blockMechanics, connectsTo, _ ->
            BlockData.createKelp(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                blockMechanics,
                connectsTo
            )
        }),
    KELP_PLANT(
        SingleBlockDropReplacement(),
        BlockSettings.PLACEABLE_IN_ENTITIES,
        { id, visualMaterial, worldMaterial, properties, blockMechanics, connectsTo, _ ->
            BlockData.createKelpPlant(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                blockMechanics,
                connectsTo
            )
        }),
    SERVER_SIDE_BLOCK(
        SingleBlockDropReplacement(),
        BlockSettings.EMPTY,
        { id, visualMaterial, worldMaterial, properties, blockMechanics, _, _ ->
            BlockData.createServerSideBlock(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                blockMechanics,
            )
        })
}

class BlockData(
    val id: String,
    val visualMaterial: Material,
    val worldMaterial: Material,
    val blockType: BlockType,
    val properties: Map<Property<*>, Any>,
    val blockMechanics: BlockMechanics,
    private val listeners: Map<Class<*>, BlockListener<*>>,
    private val connectsTo: Set<String> = setOf(),
    private var overrideBlockId: Int? = null,
    private var packetState: WrappedBlockState = DEFAULT_STATE_CREATOR.create(properties, overrideBlockId, visualMaterial),
    private val propertyApplier: PropertyApplier = DEFAULT_PROPERTY_APPLIER
) {

    interface PropertyApplier {

        fun apply(blockData: BlockData, originalState: WrappedBlockState): WrappedBlockState

    }

    interface StateCreator {

        fun create(properties: Map<Property<*>, Any>, overrideBlockId: Int?, visualMaterial: Material): WrappedBlockState

    }

    companion object {

        val DEFAULT_PROPERTY_APPLIER = object : PropertyApplier {

            override fun apply(blockData: BlockData, originalState: WrappedBlockState): WrappedBlockState {
                if (originalState.type != blockData.packetState.type) {
                    return blockData.packetState.clone()
                }
                if (blockData.properties.isEmpty()) {
                    return blockData.packetState
                }
                val blockOverrideId = blockData.overrideBlockId
                for (entry in blockData.properties) {
                    val property: Property<Any> = entry.key as Property<Any>
                    val value = blockOverrideId?.let {
                        property.getFromState(blockData.packetState)
                    } ?: entry.value
                    property.applyToState(originalState, value)
                }
                return originalState
            }

        }

        private val CAVE_VINES_PLANT_PROPERTY_APPLIER = object : PropertyApplier {

            override fun apply(blockData: BlockData, originalState: WrappedBlockState): WrappedBlockState {
                if (originalState.type != blockData.packetState.type) {
                    val cloned = blockData.packetState.clone()
                    if (originalState.type == StateTypes.CAVE_VINES || originalState.type == StateTypes.CAVE_VINES_PLANT) {
                        cloned.isBerries = originalState.isBerries
                    }
                    return cloned
                }
                for (entry in blockData.properties) {
                    val property: Property<Any> = entry.key as Property<Any>
                    val value = entry.value
                    property.applyToState(originalState, value)
                }
                return originalState
            }

        }

        private val CAVE_VINES_PROPERTY_APPLIER = object : PropertyApplier {

            override fun apply(blockData: BlockData, originalState: WrappedBlockState): WrappedBlockState {
                    if (originalState.type != blockData.packetState.type) {
                        val cloned = blockData.packetState.clone()
                        if (originalState.type == StateTypes.CAVE_VINES || originalState.type == StateTypes.CAVE_VINES_PLANT) {
                            cloned.isBerries = originalState.isBerries
                        }
                        return cloned
                    }
                    for (entry in blockData.properties) {
                        val property: Property<Any> = entry.key as Property<Any>
                        val value = entry.value
                        property.applyToState(originalState, value)
                    }
                    return originalState
            }
        }

        val DEFAULT_LOGS_PROPERTY_APPLIER = object : PropertyApplier {

            override fun apply(blockData: BlockData, originalState: WrappedBlockState): WrappedBlockState {
                return originalState
            }

        }


        private val DEFAULT_STATE_CREATOR = object : StateCreator {

            override fun create(properties: Map<Property<*>, Any>, overrideBlockId: Int?, visualMaterial: Material): WrappedBlockState {
                return if (overrideBlockId == null) {
                    val state = WrappedBlockState.getDefaultState(
                        PacketEvents.getAPI().serverManager.version.toClientVersion(),
                        SpigotConversionUtil.fromBukkitBlockData(visualMaterial.createBlockData()).type
                    )
                    for (entry in properties) {
                        val property: Property<Any> = entry.key as Property<Any>
                        val value = entry.value
                        property.applyToState(state, value)
                    }
                    state
                } else {
                    WrappedBlockState.getByGlobalId(overrideBlockId)
                }
            }

        }


        fun createLeaves(
            id: String,
            visualMaterial: Material,
            properties: Map<Property<*>, Any>,
            blockMechanics: BlockMechanics
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                visualMaterial,
                BlockType.LEAVES,
                properties,
                blockMechanics,
                hashMapOf(
                    BlockPlaceEvent::class.java to LeavesPlaceListener,
                    LeavesDecayEvent::class.java to LeavesDecayListener,
                    BlockPistonExtendEvent::class.java to LeavesPistonExtendListener,
                    BlockPistonRetractEvent::class.java to LeavesPistonRetractListener,
                ),
            )
        }

        fun createLog(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, Any>,
            blockMechanics: BlockMechanics,
            propertyApplier: PropertyApplier
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.LOG,
                properties,
                blockMechanics,
                hashMapOf(
                    BlockPlaceEvent::class.java to LogPlaceListener,
                    PlayerInteractEvent::class.java to LogStripListener
                ),
                propertyApplier = propertyApplier
            )
        }

        fun createStrippedLog(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, Any>,
            blockMechanics: BlockMechanics,
            propertyApplier: PropertyApplier
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.STRIPPED_LOG,
                properties,
                blockMechanics,
                hashMapOf(
                    BlockPlaceEvent::class.java to LogPlaceListener
                ),
                propertyApplier = propertyApplier
            )
        }


        fun createSugarcane(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, Any>,
            blockMechanics: BlockMechanics
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.SUGAR_CANE,
                properties,
                blockMechanics,
                Collections.unmodifiableMap(
                    hashMapOf(
                        BlockPlaceEvent::class.java to SugarCanePlaceListener,
                        BlockGrowEvent::class.java to SugarCaneGrowListener,
                        BlockBreakEvent::class.java to ConnectedBlockFacingUpBlockBreakListener,
                        BlockPistonExtendEvent::class.java to ConnectedBlockFacingUpPistonExtendBreakListener,
                        BlockPistonRetractEvent::class.java to ConnectedBlockFacingUpPistonRetractBreakListener,
                        BlockExplodeEvent::class.java to ConnectedBlockFacingUpBlockExplodeBreakListener,
                        EntityExplodeEvent::class.java to ConnectedBlockFacingUpEntityExplodeBreakListener,
                        RelativeBlockBreakEvent::class.java to PlantFacingUpRelativeBreakListener
                    )
                )
            )
        }

        fun createSapling(
            id: String,
            visualMaterial: Material,
            properties: Map<Property<*>, Any>,
            blockMechanics: BlockMechanics
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                visualMaterial,
                BlockType.SAPLING,
                properties,
                blockMechanics,
                Collections.unmodifiableMap(
                    hashMapOf(
                        BlockPlaceEvent::class.java to SaplingPlaceListener,
                        StructureGrowEvent::class.java to SaplingGrowListener
                    )
                )
            )
        }

        fun createCaveVines(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, Any>,
            blockMechanics: BlockMechanics,
            connectsTo: Set<String>,
            propertyApplier: PropertyApplier = CAVE_VINES_PROPERTY_APPLIER
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.CAVE_VINES,
                properties,
                blockMechanics,
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
                connectsTo,
                propertyApplier = propertyApplier
            )
        }

        fun createCaveVinesPlant(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, Any>,
            blockMechanics: BlockMechanics,
            connectsTo: Set<String>,
            propertyApplier: PropertyApplier = CAVE_VINES_PLANT_PROPERTY_APPLIER
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.CAVE_VINES_PLANT,
                properties,
                blockMechanics,
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
                connectsTo = connectsTo,
                propertyApplier = propertyApplier
            )
        }

        fun createWeepingVines(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, Any>,
            blockMechanics: BlockMechanics,
            connectsTo: Set<String>
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.WEEPING_VINES,
                properties,
                blockMechanics,
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
                connectsTo = connectsTo
            )
        }

        fun createWeepingVinesPlant(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, Any>,
            blockMechanics: BlockMechanics,
            connectsTo: Set<String>,
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.WEEPING_VINES_PLANT,
                properties,
                blockMechanics,
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
                connectsTo = connectsTo
            )
        }

        fun createTwistingVines(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, Any>,
            blockMechanics: BlockMechanics,
            connectsTo: Set<String>
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.TWISTING_VINES,
                properties,
                blockMechanics,
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
                connectsTo = connectsTo
            )
        }

        fun createTwistingVinesPlant(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, Any>,
            blockMechanics: BlockMechanics,
            connectsTo: Set<String>
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.TWISTING_VINES_PLANT,
                properties,
                blockMechanics,
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
                connectsTo = connectsTo
            )
        }

        fun createKelp(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, Any>,
            blockMechanics: BlockMechanics,
            connectsTo: Set<String>
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.KELP,
                properties,
                blockMechanics,
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
                connectsTo = connectsTo
            )
        }

        fun createKelpPlant(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, Any>,
            blockMechanics: BlockMechanics,
            connectsTo: Set<String>
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.KELP_PLANT,
                properties,
                blockMechanics,
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
                connectsTo = connectsTo
            )
        }

        fun createServerSideBlock(
            id: String,
            visualMaterial: Material,
            worldMaterial: Material,
            properties: Map<Property<*>, Any>,
            blockMechanics: BlockMechanics
        ): BlockData {
            return BlockData(
                id,
                visualMaterial,
                worldMaterial,
                BlockType.SERVER_SIDE_BLOCK,
                properties,
                blockMechanics,
                mapOf()
            )
        }
    }

    fun setOverrideBlockId(id: Int?) {
        this.overrideBlockId = id
        if (id == null) return
        this.packetState = WrappedBlockState.getByGlobalId(id)
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
        return this.blockMechanics.itemSupplier.createItem()
    }

    fun replaceItemDrops(config: LeavesConfig, items: MutableList<Item>) {
        this.blockMechanics.blockDrops.replaceItems(config, this, items)
    }

    fun replaceItemStackDrops(config: LeavesConfig, items: MutableList<ItemStack>) {
        this.blockMechanics.blockDrops.replaceItemStacks(config, this, items)
    }

    fun getDrops(config: LeavesConfig): List<ItemStack> {
        return this.blockMechanics.blockDrops.getDrops(config, this)
    }

    fun applyPropertiesToState(originalState: WrappedBlockState): WrappedBlockState {
        return this.propertyApplier.apply(this, originalState)
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