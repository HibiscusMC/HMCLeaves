package com.hibiscusmc.hmcleaves.config

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.*
import com.hibiscusmc.hmcleaves.item.BlockDrops
import com.hibiscusmc.hmcleaves.item.ConstantItemSupplier
import com.hibiscusmc.hmcleaves.item.ItemSupplier
import com.hibiscusmc.hmcleaves.item.SingleBlockDropReplacement
import com.hibiscusmc.hmcleaves.pdc.PDCUtil
import com.hibiscusmc.hmcleaves.util.parseAsAdventure
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.util.*

private val LEAVES_MATERIALS = Material.entries.filter { Tag.LEAVES.isTagged(it) }.toList()

const val BLOCKS_KEY = "blocks"
const val TYPE_KEY = "type"
const val VISUAL_MATERIAL_KEY = "visual-material"
const val WORLD_MATERIAL_KEY = "world-material"
const val PROPERTIES_KEY = "properties"
const val ITEM_KEY = "item"
const val MATERIAL_KEY = "material"
const val MODEL_DATA_KEY = "model-data"
const val NAME_KEY = "name"
const val LORE_KEY = "lore"
const val DROPS_KEY = "drops"

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
                properties,
                ConstantItemSupplier(ItemStack(material), id),
                SingleBlockDropReplacement()
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
                blockDrops
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
        ) : BlockDrops {
        if (section == null) {
            return type.defaultBlockDrops
        }
        // todo
        return type.defaultBlockDrops
    }

}