package com.hibiscusmc.hmcleaves.texture

import com.google.gson.stream.JsonWriter

data class TextureData(
    val modelPath: String,
    val properties: Map<String, Pair<Any, PropertyWriter>>,
    val parent: String? = null,
    val textures: Map<String, String> = mapOf()
) {

    fun withProperties(properties: Map<String, Pair<Any, PropertyWriter>>): TextureData {
        return TextureData(
            this.modelPath,
            this.properties + properties,
            this.parent,
            this.textures
        )
    }

    interface PropertyWriter {

        fun write(jsonWriter: JsonWriter, obj: Any)

        companion object {

            val STRING_WRITER = object : PropertyWriter {
                override fun write(jsonWriter: JsonWriter, obj: Any) {
                    jsonWriter.value(obj as String)
                }
            }

            val INT_WRITER: PropertyWriter = object : PropertyWriter {
                override fun write(jsonWriter: JsonWriter, obj: Any) {
                    jsonWriter.value(obj as Int)
                }
            }

            fun fromObject(obj: Any): Pair<Any, PropertyWriter> {
                return when (obj::javaClass) {
                    Int::javaClass -> Pair(obj, INT_WRITER)
                    String::javaClass -> Pair(obj, STRING_WRITER)
                    else -> throw IllegalArgumentException("Invalid type: ${obj::javaClass}")
                }
            }

        }

    }

}