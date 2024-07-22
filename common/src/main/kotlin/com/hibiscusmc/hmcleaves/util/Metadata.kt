package com.hibiscusmc.hmcleaves.util

@Suppress("unused")
class Metadata(private val metadata: MutableMap<MetadataKey<*>, Any>) {

    /**
     * @return the underlying map of metadata, changes to the map
     * will reflect the underlying map
     */
    fun get(): MutableMap<MetadataKey<*>, Any> {
        return this.metadata
    }

    /**
     * @return a copy of this Metadata object
     */
    fun copy(): Metadata {
        return Metadata(HashMap(this.metadata))
    }

    /**
     * @param metadata  - the metadata to copy
     * @param overwrite - whether to overwrite existing keys with the new metadata
     * @return a copy of this Metadata object with the new metadata
     */
    fun copyWith(metadata: Metadata, overwrite: Boolean): Metadata {
        val newMap: MutableMap<MetadataKey<*>, Any> = HashMap(this.metadata)
        if (overwrite) {
            newMap.putAll(metadata.get())
        } else {
            metadata.get().forEach { (key: MetadataKey<*>, value: Any) ->
                if (!newMap.containsKey(key)) {
                    newMap[key] = value
                }
            }
        }
        return Metadata(newMap)
    }

    /**
     * @param key - the key to get the value of
     * @param <T> - the type of the value
     * @return the value of the key, or null if the key does not exist or the value is not of the correct type
    </T> */
    fun <T> get(key: MetadataKey<T>): T? {
        val o = metadata[key] ?: return null
        if (!key.valueType.isInstance(o)) return null
        return key.valueType.cast(o)
    }

    /**
     * @param key   - the key to get the value of
     * @param value - the value to set the key to
     * @param <T>   - the type of the value
    </T> */
    fun <T : Any> set(key: MetadataKey<T>, value: T) {
        this.metadata[key] = value
    }

    /**
     * @param key       - the key to get the value of
     * @param value     - the value to set the key to
     * @param overwrite - whether to overwrite the value if the key already exists
     */
    fun set(key: MetadataKey<*>, value: Any, overwrite: Boolean) {
        if (overwrite) {
            this.metadata[key] = value
            return
        }
        metadata.putIfAbsent(key, value)
    }

    /**
     * @param metadata - the metadata to set, this clears the preexisting metadata
     */
    fun set(metadata: Map<MetadataKey<*>, Any>) {
        this.metadata.clear()
        this.metadata.putAll(metadata)
    }

    /**
     * This behaves similarly to [Metadata.copyWith]
     *
     * @param metadata  - the metadata to set
     * @param overwrite - whether to overwrite existing keys with the new metadata
     */
    fun putAll(metadata: Map<MetadataKey<*>, Any>, overwrite: Boolean) {
        if (overwrite) {
            this.metadata.putAll(metadata)
        } else {
            metadata.forEach { (key: MetadataKey<*>?, value: Any?) ->
                if (!this.metadata.containsKey(key)) {
                    this.metadata[key] = value
                }
            }
        }
    }

    /**
     * @param metadata  - the metadata to set
     * @param overwrite - whether to overwrite existing keys with the new metadata
     */
    fun putAll(metadata: Metadata, overwrite: Boolean) {
        this.putAll(metadata.get(), overwrite)
    }

    /**
     * @param key - the key to remove
     * @param <T> - the type of the value
     * @return the value of the key, or null if the key does not exist or the value is not of the correct type
    </T> */
    fun <T> remove(key: MetadataKey<T>): T? {
        val o: Any = metadata.remove(key) ?: return null
        if (!key.valueType.isInstance(o)) return null
        return key.valueType.cast(o)
    }

    override fun toString(): String {
        return "Metadata{" +
                "metadata=" + metadata +
                '}'
    }

    companion object {
        /**
         * @param metadata - the map of metadata to create the Metadata object from
         * @return a new Metadata object
         */
        @JvmStatic
        fun of(metadata: MutableMap<MetadataKey<*>, Any>): Metadata {
            return Metadata(metadata)
        }

        /**
         * @return a new immutable empty Metadata object
         */
        @JvmStatic
        fun empty(): Metadata {
            return Metadata(java.util.Map.of())
        }

        /**
         * @return a new mutable empty Metadata object
         */
        @JvmStatic
        fun mutableEmpty(): Metadata {
            return Metadata(HashMap())
        }
    }
}