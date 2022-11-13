package io.github.fisher2911.hmcleaves.hook;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface ItemHook {

    @Nullable
    String getId(ItemStack itemStack);

}
