package com.hibiscusmc.hmcleaves.paper.hook.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface ItemHook {

    @Nullable ItemStack fromId(String id);

    @Nullable String getItemId(ItemStack itemStack);

    ItemHook EMPTY = new ItemHook() {
        @Override
        public @Nullable ItemStack fromId(String id) {
            return null;
        }

        @Override
        public @Nullable String getItemId(ItemStack itemStack) {
            return null;
        }
    };

}
