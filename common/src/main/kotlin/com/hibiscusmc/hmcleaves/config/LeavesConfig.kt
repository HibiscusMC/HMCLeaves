package com.hibiscusmc.hmcleaves.config

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.*
import com.hibiscusmc.hmcleaves.item.BlockDrops
import com.hibiscusmc.hmcleaves.item.ConstantItemSupplier
import com.hibiscusmc.hmcleaves.item.ItemSupplier
import com.hibiscusmc.hmcleaves.item.SingleBlockDropReplacement
import com.hibiscusmc.hmcleaves.listener.ConnectedBlockFacingUpDestroyListener
import com.hibiscusmc.hmcleaves.pdc.PDCUtil
import com.hibiscusmc.hmcleaves.util.parseAsAdventure
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import java.util.*

private val LEAVES_MATERIALS = Material.entries.filter { Tag.LEAVES.isTagged(it) }.toList()
private val LOG_MATERIALS = Material.entries.filter { Tag.LOGS.isTagged(it) }.toList()
private val SAPLING_MATERIALS = Material.entries.filter { Tag.SAPLINGS.isTagged(it) }.toList()

private const val BLOCKS_KEY = "blocks"
private const val TYPE_KEY = "type"
private const val VISUAL_MATERIAL_KEY = "visual-material"
private const val WORLD_MATERIAL_KEY = "world-material"
private const val PROPERTIES_KEY = "properties"
private const val ITEM_KEY = "item"
private const val MATERIAL_KEY = "material"
private const val MODEL_DATA_KEY = "model-data"
private const val NAME_KEY = "name"
private const val LORE_KEY = "lore"
private const val DROPS_KEY = "drops"

const val DEBUG_STICK_ID = "debug_stick"

class LeavesConfig(private val plugin: HMCLeaves) {

    private val filePath = plugin.dataFolder.toPath().resolve("config.yml")

    private val defaultBlockData: MutableMap<Material, BlockData> = EnumMap(org.bukkit.Material::class.java)

    private val blockData: MutableMap<String, BlockData> = hashMapOf()

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

    fun getDefaultBLockData(material: Material): BlockData? {
        return this.defaultBlockData[material]
    }

    fun getBlockData(id: String): BlockData? {
        return this.blockData[id]
    }

    fun getBlockDataIds(): Collection<String> {
        return this.blockData.keys
    }

    fun getAllBlockData(): Map<String, BlockData> {
        return Collections.unmodifiableMap(this.blockData)
    }

    fun getDirectionalBlockData(original: BlockData, axis: BlockAxis): BlockData? {
        val newId = "${original.id}_${axis.toString().lowercase()}"
        return this.getBlockData(newId) ?: original
    }

    fun getNonDirectionalBlockData(original: BlockData): BlockData? {
        val index = original.id.lastIndexOf('_')
        if (index == -1) {
            return original
        }
        val id = original.id.substring(0, index)
        return getBlockData(id)
    }

    fun getBlockDataFromItem(item: ItemStack): BlockData? {
        val id = PDCUtil.getItemId(item) ?: return getDefaultBLockData(item.type)
        return this.blockData[id]
    }

    fun load() {
        val file = filePath.toFile()
        if (!file.exists()) {
            file.parentFile.mkdirs()
            this.plugin.saveDefaultConfig()
        }
        val config = YamlConfiguration.loadConfiguration(file)

        loadDefaults()
        loadBlocks(config)
    }

    private fun getDefaultIdFromMaterial(material: Material): String {
        return "default_${material.toString().lowercase()}"
    }

    // example: default_cave_vines -> default_cave_vines_plant
    fun getPlantFromId(id: String): BlockData? {
        return this.getBlockData("${id}_plant")
    }

    // example: default_cave_vines_plant -> default_cave_vines
    fun getNonPlantFromId(id: String): BlockData? {
        val index = id.indexOf("_plant")
        if (index == -1) return this.getBlockData(id)
        return this.getBlockData(id.substring(0, index))
    }


    private fun loadDefaults() {
        this.loadDefaultLeaves()
        this.loadDefaultLogs()
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
        val properties: Map<Property<*>, *> = hashMapOf(
            Property.DISTANCE to 7,
            Property.PERSISTENT to false
        )

        for (material in LEAVES_MATERIALS) {
            val id = getDefaultIdFromMaterial(material)
            val data = BlockData.createLeaves(
                id,
                material,
                properties,
                ConstantItemSupplier(ItemStack(material), id),
                SingleBlockDropReplacement()
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
                ConstantItemSupplier(ItemStack(material), id),
                SingleBlockDropReplacement()
            )
            this.defaultBlockData[material] = data
            this.blockData[id] = data
        }
    }

    private fun loadDefaultSugarcane() {
        val properties: Map<Property<*>, *> = hashMapOf(
            Property.AGE to 0
        )
        val material = Material.SUGAR_CANE
        val id = getDefaultIdFromMaterial(material)
        val data = BlockData.createSugarcane(
            id,
            material,
            properties,
            ConstantItemSupplier(ItemStack(material), id),
            SingleBlockDropReplacement()
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
                ConstantItemSupplier(ItemStack(material), id),
                SingleBlockDropReplacement()
            )
            this.defaultBlockData[material] = data
            this.blockData[id] = data
        }
    }

    private fun loadDefaultCaveVines() {
        val properties: Map<Property<*>, *> = hashMapOf(
            Property.AGE to 0
        )
        val material = Material.CAVE_VINES
        val id = getDefaultIdFromMaterial(material)
        val data = BlockData.createCaveVines(
            id,
            material,
            properties,
            ConstantItemSupplier(ItemStack(Material.GLOW_BERRIES), id),
            SingleBlockDropReplacement(),
            setOf(getDefaultIdFromMaterial(Material.CAVE_VINES_PLANT))
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
            properties,
            ConstantItemSupplier(ItemStack(Material.GLOW_BERRIES), id),
            SingleBlockDropReplacement(),
            setOf(getDefaultIdFromMaterial(Material.CAVE_VINES))
        )
        this.defaultBlockData[material] = data
        this.blockData[id] = data
    }

    private fun loadDefaultWeepingVines() {
        val properties: Map<Property<*>, *> = hashMapOf(
            Property.AGE to 0
        )
        val material = Material.WEEPING_VINES
        val id = getDefaultIdFromMaterial(material)
        val data = BlockData.createWeepingVines(
            id,
            material,
            properties,
            ConstantItemSupplier(ItemStack(material), id),
            SingleBlockDropReplacement(),
            setOf(getDefaultIdFromMaterial(Material.WEEPING_VINES_PLANT))
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
            properties,
            ConstantItemSupplier(ItemStack(material), id),
            SingleBlockDropReplacement(),
            setOf(getDefaultIdFromMaterial(Material.WEEPING_VINES))
        )
        this.defaultBlockData[material] = data
        this.blockData[id] = data
    }

    private fun loadDefaultTwistingVines() {
        val properties: Map<Property<*>, *> = hashMapOf(
            Property.AGE to 0
        )
        val material = Material.TWISTING_VINES
        val id = getDefaultIdFromMaterial(material)
        val data = BlockData.createTwistingVines(
            id,
            material,
            properties,
            ConstantItemSupplier(ItemStack(material), id),
            SingleBlockDropReplacement(),
            setOf(getDefaultIdFromMaterial(Material.TWISTING_VINES))
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
            properties,
            ConstantItemSupplier(ItemStack(material), id),
            SingleBlockDropReplacement(),
            setOf(getDefaultIdFromMaterial(Material.TWISTING_VINES_PLANT))
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
            properties,
            ConstantItemSupplier(ItemStack(material), id),
            SingleBlockDropReplacement(),
            setOf(getDefaultIdFromMaterial(Material.KELP_PLANT))
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
            properties,
            ConstantItemSupplier(ItemStack(material), id),
            SingleBlockDropReplacement(),
            setOf(getDefaultIdFromMaterial(Material.KELP))
        )
        this.defaultBlockData[material] = data
        this.blockData[id] = data
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

            val propertiesSection = section.getConfigurationSection(PROPERTIES_KEY)
                ?: throw IllegalArgumentException("$id requires $PROPERTIES_KEY")
            val properties: MutableMap<Property<*>, Any> = hashMapOf()
            for (propertyKey in propertiesSection.getKeys(false)) {
                val property: Property<Any> = Property.getPropertyByKey(propertyKey)
                    ?: throw IllegalArgumentException("Invalid property $propertyKey")
                val value = propertiesSection.getString(propertyKey)?.let { property.converter(it) }
                    ?: throw IllegalArgumentException("Invalid property for $propertyKey")
                properties[property] = value
            }

            val itemSupplier = section.getConfigurationSection(ITEM_KEY)?.let {
                this.loadItem(it, id)
            } ?: ConstantItemSupplier(ItemStack(worldMaterial), id)

            val blockDrops = loadDrops(section.getConfigurationSection(DROPS_KEY), id, type)
            val data = type.blockSupplier(
                id,
                visualMaterial,
                worldMaterial,
                properties,
                itemSupplier,
                blockDrops,
                setOf() // todo
            )
            blockData[id] = data
        }
    }

    private fun loadItem(section: ConfigurationSection, id: String): ItemSupplier {
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
        return ConstantItemSupplier(item, id)
    }

    private fun loadDrops(
        section: ConfigurationSection?,
        id: String,
        type: BlockType
    ): BlockDrops {
        if (section == null) {
            return type.defaultBlockDrops
        }
        // todo
        return type.defaultBlockDrops
    }

}