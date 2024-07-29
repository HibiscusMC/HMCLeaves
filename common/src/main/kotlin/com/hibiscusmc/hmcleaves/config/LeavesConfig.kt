package com.hibiscusmc.hmcleaves.config

import com.github.retrooper.packetevents.protocol.sound.SoundCategory
import com.github.retrooper.packetevents.protocol.world.states.enums.Instrument
import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.block.BlockDirection
import com.hibiscusmc.hmcleaves.block.BlockFamily
import com.hibiscusmc.hmcleaves.block.BlockIdPlaceCondition
import com.hibiscusmc.hmcleaves.block.BlockMechanics
import com.hibiscusmc.hmcleaves.block.BlockSetting
import com.hibiscusmc.hmcleaves.block.BlockSettings
import com.hibiscusmc.hmcleaves.block.BlockSoundData
import com.hibiscusmc.hmcleaves.block.BlockType
import com.hibiscusmc.hmcleaves.block.MaterialPlaceCondition
import com.hibiscusmc.hmcleaves.block.PlaceCondition
import com.hibiscusmc.hmcleaves.block.PlaceConditions
import com.hibiscusmc.hmcleaves.block.Property
import com.hibiscusmc.hmcleaves.block.SaplingData
import com.hibiscusmc.hmcleaves.block.SoundData
import com.hibiscusmc.hmcleaves.block.TagPlaceCondition
import com.hibiscusmc.hmcleaves.database.DatabaseSettings
import com.hibiscusmc.hmcleaves.database.DatabaseType
import com.hibiscusmc.hmcleaves.hook.Hooks
import com.hibiscusmc.hmcleaves.item.BlockDropType
import com.hibiscusmc.hmcleaves.item.BlockDrops
import com.hibiscusmc.hmcleaves.item.ConstantItemSupplier
import com.hibiscusmc.hmcleaves.item.EmptyItemSupplier
import com.hibiscusmc.hmcleaves.item.HookItemSupplier
import com.hibiscusmc.hmcleaves.item.IDItemSupplier
import com.hibiscusmc.hmcleaves.item.ItemSupplier
import com.hibiscusmc.hmcleaves.item.MappedBlockDropReplacements
import com.hibiscusmc.hmcleaves.item.SingleBlockDropReplacement
import com.hibiscusmc.hmcleaves.packet.mining.BlockBreakManager
import com.hibiscusmc.hmcleaves.packet.mining.BlockBreakModifier
import com.hibiscusmc.hmcleaves.packet.mining.ToolType
import com.hibiscusmc.hmcleaves.pdc.PDCUtil
import com.hibiscusmc.hmcleaves.texture.TextureData
import com.hibiscusmc.hmcleaves.texture.TextureFileGenerator
import com.hibiscusmc.hmcleaves.util.parseAsAdventure
import org.bukkit.Axis
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.Collections
import java.util.EnumMap
import java.util.Locale
import kotlin.properties.Delegates


private val LEAVES_MATERIALS = Material.entries.filter { Tag.LEAVES.isTagged(it) }.toList()
private val LOG_MATERIALS = Material.entries.filter { Tag.LOGS.isTagged(it) && !it.name.contains("STRIPPED") }.toList()
private val STRIPPED_LOG_MATERIALS =
    Material.entries.filter { Tag.LOGS.isTagged(it) && it.name.contains("STRIPPED") }.toList()
private val SAPLING_MATERIALS = Material.entries.filter { Tag.SAPLINGS.isTagged(it) }.toList()

private const val DATABASE_SETTINGS_KEY = "database-settings"
private const val DATABASE_TYPE_KEY = "type"
private const val DATABASE_PORT_KEY = "port"
private const val DATABASE_KEY = "database"
private const val DATABASE_USER_KEY = "user"
private const val DATABASE_PASSWORD_KEY = "password"
private const val DATABASE_FILE_KEY = "file"

private const val CHUNK_VERSION_KEY = "chunk-version-key"

private const val USE_CUSTOM_MINING_SPEED_FOR_DEFAULT_LOGS = "custom-mining-speed-for-default-logs"
private const val BLOCKS_KEY = "blocks"
private const val TYPE_KEY = "type"
private const val VISUAL_MATERIAL_KEY = "visual-material"
private const val WORLD_MATERIAL_KEY = "world-material"
private const val PROPERTIES_KEY = "properties"
private const val ITEM_KEY = "item"
private const val ITEM_ID_KEY = "item-id"
private const val MATERIAL_KEY = "material"
private const val MODEL_DATA_KEY = "model-data"
private const val NAME_KEY = "name"
private const val LORE_KEY = "lore"
private const val DROPS_KEY = "drops"
private const val DROPS_TYPE_KEY = "type"
private const val CONNECTS_TO_KEY = "connects-to"
private const val BLOCK_BREAK_MODIFIER_KEY = "block-break-modifier"
private const val BLOCK_HARDNESS_KEY = "hardness"
private const val REQUIRES_TOOL_KEY = "requires-tool"
private const val TOOL_TYPES_KEY = "tool-types"
private const val REQUIRED_ENCHANTMENTS_KEY = "required-enchantments"
private const val BLOCK_SETTINGS_KEY = "settings"
private const val BLOCK_FAMILY_KEY = "block-family"
private const val SAPLING_SCHEMATICS_KEY = "schematic"
private const val SAPLING_SCHEMATIC_FILES_KEY = "files"
private const val SAPLING_SCHEMATIC_RANDOM_ROTATION_KEY = "random-rotation"

private const val PLACE_CONDITIONS_KEY = "place-conditions"
private const val PLACE_CONDITION_TAGS_KEY = "tags"
private const val PLACE_CONDITION_MATERIALS_KEY = "materials"
private const val PLACE_CONDITION_IDS_KEY = "ids"
private const val PLACE_CONDITION_ID = "id"

private const val WHITELISTED_WORLDS_KEY = "whitelisted-worlds"
private const val USE_WORLD_WHITELIST_KEY = "use-world-whitelist"

private const val STEP_SOUND_KEY = "step-sound"
private const val HIT_SOUND_KEY = "hit-sound"
private const val PLACE_SOUND_KEY = "place-sound"
private const val BREAK_SOUND_KEY = "break-sound"

private const val SOUND_NAME_KEY = "name"
private const val SOUND_CATEGORY_KEY = "category"
private const val SOUND_VOLUME_KEY = "volume"
private const val SOUND_PITCH_KEY = "pitch"

private const val TEXTURE_PACK_KEY = "texture-pack"
private const val MODEL_PATH_KEY = "model-path"
private const val TEXTURE_PROPERTIES_KEY = "properties"
private const val PARENT_MODEL_KEY = "parent"
private const val TEXTURES_KEY = "textures"

private const val GENERATE_DIRECTIONS_KEY = "generate-directions"

private const val SEND_DEBUG_MESSAGES_KEY = "debug"

private const val DEBUG_STICK_ID = "debug_stick"

private const val USE_TEXTURE_HOOK_KEY = "use-texture-hook"

private const val CURRENT_CHUNK_VERSION = 1

private val INSTRUMENTS = Instrument.entries.toList()

private const val MAX_NOTE = 24;

class LeavesConfig(
    private val plugin: HMCLeaves,
    private val textureFileGenerator: TextureFileGenerator = TextureFileGenerator(plugin)
) {

    private val filePath = plugin.dataFolder.toPath().resolve("config.yml")
    private val doNotTouchPath = plugin.dataFolder.toPath().resolve("do-not-touch.yml")
    private var chunkVersion = CURRENT_CHUNK_VERSION

    private var databaseSettings: DatabaseSettings? = null
    private var sendDebugMessages = false
    private var customMiningSpeedsForDefaultLogs by Delegates.notNull<Boolean>()
    private val whitelistedWorlds = hashSetOf<String>()
    private var useWorldWhitelist = true

    private var useTextureHook = false

    private val defaultBlockData: MutableMap<Material, BlockData> = EnumMap(org.bukkit.Material::class.java)
    private val blockData: MutableMap<String, BlockData> = hashMapOf()
    private val hookIdToBlockDataId: MutableMap<String, String> = hashMapOf()
    private val saplingData: MutableMap<String, SaplingData> = hashMapOf()

    private var instrumentIndex = 0
    private var noteIndex = 0

    fun getDatabaseSettings(): DatabaseSettings {
        return this.databaseSettings ?: throw IllegalStateException("Database settings were not initialized")
    }

    fun sendDebugMessages(): Boolean {
        return this.sendDebugMessages
    }

    fun getDebugStick(): ItemStack {
        val item = ItemStack(Material.STICK)
        val meta = item.itemMeta ?: return item
        meta.setDisplayName("<red>Debug Stick".parseAsAdventure())
        item.itemMeta = meta
        PDCUtil.setItemId(item, DEBUG_STICK_ID)
        return item
    }

    fun isDebugStick(item: ItemStack): Boolean {
        return DEBUG_STICK_ID == PDCUtil.getItemId(item)
    }

    fun getDefaultBlockData(material: Material): BlockData? {
        return this.defaultBlockData[material]
    }

    fun getBlockData(id: String): BlockData? {
        return this.blockData[id]
    }

    fun getSaplingData(id: String): SaplingData? {
        return this.saplingData[id]
    }

    fun getBlockDataIds(): Collection<String> {
        return this.blockData.keys
    }

    fun getAllBlockData(): Map<String, BlockData> {
        return Collections.unmodifiableMap(this.blockData)
    }

    fun getBlockDataFromItem(item: ItemStack): BlockData? {
        val hookId = Hooks.getIdByItemStack(item)
        val id = this.hookIdToBlockDataId[hookId] ?: PDCUtil.getItemId(item) ?: return getDefaultBlockData(item.type)
        return this.blockData[id]
    }

    fun getHookIdsToBlockIds(): Map<String, String> {
        return Collections.unmodifiableMap(this.hookIdToBlockDataId)
    }

    fun getBlockDataIdFromHookId(hookId: String): String? {
        return this.hookIdToBlockDataId[hookId]
    }

    fun load() {
        val file = filePath.toFile()
        if (!file.exists()) {
            file.parentFile.mkdirs()
            this.plugin.saveDefaultConfig()
        }
        val config = YamlConfiguration.loadConfiguration(file)

        this.sendDebugMessages = config.getBoolean(SEND_DEBUG_MESSAGES_KEY, false)
        this.customMiningSpeedsForDefaultLogs = config.getBoolean(USE_CUSTOM_MINING_SPEED_FOR_DEFAULT_LOGS, false)
        this.useWorldWhitelist = config.getBoolean(USE_WORLD_WHITELIST_KEY, this.useWorldWhitelist)
        this.whitelistedWorlds.addAll(config.getStringList(WHITELISTED_WORLDS_KEY))
        this.useTextureHook = config.getBoolean(USE_TEXTURE_HOOK_KEY, false)

        this.loadDatabase(config)
        this.loadDefaults()
        this.loadBlocks(config)
    }

    fun reload() {
        this.databaseSettings = null
        this.sendDebugMessages = false
        this.customMiningSpeedsForDefaultLogs = false
        this.useWorldWhitelist = true
        this.useTextureHook = false
        this.instrumentIndex = 0
        this.noteIndex = 0

        this.whitelistedWorlds.clear()
        this.defaultBlockData.clear()
        this.blockData.clear()
        this.hookIdToBlockDataId.clear()
        this.load()
        Hooks.reload()
    }

    private fun loadDatabase(config: FileConfiguration) {
        this.databaseSettings = this.loadDatabaseSettings(config)

        val file = doNotTouchPath.toFile()
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        val doNotTouchConfig = YamlConfiguration.loadConfiguration(file)
        val currentVersion = doNotTouchConfig.getInt(CHUNK_VERSION_KEY)
        if (currentVersion >= CURRENT_CHUNK_VERSION) {
            this.chunkVersion = currentVersion
            return
        }
        doNotTouchConfig.set(CHUNK_VERSION_KEY, CURRENT_CHUNK_VERSION)
        doNotTouchConfig.save(file)
    }

    fun getChunkVersion(): Int {
        return this.chunkVersion
    }

    fun isWorldWhitelisted(world: World): Boolean {
        return !this.useWorldWhitelist || this.whitelistedWorlds.contains(world.name)
    }

    fun isWorldWhitelisted(worldName: String): Boolean {
        return !this.useWorldWhitelist || this.whitelistedWorlds.contains(worldName)
    }

    fun addWhitelistedWorld(worldName: String) {
        this.whitelistedWorlds.add(worldName)
    }

    fun removeWhitelistedWorld(worldName: String) {
        this.whitelistedWorlds.remove(worldName)
    }

    fun useTextureHook() = this.useTextureHook

    fun isWorldWhitelistEnabled(): Boolean = this.useWorldWhitelist

    private fun getDefaultIdFromMaterial(material: Material): String {
        return "default_${material.toString().lowercase()}"
    }

    private fun getAxisType(blockData: BlockData): BlockFamily.Type? {
        for (type in BlockFamily.Type.AXIS_TYPES) {
            val axisId = blockData.blockMechanics.blockFamily.getFamilyId(type)
            if (axisId != blockData.id) continue
            return type
        }
        return null
    }

    fun getDefaultDirectionalBlockData(material: Material, axis: Axis): BlockData? {
        val blockData = this.getDefaultBlockData(material) ?: return null
        return this.getDirectionalBlockData(blockData, axis)
    }

    fun getDirectionalBlockData(blockData: BlockData, axis: Axis): BlockData? {
        val directionalId =
            blockData.blockMechanics.blockFamily.getFamilyId(BlockFamily.Type.fromAxis(axis)) ?: return null
        return this.getBlockData(directionalId)
    }

    fun getNonDirectionalBlockData(blockData: BlockData): BlockData? {
        val nonDirectionalId = blockData.blockMechanics.blockFamily.getFamilyId(BlockFamily.Type.NO_AXIS) ?: return null
        return this.getBlockData(nonDirectionalId)
    }

    fun getStrippedBlockData(blockData: BlockData): BlockData? {
        val strippedId = blockData.blockMechanics.blockFamily.getFamilyId(BlockFamily.Type.STRIPPED) ?: return null
        val axis = getAxisType(blockData) ?: return this.getBlockData(strippedId)
        val strippedData = this.getBlockData(strippedId) ?: return null
        val strippedDirectionalId = strippedData.blockMechanics.blockFamily.getFamilyId(axis) ?: return null
        return this.getBlockData(strippedDirectionalId)
    }

    fun getPlant(blockData: BlockData): BlockData? {
        val plantId = blockData.blockMechanics.blockFamily.getFamilyId(BlockFamily.Type.PLANT) ?: return null
        return this.getBlockData(plantId)
    }

    fun getNonPlant(blockData: BlockData): BlockData? {
        val nonPlantId = blockData.blockMechanics.blockFamily.getFamilyId(BlockFamily.Type.NOT_PLANT) ?: return null
        return this.getBlockData(nonPlantId)
    }

    private fun loadDatabaseSettings(config: FileConfiguration): DatabaseSettings {
        val section = config.getConfigurationSection(DATABASE_SETTINGS_KEY)
            ?: throw IllegalArgumentException("$DATABASE_SETTINGS_KEY section is required")
        val type = section.getString(DATABASE_TYPE_KEY)?.let { DatabaseType.valueOf(it.uppercase()) }
            ?: throw IllegalArgumentException("$DATABASE_SETTINGS_KEY requires $DATABASE_TYPE_KEY")
        return when (type) {
            DatabaseType.SQLITE -> loadSqliteDatabase(section)
            else -> throw NotImplementedError("$type is not an implemented database type")
        }
    }

    private fun loadSqliteDatabase(databaseSection: ConfigurationSection): DatabaseSettings {
        val path = this.plugin.dataFolder.toPath().resolve("database").resolve("leaves.db");
        return DatabaseSettings(
            path,
            DatabaseType.SQLITE,
            "",
            "",
            ""
        )
    }

    private fun loadDefaults() {
        this.loadDefaultLeaves()
        this.loadDefaultLogs()
        this.loadDefaultStrippedLogs()
        this.loadDefaultSugarcane()
        this.loadDefaultSaplings()
        this.loadDefaultCaveVines()
        this.loadDefaultCaveVinesPlant()
        this.loadDefaultWeepingVines()
        this.loadDefaultWeepingVinesPlant()
        this.loadDefaultTwistingVines()
        this.loadDefaultTwistingVinesPlant()
        this.loadDefaultKelp()
        this.loadDefaultKelpPlant()
    }

    private fun loadDefaultLeaves() {
        val properties: Map<Property<*>, Any> = hashMapOf(
            Property.DISTANCE to 7,
            Property.PERSISTENT to false
        )

        for (material in LEAVES_MATERIALS) {
            val id = getDefaultIdFromMaterial(material)
            val data = BlockData.createLeaves(
                id,
                material,
                properties,
                BlockMechanics(
                    BlockFamily(),
                    BlockSoundData.EMPTY,
                    ConstantItemSupplier(ItemStack(material), id),
                    SingleBlockDropReplacement(),
                    BlockType.LEAVES.defaultSettings,
                    emptyList(),
                )
            )
            this.defaultBlockData[material] = data
            this.blockData[id] = data
        }
    }

    private fun loadDefaultLogs() {
        val properties: Map<Property<*>, Any> = hashMapOf()

        for (material in LOG_MATERIALS) {
            val id = getDefaultIdFromMaterial(material)
            val data = BlockData.createLog(
                id,
                material,
                material,
                properties,
                BlockMechanics(
                    BlockFamily(Pair(BlockFamily.Type.STRIPPED, "stripped_${id}")),
                    BlockSoundData.EMPTY,
                    ConstantItemSupplier(ItemStack(material), id),
                    SingleBlockDropReplacement(),
                    BlockType.LOG.defaultSettings,
                    emptyList(),
                    null,
                    if (this.customMiningSpeedsForDefaultLogs) BlockBreakManager.LOG_BREAK_MODIFIER else null,
                ),
                BlockData.DEFAULT_LOGS_PROPERTY_APPLIER
            )
            this.defaultBlockData[material] = data
            this.blockData[id] = data
        }
    }

    private fun loadDefaultStrippedLogs() {
        val properties: Map<Property<*>, Any> = hashMapOf()

        for (material in STRIPPED_LOG_MATERIALS) {
            val notStrippedId = getDefaultIdFromMaterial(material).replace("_stripped", "")
            val id = "stripped_${notStrippedId}"
            val data = BlockData.createLog(
                id,
                material,
                material,
                properties,
                BlockMechanics(
                    BlockFamily(Pair(BlockFamily.Type.NOT_STRIPPED, notStrippedId)),
                    BlockSoundData.EMPTY,
                    ConstantItemSupplier(ItemStack(material), id),
                    SingleBlockDropReplacement(),
                    BlockType.LOG.defaultSettings,
                    emptyList(),
                    null,
                    if (this.customMiningSpeedsForDefaultLogs) BlockBreakManager.LOG_BREAK_MODIFIER else null
                ),
                BlockData.DEFAULT_LOGS_PROPERTY_APPLIER
            )
            this.defaultBlockData[material] = data
            this.blockData[id] = data
        }
    }

    private fun loadDefaultSugarcane() {
        val properties: Map<Property<*>, Any> = hashMapOf(
            Property.AGE to 0
        )
        val material = Material.SUGAR_CANE
        val id = getDefaultIdFromMaterial(material)
        val data = BlockData.createSugarcane(
            id,
            material,
            material,
            properties,
            BlockMechanics(
                BlockFamily(),
                BlockSoundData.EMPTY,
                ConstantItemSupplier(ItemStack(material), id),
                SingleBlockDropReplacement(),
                BlockType.SUGAR_CANE.defaultSettings,
                emptyList()
            )
        )
        this.defaultBlockData[material] = data
        this.blockData[id] = data
    }

    private fun loadDefaultSaplings() {
        val properties: Map<Property<*>, Any> = hashMapOf()

        for (material in SAPLING_MATERIALS) {
            val id = getDefaultIdFromMaterial(material)
            val data = BlockData.createSapling(
                id,
                material,
                properties,
                BlockMechanics(
                    BlockFamily(),
                    BlockSoundData.EMPTY,
                    ConstantItemSupplier(ItemStack(material), id),
                    SingleBlockDropReplacement(),
                    BlockType.SAPLING.defaultSettings,
                    emptyList()
                )
            )
            this.defaultBlockData[material] = data
            this.blockData[id] = data
        }
    }

    private fun loadDefaultCaveVines() {
        val properties: Map<Property<*>, Any> = hashMapOf(
            Property.AGE to 0
        )
        val material = Material.CAVE_VINES
        val id = getDefaultIdFromMaterial(material)
        val data = BlockData.createCaveVines(
            id,
            material,
            material,
            properties,
            BlockMechanics(
                BlockFamily(Pair(BlockFamily.Type.PLANT, getDefaultIdFromMaterial(Material.CAVE_VINES_PLANT))),
                BlockSoundData.EMPTY,
                ConstantItemSupplier(ItemStack(Material.GLOW_BERRIES), id),
                SingleBlockDropReplacement(),
                BlockType.CAVE_VINES.defaultSettings,
                emptyList()
            ),
            setOf(getDefaultIdFromMaterial(Material.CAVE_VINES_PLANT)),
        )
        this.defaultBlockData[material] = data
        this.defaultBlockData[Material.GLOW_BERRIES] = data
        this.blockData[id] = data
    }

    private fun loadDefaultCaveVinesPlant() {
        val properties: Map<Property<*>, Any> = hashMapOf()
        val material = Material.CAVE_VINES_PLANT
        val id = getDefaultIdFromMaterial(material)
        val data = BlockData.createCaveVinesPlant(
            id,
            material,
            material,
            properties,
            BlockMechanics(
                BlockFamily(Pair(BlockFamily.Type.NOT_PLANT, getDefaultIdFromMaterial(Material.CAVE_VINES))),
                BlockSoundData.EMPTY,
                ConstantItemSupplier(ItemStack(Material.GLOW_BERRIES), id),
                SingleBlockDropReplacement(),
                BlockType.CAVE_VINES_PLANT.defaultSettings,
                emptyList(),
            ),
            setOf(getDefaultIdFromMaterial(Material.CAVE_VINES)),
        )
        this.defaultBlockData[material] = data
        this.blockData[id] = data
    }

    private fun loadDefaultWeepingVines() {
        val properties: Map<Property<*>, Any> = hashMapOf(
            Property.AGE to 0
        )
        val material = Material.WEEPING_VINES
        val id = getDefaultIdFromMaterial(material)
        val data = BlockData.createWeepingVines(
            id,
            material,
            material,
            properties,
            BlockMechanics(
                BlockFamily(Pair(BlockFamily.Type.PLANT, getDefaultIdFromMaterial(Material.WEEPING_VINES_PLANT))),
                BlockSoundData.EMPTY,
                ConstantItemSupplier(ItemStack(material), id),
                SingleBlockDropReplacement(),
                BlockType.WEEPING_VINES.defaultSettings,
                emptyList()
            ),
            setOf(getDefaultIdFromMaterial(Material.WEEPING_VINES_PLANT)),
        )
        this.defaultBlockData[material] = data
        this.blockData[id] = data
    }

    private fun loadDefaultWeepingVinesPlant() {
        val properties: Map<Property<*>, Any> = hashMapOf()
        val material = Material.WEEPING_VINES_PLANT
        val id = getDefaultIdFromMaterial(material)
        val data = BlockData.createWeepingVinesPlant(
            id,
            material,
            material,
            properties,
            BlockMechanics(
                BlockFamily(Pair(BlockFamily.Type.NOT_PLANT, getDefaultIdFromMaterial(Material.WEEPING_VINES))),
                BlockSoundData.EMPTY,
                ConstantItemSupplier(ItemStack(Material.WEEPING_VINES), id),
                SingleBlockDropReplacement(),
                BlockType.WEEPING_VINES_PLANT.defaultSettings,
                emptyList()
            ),
            setOf(getDefaultIdFromMaterial(Material.WEEPING_VINES)),
        )
        this.defaultBlockData[material] = data
        this.blockData[id] = data
    }

    private fun loadDefaultTwistingVines() {
        val properties: Map<Property<*>, Any> = hashMapOf(
            Property.AGE to 0
        )
        val material = Material.TWISTING_VINES
        val id = getDefaultIdFromMaterial(material)
        val data = BlockData.createTwistingVines(
            id,
            material,
            material,
            properties,
            BlockMechanics(
                BlockFamily(Pair(BlockFamily.Type.PLANT, getDefaultIdFromMaterial(Material.TWISTING_VINES_PLANT))),
                BlockSoundData.EMPTY,
                ConstantItemSupplier(ItemStack(material), id),
                SingleBlockDropReplacement(),
                BlockType.TWISTING_VINES.defaultSettings,
                emptyList()
            ),
            setOf(getDefaultIdFromMaterial(Material.TWISTING_VINES_PLANT)),
        )
        this.defaultBlockData[material] = data
        this.blockData[id] = data
    }

    private fun loadDefaultTwistingVinesPlant() {
        val properties: Map<Property<*>, Any> = hashMapOf()
        val material = Material.TWISTING_VINES_PLANT
        val id = getDefaultIdFromMaterial(material)
        val data = BlockData.createTwistingVinesPlant(
            id,
            material,
            material,
            properties,
            BlockMechanics(
                BlockFamily(Pair(BlockFamily.Type.NOT_PLANT, getDefaultIdFromMaterial(Material.TWISTING_VINES))),
                BlockSoundData.EMPTY,
                ConstantItemSupplier(ItemStack(Material.TWISTING_VINES), id),
                SingleBlockDropReplacement(),
                BlockType.TWISTING_VINES_PLANT.defaultSettings,
                emptyList()
            ),
            setOf(getDefaultIdFromMaterial(Material.TWISTING_VINES)),
        )
        this.defaultBlockData[material] = data
        this.blockData[id] = data
    }

    private fun loadDefaultKelp() {
        val properties: Map<Property<*>, Any> = hashMapOf(
            Property.AGE to 0
        )
        val material = Material.KELP
        val id = getDefaultIdFromMaterial(material)
        val data = BlockData.createKelp(
            id,
            material,
            material,
            properties,
            BlockMechanics(
                BlockFamily(Pair(BlockFamily.Type.PLANT, getDefaultIdFromMaterial(Material.KELP_PLANT))),
                BlockSoundData.EMPTY,
                ConstantItemSupplier(ItemStack(material), id),
                SingleBlockDropReplacement(),
                BlockSettings.PLACEABLE_IN_ENTITIES,
                emptyList()
            ),
            setOf(getDefaultIdFromMaterial(Material.KELP_PLANT)),
        )
        this.defaultBlockData[material] = data
        this.blockData[id] = data
    }

    private fun loadDefaultKelpPlant() {
        val properties: Map<Property<*>, Any> = hashMapOf()
        val material = Material.KELP_PLANT
        val id = getDefaultIdFromMaterial(material)
        val data = BlockData.createKelpPlant(
            id,
            material,
            material,
            properties,
            BlockMechanics(
                BlockFamily(Pair(BlockFamily.Type.NOT_PLANT, getDefaultIdFromMaterial(Material.KELP))),
                BlockSoundData.EMPTY,
                ConstantItemSupplier(ItemStack(Material.KELP), id),
                SingleBlockDropReplacement(),
                BlockSettings.PLACEABLE_IN_ENTITIES,
                emptyList()
            ),
            setOf(getDefaultIdFromMaterial(Material.KELP)),
        )
        this.defaultBlockData[material] = data
        this.blockData[id] = data
    }

    fun loadTextures(): List<File> {
        val files = mutableListOf<File>()
        files.addAll(this.loadTextures(LEAVES_MATERIALS))
        files.addAll(this.loadTextures(LOG_MATERIALS + STRIPPED_LOG_MATERIALS, Material.NOTE_BLOCK))
        files.addAll(this.loadTextures(SAPLING_MATERIALS))
        files.addAll(
            this.loadTextures(
                listOf(
                    Material.SUGAR_CANE,
                    Material.STRING,
                    Material.NOTE_BLOCK,
                    Material.CAVE_VINES,
                    Material.CAVE_VINES_PLANT,
                    Material.WEEPING_VINES,
                    Material.WEEPING_VINES_PLANT,
                    Material.TWISTING_VINES,
                    Material.TWISTING_VINES_PLANT,
                    Material.KELP,
                    Material.KELP_PLANT,
                )
            )
        )
        return files
    }

    private fun loadTextures(materials: Collection<Material>, overrideMaterial: Material? = null): List<File> {
        val files = mutableListOf<File>()
        if (overrideMaterial != null) {
            files.addAll(this.loadTextures(overrideMaterial, materials))
        }
        for (material in materials) {
            files.addAll(this.loadTextures(material, listOf(material)))
        }
        return files
    }

    private fun loadTextures(material: Material, matchMaterials: Collection<Material>): Collection<File> {
        return this.textureFileGenerator.generateFiles(
            material,
            this.blockData.values
                .filter { blockData -> matchMaterials.contains(blockData.visualMaterial) }
                .filter { blockData ->
                    blockData.blockMechanics.textureData?.modelPath != null
                }
                .toList(),
            this
        )
    }

    private fun loadBlocks(config: YamlConfiguration) {
        val blocksSection = config.getConfigurationSection(BLOCKS_KEY) ?: return
        for (id in blocksSection.getKeys(false)) {
            val section = blocksSection.getConfigurationSection(id) ?: continue
            val type = section.getString(TYPE_KEY)?.let { BlockType.valueOf(it.uppercase()) }
                ?: throw IllegalArgumentException("$id requires $TYPE_KEY")
            val visualMaterial = section.getString(VISUAL_MATERIAL_KEY)?.let { Material.valueOf(it.uppercase()) }
                ?: throw IllegalArgumentException("$id requires $VISUAL_MATERIAL_KEY")
            val worldMaterial = section.getString(WORLD_MATERIAL_KEY)?.let { Material.valueOf(it.uppercase()) }
                ?: throw IllegalArgumentException("$id requires $WORLD_MATERIAL_KEY")
            val properties = this.loadProperties(section, type)
            val blockFamily = this.loadBlockFamily(section)
            val itemSupplier = section.getConfigurationSection(ITEM_KEY)?.let {
                this.loadItem(it, id)
            } ?: ConstantItemSupplier(ItemStack(worldMaterial), id)

            val blockDrops = loadDrops(section.getConfigurationSection(DROPS_KEY), id, type)
            val connectsTo = section.getStringList(CONNECTS_TO_KEY).toSet()
            val blockBreakModifier =
                loadBlockBreakModifier(section.getConfigurationSection(BLOCK_BREAK_MODIFIER_KEY), id)
            val settings = loadBlockSettings(section.getConfigurationSection(BLOCK_SETTINGS_KEY), id, type)
            val placeConditions = loadPlaceConditions(section.getConfigurationSection(PLACE_CONDITIONS_KEY), id)
            val blockSoundData = loadBlockSoundData(section)
            val textureData = loadTextureData(section)
            val data = type.blockSupplier(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                BlockMechanics(
                    blockFamily,
                    blockSoundData,
                    itemSupplier,
                    blockDrops,
                    settings,
                    placeConditions,
                    textureData,
                    blockBreakModifier
                ),
                connectsTo,
                null
            )
            blockData[id] = data

            if (type == BlockType.SAPLING) {
                this.loadSaplingSchematicData(id, type, section)
            }

            if (type == BlockType.LOG || type == BlockType.STRIPPED_LOG) {
                this.loadLogDirectionals(
                    section,
                    id,
                    type,
                    visualMaterial,
                    worldMaterial,
                    properties,
                    blockFamily,
                    blockSoundData,
                    itemSupplier,
                    blockDrops,
                    connectsTo,
                    blockBreakModifier,
                    settings,
                    textureData,
                    placeConditions
                )
            }
        }
    }

    private fun loadSaplingSchematicData(id: String, type: BlockType, section: ConfigurationSection) {
        if (type == BlockType.SAPLING) {
            section.getConfigurationSection(SAPLING_SCHEMATICS_KEY)?.let { schematicsSection ->
                this.saplingData[id] = SaplingData(
                    id,
                    schematicsSection.getStringList(SAPLING_SCHEMATIC_FILES_KEY),
                    schematicsSection.getBoolean(SAPLING_SCHEMATIC_RANDOM_ROTATION_KEY)
                )
            }
        }
    }

    private fun loadLogDirectionals(
        section: ConfigurationSection,
        id: String,
        type: BlockType,
        visualMaterial: Material,
        worldMaterial: Material,
        properties: Map<Property<*>, Any>,
        blockFamily: BlockFamily,
        blockSoundData: BlockSoundData,
        itemSupplier: ItemSupplier,
        blockDrops: BlockDrops,
        connectsTo: Set<String>,
        blockBreakModifier: BlockBreakModifier?,
        settings: BlockSettings,
        textureData: TextureData?,
        placeConditions: List<PlaceConditions>
    ) {
        if (!section.getBoolean(GENERATE_DIRECTIONS_KEY, false)) {
            return
        }
        // an extra note isn't used for no reason
        blockFamily.addFamilyType(BlockFamily.Type.NO_AXIS, id)
        for ((axisNum, axis) in Axis.entries.withIndex()) {
            val newProperties = if (axisNum == 0) properties else this.loadProperties(section, type);
            val newId =
                "${id}_${axis.name.lowercase()}"
            blockFamily.addFamilyType(BlockFamily.Type.fromAxis(axis), newId)
            val newData = type.blockSupplier(
                newId,
                visualMaterial,
                worldMaterial,
                newProperties,
                BlockMechanics(
                    blockFamily,
                    blockSoundData,
                    itemSupplier,
                    blockDrops,
                    settings,
                    placeConditions,
                    textureData?.withProperties(this.texturePropertiesFromAxis(axis)),
                    blockBreakModifier,
                ),
                connectsTo,
                null
            )
            blockData[newId] = newData
        }
    }

    private fun texturePropertiesFromAxis(axis: Axis): Map<String, Pair<Any, TextureData.PropertyWriter>> {
        val ninetyPair = Pair(90, TextureData.PropertyWriter.INT_WRITER)
        return when (axis) {
            Axis.X -> mapOf(Pair("x", ninetyPair), Pair("y", ninetyPair))
            Axis.Y -> mapOf(Pair("y", ninetyPair), Pair("z", ninetyPair))
            Axis.Z -> mapOf(Pair("x", ninetyPair), Pair("z", ninetyPair))
        }
    }

    private fun loadProperties(section: ConfigurationSection, type: BlockType): Map<Property<*>, Any> {
        val properties: MutableMap<Property<*>, Any> = hashMapOf()
        if (type == BlockType.LOG || type == BlockType.STRIPPED_LOG) {
            val note = this.noteIndex
            val instrument = INSTRUMENTS[this.instrumentIndex]
            if (this.noteIndex > MAX_NOTE) {
                this.noteIndex = 0
                this.instrumentIndex++
            } else {
                this.noteIndex++
            }
            properties[Property.INSTRUMENT] = instrument
            properties[Property.NOTE] = note
            return properties
        }
        section.getConfigurationSection(PROPERTIES_KEY)?.let { propertiesSection ->
            for (propertyKey in propertiesSection.getKeys(false)) {
                val property: Property<Any> = Property.getPropertyByKey(propertyKey)
                    ?: throw IllegalArgumentException("Invalid property $propertyKey")
                val value = propertiesSection.getString(propertyKey)?.let { property.converter(it) }
                    ?: throw IllegalArgumentException("Invalid property for $propertyKey")
                properties[property] = value
            }
        }
        return properties
    }

    private fun loadBlockFamily(section: ConfigurationSection): BlockFamily {
        val family = BlockFamily()
        section.getConfigurationSection(BLOCK_FAMILY_KEY)?.let { blockFamilySection ->
            for (key in blockFamilySection.getKeys(false)) {
                try {
                    val type = BlockFamily.Type.valueOf(key.uppercase())
                    val blockId = blockFamilySection.getString(key)!!
                    family.addFamilyType(type, blockId)
                } catch (exception: IllegalArgumentException) {
                    this.plugin.logger.severe("$key is not a valid block family type")
                }
            }
        }
        return family
    }

    private fun loadItem(section: ConfigurationSection, id: String): ItemSupplier {
        try {
            val material = section.getString(MATERIAL_KEY)?.let { Material.valueOf(it.uppercase()) }
                ?: throw IllegalArgumentException("$id item requires $MATERIAL_KEY")
            val name = section.getString(NAME_KEY)?.parseAsAdventure() ?: ""
            val lore = section.getStringList(LORE_KEY).map {
                it.parseAsAdventure()
            }.toMutableList()
            val modelData = section.getInt(MODEL_DATA_KEY, -1)

            val item = ItemStack(material)
            val meta = item.itemMeta ?: throw IllegalStateException("Error creating metadata for $material in item $id")
            meta.setDisplayName(name)
            meta.lore = lore
            if (modelData >= 0) {
                meta.setCustomModelData(modelData)
            }
            item.itemMeta = meta
            val constantSupplier = ConstantItemSupplier(item, id)
            if (!section.contains(ITEM_ID_KEY)) {
                return constantSupplier
            }
            val hookItemId = section.getString(ITEM_ID_KEY)!!
            this.hookIdToBlockDataId[hookItemId] = id
            return HookItemSupplier(id, hookItemId, IDItemSupplier(this, hookItemId, constantSupplier))
        } catch (exception: Exception) {
            if (!section.contains(ITEM_ID_KEY)) {
                throw exception
            }
            val hookItemId = section.getString(ITEM_ID_KEY)!!
            this.hookIdToBlockDataId[hookItemId] = id
            return HookItemSupplier(id, hookItemId, IDItemSupplier(this, hookItemId, EmptyItemSupplier))
        }
    }

    private fun loadDrops(
        section: ConfigurationSection?,
        id: String,
        type: BlockType
    ): BlockDrops {
        if (section == null) {
            return type.defaultBlockDrops
        }
        val dropType = section.getString(DROPS_TYPE_KEY)?.let { BlockDropType.valueOf(it.uppercase()) }
            ?: throw IllegalArgumentException("Invalid drop type: ${section.getString(DROPS_TYPE_KEY)}")
        return when (dropType) {
            BlockDropType.MAPPED_REPLACEMENT -> loadMappedBlockDrops(section, id, type)
        }
    }

    private fun loadMappedBlockDrops(section: ConfigurationSection, id: String, type: BlockType): BlockDrops {
        val replacements: MutableMap<Material, ItemSupplier> = EnumMap(org.bukkit.Material::class.java)
        for (key in section.getKeys(false)) {
            if (key.equals(DROPS_TYPE_KEY)) continue
            val material = Material.matchMaterial(key.uppercase())
            if (material == null) {
                this.plugin.logger.warning("$key is not a valid material for block drops of $id")
                continue
            }
            val itemSection = section.getConfigurationSection(key)
            if (itemSection == null) {
                this.plugin.logger.warning("Item was not able to be loaded for block drops of $id")
                continue
            }
            val itemSupplier = loadItem(itemSection, id)
            replacements[material] = itemSupplier
        }
        return MappedBlockDropReplacements(replacements)
    }

    private fun loadBlockBreakModifier(section: ConfigurationSection?, id: String): BlockBreakModifier? {
        if (section == null) return null
        val hardness = section.getDouble(BLOCK_HARDNESS_KEY, -1.0)
        if (hardness <= 0) throw IllegalArgumentException("$BLOCK_HARDNESS_KEY cannot be $hardness for $id")
        val requiresToolToDrop = section.getBoolean(REQUIRES_TOOL_KEY, false)
        val toolTypes = section.getStringList(TOOL_TYPES_KEY).map { ToolType.valueOf(it.uppercase()) }
            .toSet()
        val enchantments = section.getStringList(REQUIRED_ENCHANTMENTS_KEY).map {
            Enchantment.getByKey(NamespacedKey.minecraft(it))
                ?: throw IllegalArgumentException("$it is not a valid enchantment for $id")
        }.toSet()
        return BlockBreakModifier(hardness, requiresToolToDrop, toolTypes, enchantments)
    }

    private fun loadBlockSettings(section: ConfigurationSection?, id: String, type: BlockType): BlockSettings {
        if (section == null) return type.defaultSettings
        val settings = EnumMap<BlockSetting, Boolean>(BlockSetting::class.java)
        for (key in section.getKeys(false)) {
            val setting = BlockSetting.valueOf(key.replace('-', '_').uppercase())
            settings[setting] = section.getBoolean(key)
        }
        return BlockSettings(settings)
    }

    private fun loadPlaceConditions(section: ConfigurationSection?, id: String): List<PlaceConditions> {
        if (section == null) return emptyList()
        if (section.contains(PLACE_CONDITION_ID)) {
            return PlaceConditions.getById(section.getString(PLACE_CONDITION_ID)!!, id)
        }
        val allConditions = arrayListOf<PlaceConditions>()
        for (key in section.getKeys(false)) {
            val placeConditionsMap = EnumMap<BlockDirection, List<PlaceCondition>>(BlockDirection::class.java)
            val conditionsSection = section.getConfigurationSection(key) ?: continue
            for (directionKey in conditionsSection.getKeys(false)) {
                val direction = BlockDirection.valueOf(directionKey.uppercase())
                val directionSection = conditionsSection.getConfigurationSection(directionKey) ?: continue
                val conditions = this.loadPlaceCondition(directionSection, id)
                placeConditionsMap[direction] = conditions
            }
            allConditions.add(PlaceConditions(placeConditionsMap))
        }
        return allConditions
    }

    private fun loadPlaceCondition(section: ConfigurationSection, id: String): List<PlaceCondition> {
        val materials =
            section.getStringList(PLACE_CONDITION_MATERIALS_KEY).mapNotNull { Material.matchMaterial(it.uppercase()) }
        val tags = section.getStringList(PLACE_CONDITION_TAGS_KEY).mapNotNull {
            Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(it.lowercase()), Material::class.java)
        }
        val ids = section.getStringList(PLACE_CONDITION_IDS_KEY)
        val conditions = arrayListOf<PlaceCondition>()
        if (materials.isNotEmpty()) {
            conditions.add(MaterialPlaceCondition(materials.toHashSet()))
        }
        if (tags.isNotEmpty()) {
            conditions.add(TagPlaceCondition(tags))
        }
        if (ids.isNotEmpty()) {
            conditions.add(BlockIdPlaceCondition(ids.toHashSet()))
        }
        return conditions
    }

    private fun loadBlockSoundData(section: ConfigurationSection?): BlockSoundData {
        if (section == null) return BlockSoundData.EMPTY
        val stepSound = this.loadSoundData(section.getConfigurationSection(STEP_SOUND_KEY))
        val hitSound = this.loadSoundData(section.getConfigurationSection(HIT_SOUND_KEY))
        val placeSound = this.loadSoundData(section.getConfigurationSection(PLACE_SOUND_KEY))
        val breakSound = this.loadSoundData(section.getConfigurationSection(BREAK_SOUND_KEY))
        return BlockSoundData(stepSound, hitSound, placeSound, breakSound)
    }

    private fun loadSoundData(section: ConfigurationSection?): SoundData? {
        if (section == null) return null
        val name = section.getString(SOUND_NAME_KEY) ?: return null
        val category = SoundCategory.valueOf(section.getString(SOUND_CATEGORY_KEY)!!.uppercase(Locale.getDefault()))
        val volume = section.getDouble(SOUND_VOLUME_KEY, 1.0).toFloat()
        val pitch = section.getDouble(SOUND_PITCH_KEY, 1.0).toFloat()
        return SoundData(name, category, volume, pitch)
    }

    private fun loadTextureData(section: ConfigurationSection?): TextureData? {
        val texturePackSection = section?.getConfigurationSection(TEXTURE_PACK_KEY) ?: return null
        val modelPath = texturePackSection.getString(MODEL_PATH_KEY) ?: return null
        val texturesPropertiesSection =
            texturePackSection.getConfigurationSection(TEXTURE_PROPERTIES_KEY)
        val textureProperties = hashMapOf<String, Pair<Any, TextureData.PropertyWriter>>()
        if (texturesPropertiesSection != null) {
            for (key in texturesPropertiesSection.getKeys(false)) {
                val obj = texturesPropertiesSection.get(key) ?: continue
                textureProperties[key] = TextureData.PropertyWriter.fromObject(obj)
            }
        }
        val parent = texturePackSection.getString(PARENT_MODEL_KEY) ?: return null
        val texturesSection = texturePackSection.getConfigurationSection(TEXTURES_KEY) ?: return null
        val textures = hashMapOf<String, String>()
        for (key in texturesSection.getKeys(false)) {
            textures[key] = texturesSection.getString(key) ?: continue
        }
        return TextureData(modelPath, textureProperties, parent, textures)
    }

}