package com.hibiscusmc.hmcleaves.texture

import com.github.retrooper.packetevents.protocol.world.states.enums.Instrument
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.block.BlockType
import com.hibiscusmc.hmcleaves.block.Property
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import org.bukkit.Material
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.Locale
import java.util.SortedSet


class TextureFileGenerator(
    private val plugin: HMCLeaves
) {

    private val texturesFolderPath = this.plugin.dataFolder.toPath().resolve("textures")
    private val modelsFolderPath = this.plugin.dataFolder.toPath().resolve("models")

    companion object {

        private const val VARIANTS_PATH: String = "variants"
        private const val MODEL_PATH: String = "model"

        private data class Variants(val jsonData: SortedSet<JSONData>)

        private data class JSONData(val key: String, val textureData: TextureData)

        // leaves
        private const val DISTANCE_KEY: String = "distance"
        private const val PERSISTENT_KEY: String = "persistent"
        private const val INSTRUMENT_KEY: String = "instrument"
        private const val NOTE_KEY: String = "note"

        // saplings
        private const val STAGE: String = "stage"

        private class VariantsAdapter : TypeAdapter<Variants>() {
            @Throws(IOException::class)
            override fun write(jsonWriter: JsonWriter, variants: Variants) {
                jsonWriter.beginObject()
                jsonWriter.name(VARIANTS_PATH)
                jsonWriter.beginObject()
                for (data in variants.jsonData) {
                    jsonWriter.name(data.key)
                    jsonWriter.beginObject()
                    jsonWriter.name(MODEL_PATH).value(data.textureData.modelPath)
                    data.textureData.properties.forEach {
                        jsonWriter.name(it.key)
                        it.value.second.write(jsonWriter, it.value.first)
                    }
                    jsonWriter.endObject()
                }
                jsonWriter.endObject()
                jsonWriter.endObject()
            }

            @Throws(IOException::class)
            override fun read(jsonReader: JsonReader): Variants? {
                return null
            }
        }
    }

    fun generateFiles(material: Material, data: Collection<BlockData>, config: LeavesConfig): Collection<File> {
        val files = hashSetOf<File>()
        this.generateTextureFiles(material, data, config)?.let { files.add(it) }
        this.generateModelFiles(data).forEach { files.add(it) }
        return files
    }

    private fun generateTextureFiles(material: Material, data: Collection<BlockData>, config: LeavesConfig): File? {
        if (data.isEmpty()) return null
        val gsonBuilder = GsonBuilder()
            .disableHtmlEscaping()
            .registerTypeAdapter(Variants::class.java, VariantsAdapter())
        val gson = gsonBuilder.create()
        val variants = Variants(
            data.map { blockData: BlockData ->
                return@map JSONData(
                    convertDataToString(blockData),
                    blockData.blockMechanics.textureData ?: return@map null
                )
            }
                .filterNotNull()
                .toSortedSet { first, second -> first.key.compareTo(second.key) }
        )
        val json = gson.toJson(variants)
        val file = texturesFolderPath.resolve(material.name.lowercase(Locale.getDefault()) + ".json").toFile()
        try {
            if (!texturesFolderPath.toFile().exists()) {
                Files.createDirectory(this.texturesFolderPath)
            }
            file.delete()
            if (!file.exists()) {
                file.createNewFile()
            }
            Files.writeString(file.toPath(), json, StandardOpenOption.WRITE)
            return file
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    private fun generateModelFiles(data: Collection<BlockData>): Collection<File> {
        if (data.isEmpty()) return emptySet()
        val gsonBuilder = GsonBuilder()
            .disableHtmlEscaping()
        val gson = gsonBuilder.create()
        val files = hashSetOf<File>()
        data.map { Pair(it.id, it.blockMechanics.textureData) }
            .forEach {
                val jsonObject = JsonObject()
                val id = it.first
                val textureData = it.second ?: return@forEach
                jsonObject.addProperty("parent", textureData.parent ?: return@forEach)
                val texturesJson = JsonObject()
                for (entry in textureData.textures) {
                    texturesJson.addProperty(entry.key, entry.value)
                }
                jsonObject.add("textures", texturesJson)
                val file = this.modelsFolderPath.resolve("${id.lowercase()}.json").toFile()
                try {
                    if (!modelsFolderPath.toFile().exists()) {
                        Files.createDirectory(this.modelsFolderPath)
                    }
                    file.delete()
                    if (!file.exists()) {
                        file.createNewFile()
                    }
                    val json = gson.toJson(jsonObject)
                    Files.writeString(file.toPath(), json, StandardOpenOption.WRITE)
                    files.add(file)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        return files
    }

    private fun convertDataToString(blockData: BlockData): String {
        if (blockData.blockType == BlockType.LEAVES) {
            return "$DISTANCE_KEY=${blockData.properties[Property.DISTANCE].toString()},$PERSISTENT_KEY=${blockData.properties[Property.PERSISTENT]}"
        }
        if (blockData.blockType == BlockType.SAPLING) {
            return "$STAGE=${blockData.properties[Property.STAGE]}"
        }
        if (blockData.blockType == BlockType.LOG || blockData.blockType == BlockType.STRIPPED_LOG) {
            val instrument = blockData.properties[Property.INSTRUMENT]
            if (instrument !is Instrument) return ""
            return "$INSTRUMENT_KEY=${instrument.name.lowercase()},$NOTE_KEY=${blockData.properties[Property.NOTE]}"
        }
        throw IllegalArgumentException(
            "${blockData.blockType} cannot be converted to a string for texture file!"
        )
    }


}