package io.github.fisher2911.hmcleaves.hook;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ItemsAdderHook implements Listener {

    @Nullable
    public String getId(ItemStack itemStack) {
        final CustomStack stack = CustomStack.byItemStack(itemStack);
        if (stack == null) return null;
        return stack.getId();
    }


}
