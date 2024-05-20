package com.hibiscusmc.hmcleaves.block

import java.util.EnumMap

enum class BlockSetting {

    PLACEABLE_IN_ENTITIES,
    GROWS_BERRIES,
    SOIL_DOES_NOT_REQUIRE_WATER

}

class BlockSettings(private val settings: Map<BlockSetting, Boolean>) {

    companion object {

        val EMPTY = BlockSettings(EnumMap(BlockSetting::class.java))
        val PLACEABLE_IN_ENTITIES = BlockSettings(EnumMap(mapOf(BlockSetting.PLACEABLE_IN_ENTITIES to true)))
        val ALL = BlockSettings(EnumMap(BlockSetting.entries.associateWith { true }))

    }

    fun isEnabled(setting: BlockSetting) : Boolean {
        return this.settings.getOrDefault(setting, false)
    }

}