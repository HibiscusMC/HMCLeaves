package com.hibiscusmc.hmcleaves.hook.item

import com.hibiscusmc.hmcleaves.HMCLeaves
import com.hibiscusmc.hmcleaves.hook.Hook
import com.hibiscusmc.hmcleaves.pdc.PDCUtil
import dev.lone.itemsadder.api.CustomStack
import io.th0rgal.oraxen.api.OraxenItems
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

abstract class ItemHook(val id: String) : Hook {

    override fun id() = this.id

    override fun isEnabled(): Boolean {
        return Bukkit.getPluginManager().getPlugin(this.id) != null
    }

    abstract fun getItemById(itemId: String, hookItemId: String): ItemStack?

}

class OraxenHook : ItemHook("Oraxen") {

    override fun getItemById(itemId: String, hookItemId: String): ItemStack? {
        val item = OraxenItems.getItemById(hookItemId)?.build() ?: return null
        PDCUtil.setItemId(item, itemId)
        return item
    }

}

class ItemsAdderHook : ItemHook("ItemsAdder"), Listener {

    override fun getItemById(itemId: String, hookItemId: String): ItemStack? {
        val item = CustomStack.getInstance(hookItemId)?.itemStack ?: return null
        PDCUtil.setItemId(item, itemId)
        return item
    }

}