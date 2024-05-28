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

    abstract fun getIdByItemStack(itemStack: ItemStack): String?

}

class OraxenHook : ItemHook("Oraxen") {

    override fun getItemById(itemId: String, hookItemId: String): ItemStack? {
        return OraxenItems.getItemById(hookItemId)?.build() ?: return null
    }

    override fun getIdByItemStack(itemStack: ItemStack): String? {
        return OraxenItems.getIdByItem(itemStack)
    }
}

class ItemsAdderHook : ItemHook("ItemsAdder"), Listener {

    override fun getItemById(itemId: String, hookItemId: String): ItemStack? {
        return CustomStack.getInstance(hookItemId)?.itemStack ?: return null
    }

    override fun getIdByItemStack(itemStack: ItemStack): String? {
        return CustomStack.byItemStack(itemStack)?.id
    }

}