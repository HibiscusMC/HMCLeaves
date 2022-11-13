package io.github.fisher2911.hmcleaves.hook.itemsadder;

import dev.lone.itemsadder.api.CustomStack;
import io.github.fisher2911.hmcleaves.hook.ItemHook;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ItemsAdderHook implements ItemHook {

    @Override
    @Nullable
    public String getId(ItemStack itemStack) {
        final CustomStack stack = CustomStack.byItemStack(itemStack);
        if (stack == null) return null;
        return stack.getId();
    }

    @Override
    public @Nullable ItemStack getItem(String id) {
        final CustomStack stack = CustomStack.getInstance(id);
        if (stack == null) return null;
        return stack.getItemStack();
    }

}
