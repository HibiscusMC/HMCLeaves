package com.hibiscusmc.hmcleaves.database

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockGroup
import com.hibiscusmc.hmcleaves.block.findBlockGroupsInChunk
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import org.bukkit.ChunkSnapshot
import org.bukkit.World

class ChunkFirstLoadHandler(
    private val plugin: HMCLeaves,
    private val config: LeavesConfig = plugin.leavesConfig
) {

    fun load(
        world: World,
        chunk: ChunkSnapshot,
        leavesChunk: LeavesChunk
    ): Collection<BlockGroup> {
        return findBlockGroupsInChunk(
            this.config,
            world,
            chunk,
            leavesChunk
        )
    }

}