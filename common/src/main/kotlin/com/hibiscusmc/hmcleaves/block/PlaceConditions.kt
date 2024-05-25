package com.hibiscusmc.hmcleaves.block

import com.hibiscusmc.hmcleaves.util.getChunkPosition
import com.hibiscusmc.hmcleaves.util.getPositionInChunk
import com.hibiscusmc.hmcleaves.util.toPositionInChunk
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import com.hibiscusmc.hmcleaves.world.WorldManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.World
import org.bukkit.block.Block
import java.util.Collections
import java.util.EnumMap

class PlaceConditions(
    private val directionConditions: Map<BlockDirection, List<PlaceCondition>>
) {

    companion object {
        val EMPTY = listOf(PlaceConditions(EnumMap(BlockDirection::class.java)))

//        fun createSugarCane(sugarCaneId: String): List<PlaceConditions> {
//            val map = EnumMap<BlockDirection, List<PlaceCondition>>(BlockDirection::class.java)
//            val tagCondition = TagPlaceCondition(listOf(Tag.DIRT, Tag.SAND))
//            val idCondition = BlockIdPlaceCondition(Collections.singleton(sugarCaneId))
//            val conditions = listOf(tagCondition, idCondition)
//            map[BlockDirection.DOWN] = conditions
//            return listOf(PlaceConditions(map))
//        }

//        private const val SUGAR_CANE_ID = "sugarcane"

        fun getById(id: String, blockId: String): List<PlaceConditions> {
            return when (id) {
//                SUGAR_CANE_ID -> createSugarCane(blockId)
                else -> EMPTY
            }
        }

    }

    fun canBePlaced(
        worldManager: WorldManager,
        world: World,
        location: Location,
        materialGetter: (Location) -> Material = { loc -> loc.block.type },
        blockDataGetter: (Location, LeavesChunk) -> BlockData? = Getter@{ loc, chunk -> chunk[loc.toPositionInChunk() ?: return@Getter null] }
    ): Boolean {
        val block = world.getBlockAt(location)
        val leavesWorld = worldManager[world.uid]
        for (entry in directionConditions) {
            val direction = entry.key
            val conditions = entry.value
            val relativeBlock = block.getRelative(direction.bukkitBlockFace)
            val leavesChunk = leavesWorld?.get(relativeBlock.getChunkPosition())
            val relativeData =
                leavesChunk?.let { blockDataGetter(relativeBlock.location, it) }

//                blockDataGetter(relativeBlock.location, leavesWorld?.get(relativeBlock.getChunkPosition()))
//                leavesWorld?.get(relativeBlock.getChunkPosition())?.get(relativeBlock.getPositionInChunk())
            var matches = false
            for (condition in conditions) {
                if (condition.matches(materialGetter(relativeBlock.location), relativeData)) {
                    matches = true
                    break
                }
            }
            if (!matches) return false
        }
        return true
    }

}

interface PlaceCondition {

    fun matches(material: Material, blockData: BlockData?): Boolean

}

class TagPlaceCondition(private val tags: List<Tag<Material>>) : PlaceCondition {

    override fun matches(material: Material, blockData: BlockData?): Boolean {
        for (tag in this.tags) {
            if (tag.isTagged(material)) return true
        }
        return false
    }
}

class MaterialPlaceCondition(private val materials: Set<Material>) : PlaceCondition {

    override fun matches(material: Material, blockData: BlockData?): Boolean {
        return this.materials.contains(material)
    }

}

class BlockIdPlaceCondition(private val ids: Set<String>) : PlaceCondition {

    override fun matches(material: Material, blockData: BlockData?): Boolean {
        return this.ids.contains(blockData?.id ?: return false)
    }

}