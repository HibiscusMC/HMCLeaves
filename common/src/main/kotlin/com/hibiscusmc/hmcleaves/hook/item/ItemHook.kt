package com.hibiscusmc.hmcleaves.hook.item

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.hook.Hook
import dev.lone.itemsadder.api.CustomBlock
import dev.lone.itemsadder.api.CustomStack
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.th0rgal.oraxen.api.OraxenBlocks
import io.th0rgal.oraxen.api.OraxenItems
import io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

abstract class ItemHook(val id: String) : Hook, Listener {

    override fun id() = this.id

    override fun isEnabled(): Boolean {
        return Bukkit.getPluginManager().getPlugin(this.id) != null
    }

    abstract fun getItemById(itemId: String, hookItemId: String): ItemStack?

    abstract fun getIdByItemStack(itemStack: ItemStack): String?

}

class OraxenHook(private val plugin: HMCLeaves) : ItemHook("Oraxen") {

    override fun getItemById(itemId: String, hookItemId: String): ItemStack? {
        return OraxenItems.getItemById(hookItemId)?.build() ?: return null
    }

    override fun getIdByItemStack(itemStack: ItemStack): String? {
        return OraxenItems.getIdByItem(itemStack)
    }

    @EventHandler
    fun onItemsLoad(event: OraxenItemsLoadedEvent) {
        val config = this.plugin.leavesConfig
        val items = config.getHookIdsToBlockIds()
        for (entry in items.entries) {
            val hookId = entry.key
            val blockDataId = entry.value
            val blockData = config.getBlockData(blockDataId) ?: continue
            val oraxenBlock = OraxenBlocks.getOraxenBlockData(hookId) ?: continue
            val blockStateId = SpigotConversionUtil.fromBukkitBlockData(oraxenBlock).globalId
            blockData.setOverrideBlockId(blockStateId)
            plugin.getLeavesLogger().info("Overriding block date state id of $blockDataId with Oraxen ${hookId}: $blockStateId ")
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

    @EventHandler
    fun onItemsLoad(event: ItemsAdderLoadDataEvent) {
        val config = this.plugin.leavesConfig
        val items = config.getHookIdsToBlockIds()
        for (entry in items.entries) {
            val hookId = entry.key
            val blockDataId = entry.value
            val blockData = config.getBlockData(blockDataId) ?: continue
            val itemsAdderBlock = CustomBlock.getInstance(hookId) ?: continue
            val blockStateId = SpigotConversionUtil.fromBukkitBlockData(itemsAdderBlock.baseBlockData ?: continue).globalId
            blockData.setOverrideBlockId(blockStateId)
            plugin.getLeavesLogger().info("Overriding block date state id of $blockDataId with ItemsAdder ${hookId}: $blockStateId ")
        }
    }

}