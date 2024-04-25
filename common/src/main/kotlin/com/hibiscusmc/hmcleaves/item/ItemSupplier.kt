package com.hibiscusmc.hmcleaves.item

import com.hibiscusmc.hmcleaves.block.BlockData
import com.hibiscusmc.hmcleaves.pdc.PDCUtil
import org.bukkit.inventory.ItemStack

interface ItemSupplier {

    fun createItem(): ItemStack?

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
