package com.hibiscusmc.hmcleaves.config

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.world.BlockData
import com.hibiscusmc.hmcleaves.world.Property
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.configuration.file.YamlConfiguration
import java.util.*

private val LEAVES_MATERIALS = Material.entries.filter { Tag.LEAVES.isTagged(it) }.toList()

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

    fun load() {
        val file = filePath.toFile()
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        val config = YamlConfiguration.loadConfiguration(file)

        loadDefaultLeaves()
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
                properties
            )
            this.defaultBlockData[material] = data
            this.blockData[id] = data
        }
    }

}