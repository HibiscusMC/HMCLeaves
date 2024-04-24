package com.hibiscusmc.hmcleaves.pdc

import com.hibiscusmc.hmcleaves.HMCLeaves
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin


object PDCUtil {

    private val PLUGIN: HMCLeaves = JavaPlugin.getPlugin<HMCLeaves>(HMCLeaves::class.java)
    private val ITEM_ID_KEY = NamespacedKey(PLUGIN, "item_id")


    fun getItemId(itemStack: ItemStack): String? {
        val itemMeta: ItemMeta = itemStack.itemMeta ?: return null
        return itemMeta.persistentDataContainer.get(ITEM_ID_KEY, PersistentDataType.STRING)
    }

    fun setItemId(itemStack: ItemStack, itemId: String) {
        val itemMeta: ItemMeta = itemStack.itemMeta ?: return
        itemMeta.persistentDataContainer.set(ITEM_ID_KEY, PersistentDataType.STRING, itemId)
        itemStack.setItemMeta(itemMeta)
    }
}