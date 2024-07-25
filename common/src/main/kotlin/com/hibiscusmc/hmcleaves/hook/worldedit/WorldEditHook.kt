package com.hibiscusmc.hmcleaves.hook.worldedit.regular;

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
import com.sk89q.worldedit.extent.Extent
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.math.transform.Identity
import com.sk89q.worldedit.math.transform.Transform
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.util.eventbus.Subscribe
import com.sk89q.worldedit.world.block.BlockStateHolder
import com.sk89q.worldedit.world.block.BlockTypes
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.io.FileInputStream
import java.io.IOException


class WorldEditHook(
    private val plugin: HMCLeaves,
    private val extentCreator: (event: EditSessionEvent) -> Extent,
    private val worldManager: WorldManager = plugin.worldManager,
    private val config: LeavesConfig = plugin.leavesConfig
) {

    private val schematicsPath = this.plugin.dataFolder.toPath().resolve("schematics");

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
                        val block = BukkitAdapter.adapt(clipboard.getBlock(BlockVector3.at(x, y, z)))
                        val newBukkitValuesTag = bukkitTag.createBuilder()
                            .put(BLOCK_ID_KEY, StringTag(blockData.id))
                            .put(BLOCK_DATA_STRING, StringTag(block.asString))
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
        event.extent = extentCreator(event)
    }

    fun pasteSaplingSchematic(saplingData: SaplingData, position: Position) {
        if (saplingData.schematicFiles.isEmpty()) return
        val randomSchematic: String = saplingData.schematicFiles.random()
        val file = schematicsPath.resolve(randomSchematic).toFile()
        if (!file.exists()) {
            plugin.logger.warning(
                "Could not find sapling schematic for ${saplingData.id}: $randomSchematic," +
                        " tree growth was cancelled at $position"
            )
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
        const val BUKKIT_NBT_TAG = "PublicBukkitValues"
        const val BLOCK_DATA_STRING = "hmcleaves_block_data_string"
//        private var distanceProperty: Property<Int>? = null
//        private var persistentProperty: Property<Boolean>? = null
//        private var stageProperty: Property<Int>? = null

        val TRANSFORMS: List<Transform> = listOf(
            Identity(),
            AffineTransform().rotateY(90.0),
            AffineTransform().rotateY(180.0),
            AffineTransform().rotateY(270.0)
        )
    }

}