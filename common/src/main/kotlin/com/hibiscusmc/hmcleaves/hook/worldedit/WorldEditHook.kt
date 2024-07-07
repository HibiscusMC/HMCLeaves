package com.hibiscusmc.hmcleaves.hook.worldedit

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.block.SaplingData
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import com.hibiscusmc.hmcleaves.world.Position
import com.hibiscusmc.hmcleaves.world.WorldManager
import com.sk89q.jnbt.CompoundTag
import com.sk89q.jnbt.StringTag
import com.sk89q.worldedit.EmptyClipboardException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.extent.AbstractDelegateExtent
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.math.transform.Identity
import com.sk89q.worldedit.math.transform.Transform
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.util.eventbus.Subscribe
import com.sk89q.worldedit.world.block.BaseBlock
import com.sk89q.worldedit.world.block.BlockStateHolder
import com.sk89q.worldedit.world.block.BlockTypes
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Path


class WorldEditHook(
    private val plugin: HMCLeaves,
    private val worldManager: WorldManager = plugin.worldManager,
    private val config: LeavesConfig = plugin.leavesConfig
) {

    private val schematicsPath: Path = this.plugin.dataFolder.toPath().resolve("schematics");

    fun load() {
        WorldEdit.getInstance().eventBus.register(this)
    }

    fun trySaveSchematic(player: Player) {
        try {
            val clipboardHolder = WorldEdit.getInstance().sessionManager.get(BukkitAdapter.adapt(player)).clipboard
            val clipboard = clipboardHolder.clipboard
            val region = clipboard.region
            val min = region.minimumPoint
            val max = region.maximumPoint
            val world = BukkitAdapter.adapt(region.world).uid
            var transformedBlocks = 0
            for (x in min.blockX..max.blockX) {
                for (y in min.blockY..max.blockY) {
                    for (z in min.blockZ..max.blockZ) {
                        val blockData: BlockData = this.worldManager[Position(world, x, y, z)] ?: continue
                        val furnaceBlock = BlockTypes.FURNACE?.defaultState?.toBaseBlock() ?: continue
                        var baseNBT = furnaceBlock.nbtData
                        if (baseNBT == null) {
                            baseNBT = CompoundTag(HashMap())
                        }
                        val bukkitTag = if (baseNBT.value[BUKKIT_NBT_TAG] is CompoundTag) {
                            baseNBT.value[BUKKIT_NBT_TAG] as CompoundTag
                        } else {
                            CompoundTag(HashMap())
                        }
                        val newBukkitValuesTag = bukkitTag.createBuilder()
                            .put(BLOCK_ID_KEY, StringTag(blockData.id))
                            .build()
                        clipboard.setBlock(
                            BlockVector3.at(x, y, z), furnaceBlock.toBaseBlock(
                                baseNBT.createBuilder()
                                    .put(BUKKIT_NBT_TAG, newBukkitValuesTag)
                                    .build()
                            )
                        )
                        transformedBlocks++
                    }
                }
            }
            player.sendMessage("${ChatColor.GREEN} Successfully saved schematic! $transformedBlocks blocks were transformed.")
        } catch (e: EmptyClipboardException) {
            player.sendMessage("${ChatColor.RED} You do not have a clipboard selected!")
        } catch (e: WorldEditException) {
            player.sendMessage("${ChatColor.RED} An error occurred while transforming the schematic!")
            e.printStackTrace()
        }
    }

    @Subscribe
    fun onEditSession(event: EditSessionEvent) {
        event.extent = object : AbstractDelegateExtent(event.extent) {
            @Throws(WorldEditException::class)
            override fun <T : BlockStateHolder<T>?> setBlock(pos: BlockVector3, block: T): Boolean {
                val position = Position(BukkitAdapter.adapt(event.world).uid, pos.x, pos.y, pos.z)
                if (block !is BaseBlock) return super.setBlock(pos, block)
                if (block.getBlockType() !== BlockTypes.FURNACE) return super.setBlock(pos, block)
                val tag = block.nbtData ?: return super.setBlock(pos, block)
                val bukkitTag = tag.value[BUKKIT_NBT_TAG] as? CompoundTag ?: return super.setBlock(pos, block)
                val id = bukkitTag.getString(BLOCK_ID_KEY) ?: return super.setBlock(pos, block)
                if (id.isBlank()) return super.setBlock(pos, block)
                val blockData = config.getBlockData(id) ?: return super.setBlock(pos, block)
                val bukkitBlockData = blockData.worldMaterial.createBlockData()
                worldManager[position] = blockData
                return extent.setBlock(pos, BukkitAdapter.adapt(bukkitBlockData))
            }
        }
    }

    fun pasteSaplingSchematic(saplingData: SaplingData, position: Position) {
        if (saplingData.schematicFiles.isEmpty()) return
        val randomSchematic: String = saplingData.schematicFiles.random()
        val file = schematicsPath.resolve(randomSchematic).toFile()
        if (!file.exists()) {
            plugin.logger.warning("Could not find sapling schematic for ${saplingData.id}: $randomSchematic," +
                    " tree growth was cancelled at $position")
            return
        }
        val format = ClipboardFormats.findByFile(file)
        if (format == null) {
            plugin.logger.warning("Could not find schematic format for $randomSchematic")
            return
        }
        try {
            format.getReader(FileInputStream(file)).use { reader ->
                val clipboard = reader.read()
                val bukkitWorld = Bukkit.getWorld(position.world)
                if (bukkitWorld == null) {
                    plugin.logger
                        .warning("Could not find world ${position.world} for $randomSchematic")
                    return
                }
                val world = BukkitAdapter.adapt(bukkitWorld)
                try {
                    WorldEdit.getInstance().newEditSession(world).use { editSession ->
                        val clipboardHolder = ClipboardHolder(clipboard)
                        if (saplingData.randomPasteRotation) {
                            clipboardHolder.setTransform(clipboardHolder.transform.combine(TRANSFORMS.random()))
                        }
                        val operation = clipboardHolder
                            .createPaste(editSession)
                            .to(BlockVector3.at(position.x, position.y, position.z))
                            .ignoreAirBlocks(true)
                            .build()
                        Operations.complete(operation)
                    }
                } catch (e: WorldEditException) {
                    e.printStackTrace()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {

        const val BLOCK_ID_KEY = "hmcleaves_block_data"
        private val BUKKIT_NBT_TAG = "PublicBukkitValues"
//        private var distanceProperty: Property<Int>? = null
//        private var persistentProperty: Property<Boolean>? = null
//        private var stageProperty: Property<Int>? = null

        private val TRANSFORMS: List<Transform> = listOf(
            Identity(),
            AffineTransform().rotateY(90.0),
            AffineTransform().rotateY(180.0),
            AffineTransform().rotateY(270.0)
        )
    }
}