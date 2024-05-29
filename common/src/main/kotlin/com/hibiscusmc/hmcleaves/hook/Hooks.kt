package com.hibiscusmc.hmcleaves.hook

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.hook.item.ItemHook
import com.hibiscusmc.hmcleaves.hook.item.ItemsAdderHook
import com.hibiscusmc.hmcleaves.hook.item.OraxenHook
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class Hooks {

    companion object {

        private lateinit var plugin: HMCLeaves

        private var initialized = false

        private var itemHook: ItemHook? = null

        fun init() {
            plugin = JavaPlugin.getPlugin(HMCLeaves::class.java)
            if (initialized) return
            try {
                itemHook = createOraxenHook(plugin)
            } catch (_: Exception) {
            }
            itemHook = if (itemHook != null) {
                itemHook
            } else {
                try {
                    ItemsAdderHook(plugin)
                } catch (_: Exception) {
                    null
                }
            }
            itemHook?.let {
                it.load()
                plugin.server.pluginManager.registerEvents(it, plugin)
            }
        }

        private fun createOraxenHook(plugin: HMCLeaves): ItemHook {
            return OraxenHook(plugin)
        }

        private fun createItemsAdderHook(plugin: HMCLeaves): ItemHook {
            return ItemsAdderHook(plugin)
        }

        fun getItemStackById(itemId: String, hookItemId: String): ItemStack? {
            return itemHook?.getItemById(itemId, hookItemId)
        }

        fun getIdByItemStack(itemStack: ItemStack): String? {
            return itemHook?.getIdByItemStack(itemStack)
        }

        fun isHandledByHook(block: Block): Boolean {
            val hookId = itemHook?.getIDFromBlock(block) ?: return false
            val blockDataId = plugin.leavesConfig.getBlockDataIdFromHookId(hookId) ?: return false
            val blockData = plugin.leavesConfig.getBlockData(blockDataId) ?: return false
            return blockData.worldMaterial == Material.NOTE_BLOCK
        }

        fun isHandledByHook(itemStack: ItemStack): Boolean {
            val hookId = getIdByItemStack(itemStack) ?: return false
            val blockDataId = plugin.leavesConfig.getBlockDataIdFromHookId(hookId) ?: return false
            val blockData = plugin.leavesConfig.getBlockData(blockDataId) ?: return false
            return blockData.worldMaterial == Material.NOTE_BLOCK
        }

    }

}