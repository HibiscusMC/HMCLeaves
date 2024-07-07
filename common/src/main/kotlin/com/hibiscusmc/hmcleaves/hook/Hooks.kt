package com.hibiscusmc.hmcleaves.hook

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.block.SaplingData
import com.hibiscusmc.hmcleaves.hook.item.ItemHook
import com.hibiscusmc.hmcleaves.hook.item.ItemsAdderHook
import com.hibiscusmc.hmcleaves.hook.item.OraxenHook
import com.hibiscusmc.hmcleaves.hook.worldedit.WorldEditHook
import com.hibiscusmc.hmcleaves.world.Position
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File


class Hooks {

    companion object {

        private lateinit var plugin: HMCLeaves

        private var initialized = false

        private var itemHook: ItemHook? = null
        private var worldEditHook: WorldEditHook? = null

        fun init() {
            plugin = JavaPlugin.getPlugin(HMCLeaves::class.java)
            if (initialized) return
            this.itemHook = this.createItemHook()
            itemHook?.let {
                it.load()
                plugin.server.pluginManager.registerEvents(it, plugin)
            }
            if (plugin.server.pluginManager.getPlugin("WorldEdit") != null) {
                plugin.logger.info("World edit found")
                worldEditHook = WorldEditHook(plugin)
                worldEditHook?.load()
            } else {
                plugin.logger.info("World edit not found")
            }
        }

        private fun createItemHook(): ItemHook? {
            val pluginManager = plugin.server.pluginManager
            if (pluginManager.getPlugin("Oraxen") != null) {
                return createOraxenHook(plugin)

            }
            if (pluginManager.getPlugin("ItemsAdder") != null) {
                return createItemsAdderHook(plugin)
            }
            return null
        }

        fun unload() {
            initialized = false
            itemHook = null
            init()
        }

        fun reload() {
            unload()
            init()
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

        fun transferTextures(file: File) {
            itemHook?.transferTextures(file)
        }

        fun trySaveSchematic(player: Player) {
            if (worldEditHook == null) return
            worldEditHook?.trySaveSchematic(player)
        }

        fun pasteSaplingSchematic(saplingData: SaplingData, position: Position) {
            if (worldEditHook == null) return
            worldEditHook?.pasteSaplingSchematic(saplingData, position)
        }

    }

}