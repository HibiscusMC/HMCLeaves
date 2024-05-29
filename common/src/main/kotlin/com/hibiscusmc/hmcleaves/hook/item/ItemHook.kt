package com.hibiscusmc.hmcleaves.hook.item

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.hook.Hook
import com.hibiscusmc.hmcleaves.util.getChunkPosition
import com.hibiscusmc.hmcleaves.util.toPosition
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
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockBreakEvent
import io.th0rgal.oraxen.api.events.noteblock.OraxenNoteBlockPlaceEvent
import io.th0rgal.oraxen.items.ItemUpdater
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import kotlin.math.log

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

}

class OraxenHook(private val plugin: HMCLeaves) : ItemHook("Oraxen") {

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

    override fun load() {
        val config = this.plugin.leavesConfig
        val items = config.getHookIdsToBlockIds()
        val logger = this.plugin.getLeavesLogger()
        logger.info("Loaded Oraxen Items")
        logger.info("Loading ${items.entries.size} hook items")
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

}