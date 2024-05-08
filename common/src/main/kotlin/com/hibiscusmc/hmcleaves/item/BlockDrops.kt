package com.hibiscusmc.hmcleaves.item

import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack

interface BlockDrops {

    fun replaceItems(config: LeavesConfig, data: BlockData, items: MutableList<Item>)

    fun replaceItemStacks(config: LeavesConfig, data: BlockData, items: MutableList<ItemStack>)

    fun getDrops(config: LeavesConfig, data: BlockData): List<ItemStack>

}

enum class BlockDropType {

    MAPPED_REPLACEMENT

}

open class SingleBlockDropReplacement : BlockDrops {

    override fun replaceItems(config: LeavesConfig, data: BlockData, items: MutableList<Item>) {
        items.forEach { item ->
            val itemStack = item.itemStack
            if (data.worldMaterial != itemStack.type) return@forEach
            val newItem = data.createItem() ?: return@forEach
            newItem.amount = itemStack.amount
            item.itemStack = newItem
        }
    }

    override fun getDrops(config: LeavesConfig, data: BlockData): List<ItemStack> {
        return listOf(data.createItem() ?: return listOf())
    }

    override fun replaceItemStacks(config: LeavesConfig, data: BlockData, items: MutableList<ItemStack>) {
        items.replaceAll { itemStack ->
            if (data.worldMaterial != itemStack.type) return@replaceAll itemStack
            val newItem = data.createItem() ?: return@replaceAll itemStack
            newItem.amount = itemStack.amount
            return@replaceAll newItem
        }
    }

}

class LogDropReplacement : BlockDrops {

    override fun replaceItems(config: LeavesConfig, data: BlockData, items: MutableList<Item>) {
        items.forEach { item ->
            val itemStack = item.itemStack
            if (data.worldMaterial != itemStack.type) {
                return@forEach
            }
            val newData = config.getNonDirectionalBlockData(data) ?: return@forEach
            val newItem = newData.createItem() ?: itemStack
            newItem.amount = itemStack.amount
            item.itemStack = newItem
            return@forEach
        }
    }

    override fun getDrops(config: LeavesConfig, data: BlockData): List<ItemStack> {
        val newData = config.getNonDirectionalBlockData(data) ?: return listOf()
        return listOf(newData.createItem() ?: return listOf())
    }

    override fun replaceItemStacks(config: LeavesConfig, data: BlockData, items: MutableList<ItemStack>) {
        items.replaceAll { itemStack ->
            if (data.worldMaterial != itemStack.type) {
                return@replaceAll itemStack
            }
            val newData = config.getNonDirectionalBlockData(data) ?: return@replaceAll itemStack
            val newItem = newData.createItem() ?: itemStack
            newItem.amount = itemStack.amount
            return@replaceAll newItem
        }
    }

}

class MappedBlockDropReplacements(private val replacements: Map<Material, ItemSupplier>) :
    SingleBlockDropReplacement() {

    override fun replaceItems(config: LeavesConfig, data: BlockData, items: MutableList<Item>) {
        super.replaceItems(config, data, items)
        items.forEach { item ->
            val itemStack = item.itemStack
            val newItem = this.replacements[itemStack.type]?.createItem() ?: return@forEach
            newItem.amount = itemStack.amount
            item.itemStack = newItem
        }
    }

    override fun replaceItemStacks(config: LeavesConfig, data: BlockData, items: MutableList<ItemStack>) {
        super.replaceItemStacks(config, data, items)
        items.replaceAll { itemStack ->
            val newItem = this.replacements[itemStack.type]?.createItem() ?: return@replaceAll itemStack
            newItem.amount = itemStack.amount
            return@replaceAll newItem
        }
    }

}