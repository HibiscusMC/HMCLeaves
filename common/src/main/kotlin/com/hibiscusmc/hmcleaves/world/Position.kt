package com.hibiscusmc.hmcleaves.world

import com.hibiscusmc.hmcleaves.block.BlockDirection
import com.hibiscusmc.hmcleaves.util.getChunkPosition
import com.hibiscusmc.hmcleaves.util.toBlockDirection
import org.bukkit.block.BlockFace
import java.util.*

data class Position(val world: UUID, val x: Int, val y: Int, val z: Int) {


    private val hash = calculateHash()

    fun relative(direction: BlockDirection, minHeight: Int, maxHeight: Int): Position? {
        val newY = this.y + direction.yOffset
        if (newY < minHeight || newY >= maxHeight) {
            return null
        }
        return Position(
            this.world,
            this.x + direction.xOffset,
            this.y + direction.yOffset,
            this.z + direction.zOffset
        )
    }

    fun relative(direction: BlockFace, minHeight: Int, maxHeight: Int): Position? {
        return this.relative(direction.toBlockDirection() ?: return null, minHeight, maxHeight)
    }

    fun toPositionInChunk(): PositionInChunk {
        return PositionInChunk(
            this.world,
            convertCoordToCoordInChunk(this.x),
            this.y,
            convertCoordToCoordInChunk(this.z)
        )
    }

    fun getChunkPosition(): ChunkPosition {
        return ChunkPosition(
            this.world,
            convertCoordToChunkCoord(this.x),
            convertCoordToChunkCoord(this.z)
        )
    }

    private fun calculateHash(): Int {
        var result = world.hashCode()
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Position

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false
        if (world != other.world) return false

        return true
    }

    override fun hashCode(): Int {
        return hash
    }


}

/**
 * x, y, and z are relative
 */
data class PositionInChunk(val world: UUID, val x: Int, val y: Int, val z: Int) {

    init {
        if (x < 0 || x > 15) throw IllegalStateException("Invalid x in PositionInChunk: $x")
        if (z < 0 || z > 15) throw IllegalStateException("Invalid z in PositionInChunk: $z")
    }

    private var hash = calculateHash()

    fun relative(direction: BlockDirection, minHeight: Int, maxHeight: Int): PositionInChunk? {
        val newX = this.x + direction.xOffset
        val newY = this.y + direction.yOffset
        val newZ = this.z + direction.zOffset
        if (newX < 0 || newX > 15 || newZ < 0 || newZ > 15 || newY < minHeight || newY >= maxHeight) {
            return null
        }
        return PositionInChunk(
            this.world,
            newX,
            newY,
            newZ
        )
    }

    fun relative(direction: BlockFace, minHeight: Int, maxHeight: Int): PositionInChunk? {
        return this.relative(direction.toBlockDirection() ?: return null, minHeight, maxHeight)
    }

    private fun calculateHash(): Int {
        var result = world.hashCode()
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PositionInChunk

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false
        if (world != other.world) return false

        return true
    }

    override fun hashCode(): Int {
        return hash
    }

}

data class ChunkPosition(val world: UUID, val x: Int, val z: Int) {

    private var hash = calculateHash()

    private fun calculateHash(): Int {
        var result = world.hashCode()
        result = 31 * result + x
        result = 31 * result + z
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChunkPosition

        if (x != other.x) return false
        if (z != other.z) return false
        if (world != other.world) return false

        return true
    }

    override fun hashCode(): Int {
        return hash
    }

}

fun convertCoordToCoordInChunk(value: Int): Int {
    return value and 15;
}

fun convertCoordToChunkCoord(value: Int): Int {
    return value shr 4
}