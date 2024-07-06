package com.hibiscusmc.hmcleaves.config

import com.github.retrooper.packetevents.protocol.world.states.enums.Instrument
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.block.BlockFamily
import com.hibiscusmc.hmcleaves.block.BlockType
import com.hibiscusmc.hmcleaves.block.Property
import com.hibiscusmc.hmcleaves.hook.Hooks
import org.bukkit.Axis
import org.bukkit.Material
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Locale


class TextureFileGenerator(
    private val plugin: HMCLeaves
) {

    private val folderPath: Path = this.plugin.dataFolder.toPath().resolve("textures")

    companion object {

        private const val VARIANTS_PATH: String = "variants"
        private const val MODEL_PATH: String = "model"

        private class Variants(val jsonData: List<JSONData>)

        private class JSONData(val key: String, private val id: String, val path: String?)

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
                    jsonWriter.beginArray()
                    jsonWriter.beginObject()
                    jsonWriter.name(MODEL_PATH).value(data.path)
                    jsonWriter.endObject()
                    jsonWriter.endArray()
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

    fun generateFile(material: Material, data: Collection<BlockData>, config: LeavesConfig) {
        if (data.isEmpty()) return
        val gsonBuilder = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(Variants::class.java, VariantsAdapter())
        val gson = gsonBuilder.create()
        val variants = Variants(
            data
                .map { blockData: BlockData ->
                    if (blockData.blockType == BlockType.LEAVES) {
                        return@map JSONData(
                            convertDataToString(blockData),
                            blockData.id,
                            blockData.modelPath
                        )
                    }
                    if (blockData.blockType == BlockType.SAPLING) {
                        return@map JSONData(
                            convertDataToString(blockData),
                            blockData.id,
                            blockData.modelPath
                        )
                    }
                    if (blockData.blockType == BlockType.LOG || blockData.blockType == BlockType.STRIPPED_LOG) {
                        val family = blockData.blockFamily
                        var axis = if (blockData.id == family.getFamilyId(BlockFamily.Type.AXIS_X)) Axis.X else null
                        if (axis == null) {
                            axis = if (blockData.id == family.getFamilyId(BlockFamily.Type.AXIS_Y)) Axis.Y else null
                        }
                        if (axis == null) {
                            axis = if (blockData.id == family.getFamilyId(BlockFamily.Type.AXIS_Z)) Axis.Z else null
                        }
                        if (axis == null) return@map null
                        return@map JSONData(
                            convertDataToString(blockData),
                            blockData.id,
                            "${blockData.modelPath}_${axis.name.lowercase()}"
                        )
                    }
                    throw IllegalArgumentException("Cannot convert data type: " + blockData.blockType)
                }
                .filterNotNull()
                .toList()
        )
        val json = gson.toJson(variants)
        val file = folderPath.resolve(material.name.lowercase(Locale.getDefault()) + ".json").toFile()
        try {
            if (!folderPath.toFile().exists()) {
                Files.createDirectory(this.folderPath)
            }
            file.delete()
            if (!file.exists()) {
                file.createNewFile()
            }
            Files.writeString(file.toPath(), json, StandardOpenOption.WRITE)
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }
        if (!config.useTextureHook()) return
        Hooks.transferTextures(file)
    }

    private fun convertDataToString(blockData: BlockData): String {
        if (blockData.blockType == BlockType.LEAVES) {
            return "${DISTANCE_KEY}=${blockData.properties[Property.DISTANCE].toString()},${PERSISTENT_KEY}=${blockData.properties[Property.PERSISTENT]}"
        }
        if (blockData.blockType == BlockType.SAPLING) {
            return "${STAGE}=${blockData.properties[Property.AGE]}"
        }
        if (blockData.blockType == BlockType.LOG || blockData.blockType == BlockType.STRIPPED_LOG) {
            val instrument = blockData.properties[Property.INSTRUMENT]
            if (instrument !is Instrument) return ""
            return "${INSTRUMENT_KEY}=${instrument.name},${NOTE_KEY}=${blockData.properties[Property.NOTE]}"
        }
        throw IllegalArgumentException(
            "${blockData.blockType} cannot be converted to a string for texture file!"
        )
    }


}