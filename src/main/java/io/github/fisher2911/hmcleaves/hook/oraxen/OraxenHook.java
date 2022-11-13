package io.github.fisher2911.hmcleaves.hook.oraxen;

import io.github.fisher2911.hmcleaves.hook.ItemHook;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class OraxenHook implements ItemHook {

    @Override
    @Nullable
    public String getId(ItemStack itemStack) {
        return OraxenItems.getIdByItem(itemStack);
    }

    @Override
    public @Nullable ItemStack getItem(String id) {
        return OraxenItems.getItemById(id).build();
    }

}
