package com.hibiscusmc.hmcleaves.util

import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin


data class MetadataKey<V>(val key: NamespacedKey, val valueType: Class<V>) {


    override fun toString(): String {
        return "MetadataKey{" +
                "key=" + key +
                ", valueType=" + valueType +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MetadataKey<*>

        if (key != other.key) return false
        if (valueType != other.valueType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + valueType.hashCode()
        return result
    }

    companion object {
        private const val OBJECT_KEY_NAME = "object"

        /**
         * Creates a new [MetadataKey] with the given [NamespacedKey] and [java.lang.Object] type.
         * @param plugin - the plugin that this key belongs to
         * @return a new [MetadataKey] with the given [NamespacedKey] and [java.lang.Object] type.
         */
        @JvmStatic
        fun objectKey(plugin: JavaPlugin?): MetadataKey<Any> {
            return MetadataKey(
                NamespacedKey(plugin!!, OBJECT_KEY_NAME),
                Any::class.java
            )
        }

        /**
         * Creates a new [MetadataKey] with the given [NamespacedKey] and [V] type.
         * @param key - the [NamespacedKey] to use
         * @return a new [MetadataKey] with the given [NamespacedKey] and [V] type.
         */
        @JvmStatic
        fun <V> of(key: NamespacedKey, valueType: Class<V>): MetadataKey<V> {
            return MetadataKey(key, valueType)
        }
    }
}