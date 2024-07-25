package com.hibiscusmc.hmcleaves.hook.worldedit.regular;

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.world.Position
import com.sk89q.jnbt.CompoundTag
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.extent.AbstractDelegateExtent
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockStateHolder
import com.sk89q.worldedit.world.block.BlockTypes
import org.bukkit.Bukkit


class WorldEditDelegate(
    private val plugin: HMCLeaves,
    private val config: LeavesConfig,
    private val event: EditSessionEvent
) : AbstractDelegateExtent(event.extent) {

    @Throws(WorldEditException::class)
    override fun <T : BlockStateHolder<T>?> setBlock(pos: BlockVector3, block: T): Boolean {
        val position = Position(BukkitAdapter.adapt(this.event.world).uid, pos.x, pos.y, pos.z)
        this.plugin.logger.info("$position")
        if (block !is BaseBlock) {
            return super.setBlock(pos, block)
        }
        if (block.getBlockType() !== BlockTypes.FURNACE) {
            return super.setBlock(pos, block)
        }
        val tag = block.nbtData ?: return super.setBlock(pos, block)
        val bukkitTag = tag.value[WorldEditHook.BUKKIT_NBT_TAG] as? CompoundTag ?: return super.setBlock(pos, block)
        val id = bukkitTag.getString(WorldEditHook.BLOCK_ID_KEY) ?: return super.setBlock(pos, block)
        if (id.isBlank()) {
            return super.setBlock(pos, block)
        }
        val blockData = config.getBlockData(id) ?: return super.setBlock(pos, block)
        val bukkitBlockData  = bukkitTag.getString(WorldEditHook.BLOCK_DATA_STRING)?.let {
            Bukkit.createBlockData(it)
        } ?: blockData.worldMaterial.createBlockData()
        this.plugin.worldManager[position] = blockData
        return extent.setBlock(pos, BukkitAdapter.adapt(bukkitBlockData))
    }
}