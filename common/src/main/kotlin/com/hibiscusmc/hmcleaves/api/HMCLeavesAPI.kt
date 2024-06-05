package com.hibiscusmc.hmcleaves.api

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.util.toPosition
import com.hibiscusmc.hmcleaves.world.ChunkPosition
import com.hibiscusmc.hmcleaves.world.LeavesChunk
import com.hibiscusmc.hmcleaves.world.Position
import com.hibiscusmc.hmcleaves.world.WorldManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin


class HMCLeavesAPI private constructor(private val plugin: HMCLeaves) {

    companion object {
        var instance: HMCLeavesAPI? = null
            get() {
                if (field == null) {
                    field = HMCLeavesAPI(JavaPlugin.getPlugin(HMCLeaves::class.java))
                }
                return field
            }
            private set
    }

    private val config = plugin.leavesConfig
    private val worldManager: WorldManager = plugin.worldManager


    /**
     * Get the [BlockData] at the given [Location].
     * return [BlockData.EMPTY] if the block is not a custom block.
     */
    fun getBlockDataAt(location: Location): BlockData? {
        return this.worldManager[location.toPosition() ?: return null]
    }

    /**
     * Check if the given [Location] is a custom block.
     * @param location The location to check.
     * @return true if the location is a custom block, false if not.
     */
    fun isCustomBlock(location: Location): Boolean {
        return this.worldManager[location.toPosition() ?: return false] != null
    }

    /**
     * Set the [BlockData] at the given [Location].
     * @param location The location to set the block at.
     * @param id The id of the block to set.
     * @param setBlockInWorld If true, the block will be set in the world.
     * If false, the block will only be set in the cache,
     * which may cause issues so be very careful if you do this.
     * @return true if the block was set, false if the block id was not found.
     */
    fun setCustomBlock(location: Location, id: String, setBlockInWorld: Boolean = true): Boolean {
        val blockData = config.getBlockData(id) ?: return false
        val position = location.toPosition() ?: return false
        val chunkPosition = position.getChunkPosition()
        val leavesChunk = this.worldManager.getOrAdd(position.world).getOrAdd(chunkPosition)
        leavesChunk[position.toPositionInChunk()] = blockData
        if (setBlockInWorld) {
            location.block.type = blockData.worldMaterial
        }
        return true
    }

    /**
     * Remove the [BlockData] at the given [Location].
     * @param location The location to remove the block at.
     * @param setBlockInWorld If true, the block will be set to air in the world.
     * If false, the block will only be removed from the cache,
     * which may cause issues so be very careful if you do this.
     * @return The [BlockData] that was removed.
     */
    fun removeBlockDataAt(location: Location, setBlockInWorld: Boolean): BlockData? {
        if (setBlockInWorld) {
            location.block.type = Material.AIR
        }
        val position = location.toPosition() ?: return null
        val chunkPosition = position.getChunkPosition()
        val leavesChunk = this.worldManager.getOrAdd(position.world).getOrAdd(chunkPosition)
        return leavesChunk.remove(position.toPositionInChunk(), true)
    }

    /**
     *
     * @param world The world to check.
     * @return true if the world is whitelisted, false if not.
     */
    fun isWorldWhitelisted(world: World): Boolean {
        return config.isWorldWhitelisted(world.name)
    }

    /**
     *
     * @param world The world to check.
     * @return true if the world is whitelisted, false if not.
     */
    fun isWorldWhitelisted(world: String): Boolean {
        return config.isWorldWhitelisted(world)
    }

    /**
     * Add a world to the whitelist.
     * @param world The world to add.
     */
    fun addWhitelistedWorld(world: World) {
        config.addWhitelistedWorld(world.name)
    }

    /**
     * Add a world to the whitelist.
     * @param world The world to add.
     */
    fun addWhitelistedWorld(world: String) {
        config.addWhitelistedWorld(world)
    }

    /**
     * Remove a world from the whitelist.
     * @param world The world to remove.
     */
    fun removeWhitelistedWorld(world: World) {
        config.removeWhitelistedWorld(world.name)
    }

    /**
     * Remove a world from the whitelist.
     * @param world The world to remove.
     */
    fun removeWhitelistedWorld(world: String) {
        config.removeWhitelistedWorld(world)
    }

    val isWorldWhitelistEnabled: Boolean
        get() = config.isWorldWhitelistEnabled()

    fun getLeavesChunk(chunkPosition: ChunkPosition): LeavesChunk? {
        return this.worldManager[chunkPosition.world]?.get(chunkPosition)
    }

    fun getLeavesChunk(position: Position): LeavesChunk? {
        return this.getLeavesChunk(position.getChunkPosition())
    }

}