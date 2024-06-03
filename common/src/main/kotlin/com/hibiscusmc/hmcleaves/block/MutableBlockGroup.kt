package com.hibiscusmc.hmcleaves.block

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import com.hibiscusmc.hmcleaves.world.PositionInChunk
import org.bukkit.ChunkSnapshot
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import java.util.ArrayDeque
import java.util.UUID

data class BlockGroup(
    val world: UUID,
    val minX: Int,
    val minY: Int,
    val minZ: Int,
    val maxX: Int,
    val maxY: Int,
    val maxZ: Int
)

class MutableBlockGroup(
    private val world: UUID,
    private var minX: Int,
    private var minY: Int,
    private var minZ: Int,
    private var maxX: Int,
    private var maxY: Int,
    private var maxZ: Int
) {

    fun add(position: PositionInChunk) {
        this.minX = this.minX.coerceAtMost(position.x)
        this.minY = this.minY.coerceAtMost(position.y)
        this.minZ = this.minZ.coerceAtMost(position.z)
        this.maxX = this.maxX.coerceAtLeast(position.x)
        this.maxY = this.maxY.coerceAtLeast(position.y)
        this.maxZ = this.maxZ.coerceAtLeast(position.z)
    }

    fun toBlockGroup(): BlockGroup {
        return BlockGroup(this.world, this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ)
    }

    fun getWorld() = this.world

    fun getMinX() = this.minX

    fun getMinY() = this.minY

    fun getMinZ() = this.minZ

    fun getMaxX() = this.maxX

    fun getMaxY() = this.maxY

    fun getMaxZ() = this.maxZ

}

fun findBlockGroupsInChunk(
    plugin: HMCLeaves,
    config: LeavesConfig,
    world: World,
    chunk: ChunkSnapshot,
    leavesChunk: LeavesChunk
): Collection<BlockGroup> {
    val found = mutableListOf<BlockGroup>()
    val minHeight = world.minHeight
    val maxHeight = world.maxHeight
    val usedBlocks: Array<Array<Array<Boolean>>> = Array(16) {
        Array(16) {
            Array(maxHeight - minHeight) { false }
        }
    }

    val worldUUID = world.uid
    for (x in 0 until 16) {
        for (z in 0 until 16) {
            for (y in minHeight until maxHeight) {
                val pos = PositionInChunk(worldUUID, x, y, z)
                if (usedBlocks[x][z][y - minHeight]) continue
                usedBlocks[x][z][y - minHeight] = true
                val block = chunk.getBlockData(x, y, z)
                val data = config.getDefaultBlockData(block.material) ?: continue
                leavesChunk.setDefaultBlock(pos, data)
                val group = MutableBlockGroup(worldUUID, x, y, z, x, y, z)
                findConnectedBlocks(
                    plugin,
                    config,
                    pos,
                    usedBlocks,
                    chunk,
                    leavesChunk,
                    minHeight,
                    maxHeight,
                    group
                )
                found.add(group.toBlockGroup())
            }
        }
    }

    return found
}

private fun findConnectedBlocks(
    plugin: HMCLeaves,
    config: LeavesConfig,
    originalPos: PositionInChunk,
    used: Array<Array<Array<Boolean>>>,
    chunk: ChunkSnapshot,
    leavesChunk: LeavesChunk,
    minHeight: Int,
    maxHeight: Int,
    group: MutableBlockGroup
) {
    val stack = ArrayDeque<PositionInChunk>()
    stack.push(originalPos)
    while (!stack.isEmpty()) {
        val startPos = stack.removeFirst()
        for (direction in BLOCK_DIRECTIONS) {
            val relative = startPos.relative(direction, minHeight, maxHeight) ?: continue
            val x = relative.x
            val y = relative.y
            val z = relative.z
            if (used[x][z][y - minHeight]) continue
            used[x][z][y - minHeight] = true
            val block = chunk.getBlockData(x, y, z)
            val data = config.getDefaultBlockData(block.material) ?: continue
            leavesChunk.setDefaultBlock(relative, data)
            group.add(relative)
            stack.push(relative)
        }
    }
}