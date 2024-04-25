package com.hibiscusmc.hmcleaves.item

import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.config.LeavesConfig
import org.bukkit.Bukkit
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack

interface BlockDrops {

    fun replaceItems(config: LeavesConfig, data: BlockData, items: MutableList<Item>)

    fun getDrops(config: LeavesConfig, data: BlockData): List<ItemStack>

}

class SingleBlockDropReplacement : BlockDrops {

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

}