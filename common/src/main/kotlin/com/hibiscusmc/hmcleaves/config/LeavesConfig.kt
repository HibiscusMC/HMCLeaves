package com.hibiscusmc.hmcleaves.config

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.block.BlockType
import com.hibiscusmc.hmcleaves.block.Property
import com.hibiscusmc.hmcleaves.pdc.PDCUtil
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.util.*

private val LEAVES_MATERIALS = Material.entries.filter { Tag.LEAVES.isTagged(it) }.toList()

const val BLOCKS_KEY = "blocks"
const val TYPE_KEY = "type"
const val VISUAL_MATERIAL_KEY = "visual-material"
const val WORLD_MATERIAL_KEY = "world-material"
const val PROPERTIES_KEY = "properties"

class LeavesConfig(private val plugin: HMCLeaves) {

    private val filePath = plugin.dataFolder.toPath().resolve("config.yml")

    private val defaultBlockData: MutableMap<Material, BlockData> = EnumMap(org.bukkit.Material::class.java)

    private val blockData: MutableMap<String, BlockData> = hashMapOf()

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

    fun getBlockDataFromItem(item: ItemStack) : BlockData? {
        val id = PDCUtil.getItemId(item) ?: return null
        return this.blockData[id]
    }

    fun load() {
        val file = filePath.toFile()
        if (!file.exists()) {
            file.parentFile.mkdirs()
            this.plugin.saveDefaultConfig()
        }
        val config = YamlConfiguration.loadConfiguration(file)

        loadDefaultLeaves()
        loadBlocks(config)
    }

    private fun getDefaultIdFromMaterial(material: Material): String {
        return "${material.toString().lowercase()}_default"
    }

    private fun loadDefaults() {
        loadDefaultLeaves()
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
                material,
                properties
            )
            this.defaultBlockData[material] = data
            this.blockData[id] = data
        }
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
            val properties: MutableMap<Property<*>, Any> = hashMapOf<Property<*>, Any>()
            for (propertyKey in propertiesSection.getKeys(false)) {
                val property: Property<Any> = Property.getPropertyByKey(propertyKey)
                    ?: throw IllegalArgumentException("Invalid property $propertyKey")
                val value = propertiesSection.getString(propertyKey)?.let { property.converter(it) }
                    ?: throw IllegalArgumentException("Invalid property for $propertyKey")
                properties[property] = value
            }

            val data = type.blockSupplier(id, visualMaterial, worldMaterial, properties)
            blockData[id] = data
        }
    }

    private fun loadItem(section: YamlConfiguration) : ItemStack {
        TODO("NOT YET IMPLEMENTED")
    }

}