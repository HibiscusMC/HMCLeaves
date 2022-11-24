package io.github.fisher2911.hmcleaves.hook;

import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface ItemHook extends Listener {

    @Nullable
    String getId(ItemStack itemStack);

    @Nullable
    ItemStack getItem(String id);

}
