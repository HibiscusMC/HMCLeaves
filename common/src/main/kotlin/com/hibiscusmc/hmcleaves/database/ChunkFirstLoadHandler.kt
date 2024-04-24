package com.hibiscusmc.hmcleaves.database

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import com.hibiscusmc.hmcleaves.block.findBlockGroupsInChunk
import org.bukkit.ChunkSnapshot
import org.bukkit.World
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class ChunkFirstLoadHandler(
    private val plugin: HMCLeaves,
    private val config: LeavesConfig = plugin.getLeavesConfig(),
    private val executor: Executor = Executors.newFixedThreadPool(3)
)  {

    fun load(
        world: World,
        chunk: ChunkSnapshot,
        leavesChunk: LeavesChunk
    ) {
        executor.execute {
            findBlockGroupsInChunk(
                this.config,
                world,
                chunk,
                leavesChunk
            )
        }
    }

}