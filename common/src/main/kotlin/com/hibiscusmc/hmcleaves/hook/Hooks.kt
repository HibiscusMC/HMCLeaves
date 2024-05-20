package com.hibiscusmc.hmcleaves.hook

import com.hibiscusmc.hmcleaves.hook.item.ItemHook
import com.hibiscusmc.hmcleaves.hook.item.ItemsAdderHook
import com.hibiscusmc.hmcleaves.hook.item.OraxenHook
import org.bukkit.inventory.ItemStack

class Hooks {

    companion object {

        private var initialized = false

        private var itemHook: ItemHook? = null

        fun init() {
            if (initialized) return
            try {
                itemHook = OraxenHook()
            } catch (_: Exception) {
            }
            if (itemHook == null) {
                try {
                    itemHook = ItemsAdderHook()
                } catch (_: Exception) {

                }
            }
        }

        fun getItemStackById(itemId: String, hookItemId: String): ItemStack? {
            return itemHook?.getItemById(itemId, hookItemId)
        }

    }

}