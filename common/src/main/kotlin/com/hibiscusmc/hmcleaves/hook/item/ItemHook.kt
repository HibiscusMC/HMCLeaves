package com.hibiscusmc.hmcleaves.hook.item

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.hook.Hook
import com.hibiscusmc.hmcleaves.util.getChunkPosition
import com.hibiscusmc.hmcleaves.util.toPositionInChunk
import dev.lone.itemsadder.api.CustomBlock
import dev.lone.itemsadder.api.CustomStack
import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.th0rgal.oraxen.OraxenPlugin
import io.th0rgal.oraxen.api.OraxenBlocks
import io.th0rgal.oraxen.api.OraxenItems
import io.th0rgal.oraxen.api.OraxenPack
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockBreakEvent
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockPlaceEvent
import io.th0rgal.oraxen.items.ItemUpdater
import io.th0rgal.oraxen.utils.VirtualFile
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.name


abstract class ItemHook(val id: String) : Hook, Listener {

    override fun id() = this.id

    override fun isEnabled(): Boolean {
        return Bukkit.getPluginManager().getPlugin(this.id) != null
    }

    abstract fun getItemById(itemId: String, hookItemId: String): ItemStack?

    abstract fun getIdByItemStack(itemStack: ItemStack): String?

    abstract fun isHookBlock(location: Location): Boolean

    abstract fun getIDFromBlock(block: Block): String?

    abstract fun load()

    abstract fun reloadTextures()

}

class OraxenHook(private val plugin: HMCLeaves) : ItemHook("Oraxen") {

    private val texturesPath: Path = OraxenPlugin.get().dataFolder
        .toPath()
        .resolve("pack")
        .resolve("assets")
        .resolve("minecraft")
        .resolve("blockstates");

    private val modelsPath: Path = Path.of("assets")
        .resolve("minecraft")
        .resolve("models");


    override fun getItemById(itemId: String, hookItemId: String): ItemStack? {
        return ItemUpdater.updateItem(OraxenItems.getItemById(hookItemId)?.build() ?: return null)
    }

    override fun getIdByItemStack(itemStack: ItemStack): String? {
        return OraxenItems.getIdByItem(itemStack)
    }

    override fun isHookBlock(location: Location): Boolean {
        return OraxenBlocks.getOraxenBlock(location) != null
    }

    override fun getIDFromBlock(block: Block): String? {
        return OraxenBlocks.getOraxenBlock(block.location)?.itemID
    }

    @EventHandler
    private fun onItemsLoad(event: OraxenItemsLoadedEvent) {
        this.load()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onOraxenBlockPlace(event: OraxenNoteBlockPlaceEvent) {
        val id = event.mechanic.itemID ?: return
        val block = event.block
        val location = block.location
        val config = this.plugin.leavesConfig
        val data = config.getBlockData(config.getBlockDataIdFromHookId(id) ?: return) ?: return
        val worldUUID = location.world?.uid ?: return
        this.plugin.worldManager.getOrAdd(worldUUID).getOrAdd(block.getChunkPosition())[location.toPositionInChunk()
            ?: return] = data
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onOraxenBlockBreak(event: OraxenNoteBlockBreakEvent) {
        val block = event.block
        val location = block.location
        val worldUUID = location.world?.uid ?: return
        this.plugin.worldManager.get(worldUUID)?.get(block.getChunkPosition())
            ?.remove(location.toPositionInChunk() ?: return, false)
    }

    @EventHandler
    private fun onOraxenPackGenerate(event: OraxenPackGeneratedEvent) {
        val files = this.plugin.leavesConfig.loadTextures().associateBy { it.name }
        for (virtualFile in event.output) {
            val filePath = Path.of(virtualFile.path)
            val newFile = files[filePath.name] ?: continue
            this.combineFiles(virtualFile, newFile)
        }

        event.output.addAll(
            this.createModelFiles(
                files.filter { it.value.path.contains("models") }
                    .map { it.value }
                    .toSet()
            ).toTypedArray()
        )
    }

    fun combineFiles(destination: VirtualFile, transferFile: File) {
        val transferJson = JsonParser.parseReader(transferFile.bufferedReader()).asJsonObject
        val destinationJson = destination.toJsonObject() ?: return

        val newJson = JsonObject()
        val variantsObject = JsonObject()

        val transferVariants = transferJson.get("variants").asJsonObject
        transferVariants.keySet().forEach { key ->
            variantsObject.add(key, transferVariants.get(key).deepCopy())
        }
        val destinationVariants = destinationJson.get("variants").asJsonObject
        destinationVariants.keySet().forEach { key ->
            variantsObject.add(key, destinationVariants.get(key).deepCopy())
        }
        newJson.add("variants", variantsObject)

        val gson = GsonBuilder()
            .disableHtmlEscaping()
            .create()

        val stringJson = gson.toJson(newJson)
        val inputStream = ByteArrayInputStream(stringJson.toByteArray(StandardCharsets.UTF_8))
        destination.inputStream = inputStream
    }

    override fun load() {
        val config = this.plugin.leavesConfig
        val items = config.getHookIdsToBlockIds()
        val logger = this.plugin.getLeavesLogger()
        for (entry in items.entries) {
            val hookId = entry.key
            val blockDataId = entry.value
            val blockData = config.getBlockData(blockDataId)
            if (blockData == null) {
                logger.warn("Could not load Oraxen hook item because BlockData $blockDataId was not found")
                continue
            }
            val oraxenBlock = OraxenBlocks.getOraxenBlockData(hookId)
            if (oraxenBlock == null) {
                logger.warn("Could not load Oraxen hook item because Oraxen Block $hookId was not found")
                continue
            }
            val blockStateId = SpigotConversionUtil.fromBukkitBlockData(oraxenBlock).globalId
            blockData.setOverrideBlockId(blockStateId)
            this.plugin.getLeavesLogger()
                .info("Overriding block date state id of $blockDataId with Oraxen ${hookId}: $blockStateId ")
        }
    }

    private fun createModelFiles(files: Collection<File>): Collection<VirtualFile> {
        val virtualFiles = hashSetOf<VirtualFile>()
        for (file in files) {
            try {
                val data = Files.readString(file.toPath())
                val inputStream = ByteArrayInputStream(data.toByteArray(StandardCharsets.UTF_8))
                val virtualFile = VirtualFile(this.modelsPath.toString(), file.name, inputStream)
                virtualFiles.add(virtualFile)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return virtualFiles
    }

    override fun reloadTextures() {
        OraxenPack.reloadPack()
    }

}

class ItemsAdderHook(private val plugin: HMCLeaves) : ItemHook("ItemsAdder") {

    override fun getItemById(itemId: String, hookItemId: String): ItemStack? {
        return CustomStack.getInstance(hookItemId)?.itemStack ?: return null
    }

    override fun getIdByItemStack(itemStack: ItemStack): String? {
        return CustomStack.byItemStack(itemStack)?.id
    }

    override fun isHookBlock(location: Location): Boolean {
        return CustomBlock.byAlreadyPlaced(location.block) != null
    }

    override fun getIDFromBlock(block: Block): String? {
        return CustomBlock.byAlreadyPlaced(block)?.namespacedID
    }

    @EventHandler
    private fun onItemsLoad(event: ItemsAdderLoadDataEvent) {
        this.load()
    }

    @EventHandler
    private fun onItemsAdderBlockPlace(event: CustomBlockPlaceEvent) {
        val config = this.plugin.leavesConfig
        val data = config.getBlockData(config.getBlockDataIdFromHookId(event.namespacedID) ?: return) ?: return
        val block = event.block
        val location = block.location
        val worldUUID = location.world?.uid ?: return
        this.plugin.worldManager.getOrAdd(worldUUID).getOrAdd(block.getChunkPosition())[location.toPositionInChunk()
            ?: return] = data
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onItemsAdderBlockBreak(event: CustomBlockBreakEvent) {
        val block = event.block
        val location = block.location
        val worldUUID = location.world?.uid ?: return
        this.plugin.worldManager[worldUUID]?.get(block.getChunkPosition())
            ?.remove(location.toPositionInChunk() ?: return, false)
    }


    override fun load() {
        val config = this.plugin.leavesConfig
        val items = config.getHookIdsToBlockIds()
        val logger = this.plugin.getLeavesLogger()
        this.plugin.getLeavesLogger().info("Loaded ItemsAdder Items")
        for (entry in items.entries) {
            val hookId = entry.key
            val blockDataId = entry.value
            val blockData = config.getBlockData(blockDataId)
            if (blockData == null) {
                logger.warn("Could not load ItemsAdder hook item because BlockData $blockDataId was not found")
                continue
            }
            val itemsAdderBlock = CustomBlock.getInstance(hookId)
            if (itemsAdderBlock == null) {
                logger.warn("Could not load ItemsAdder hook item because ItemsAdder Block $hookId was not found")
                continue
            }
            val blockStateId =
                SpigotConversionUtil.fromBukkitBlockData(itemsAdderBlock.baseBlockData ?: continue).globalId
            blockData.setOverrideBlockId(blockStateId)
            plugin.getLeavesLogger()
                .info("Overriding block date state id of $blockDataId with ItemsAdder ${hookId}: $blockStateId ")
        }
    }

    override fun reloadTextures() {

    }
}