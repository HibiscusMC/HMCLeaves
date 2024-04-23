package com.hibiscusmc.hmcleaves.world

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import org.bukkit.ChunkSnapshot
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class BlockGroup(
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
}

fun findBlockGroupsInChunk(
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
                val data = config.getDefaultBLockData(block.material) ?: continue
                leavesChunk[pos] = data
                val group = BlockGroup(worldUUID, x, y, z, x, y, z)
                findConnectedBlocks(
                    config,
                    pos,
                    usedBlocks,
                    chunk,
                    leavesChunk,
                    minHeight,
                    maxHeight,
                    group
                )
                found.add(group)
            }
        }
    }

    return found
}

private fun findConnectedBlocks(
    config: LeavesConfig,
    startPos: PositionInChunk,
    used: Array<Array<Array<Boolean>>>,
    chunk: ChunkSnapshot,
    leavesChunk: LeavesChunk,
    minHeight: Int,
    maxHeight: Int,
    group: BlockGroup
) {
    for (direction in BLOCK_DIRECTIONS) {
        val relative = startPos.relative(direction, minHeight, maxHeight) ?: continue
        val x = relative.x
        val y = relative.y
        val z = relative.z
        if (used[x][z][y - minHeight]) continue
        used[x][z][y - minHeight] = true
        val block = chunk.getBlockData(x, y, z)
        val data = config.getDefaultBLockData(block.material) ?: continue
        leavesChunk[relative] = data
        group.add(relative)
        findConnectedBlocks(
            config,
            relative,
            used,
            chunk,
            leavesChunk,
            minHeight,
            maxHeight,
            group
        )
    }

}