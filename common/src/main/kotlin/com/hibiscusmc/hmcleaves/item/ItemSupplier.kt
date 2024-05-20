package com.hibiscusmc.hmcleaves.item

import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.hook.Hooks
import com.hibiscusmc.hmcleaves.pdc.PDCUtil
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

interface ItemSupplier {

    fun createItem(): ItemStack?

}

data object EmptyItemSupplier: ItemSupplier {

    private val air = ItemStack(Material.AIR)

    override fun createItem() = this.air
}

class ConstantItemSupplier(
    private val itemStack: ItemStack,
    private val blockDataId: String
) : ItemSupplier {

    init {
        PDCUtil.setItemId(this.itemStack, this.blockDataId)
    }

    override fun createItem(): ItemStack {
        return this.itemStack.clone()
    }
}

class HookItemSupplier(
    private val itemId: String,
    private val hookItemId: String,
    private val defaultSupplier: ItemSupplier = EmptyItemSupplier
) : ItemSupplier {

    override fun createItem(): ItemStack? {
        return Hooks.getItemStackById(this.itemId, this.hookItemId) ?: this.defaultSupplier.createItem()
    }
}
