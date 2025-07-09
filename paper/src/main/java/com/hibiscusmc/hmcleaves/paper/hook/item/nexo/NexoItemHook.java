package com.hibiscusmc.hmcleaves.paper.hook.item.nexo;

import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.hook.item.ItemHook;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.api.events.custom_block.NexoBlockPlaceEvent;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class NexoItemHook implements ItemHook, Listener {

    private final HMCLeaves plugin;

    public NexoItemHook(HMCLeaves plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable ItemStack fromId(String id) {
        final ItemBuilder itemBuilder = NexoItems.itemFromId(id);
        if (itemBuilder == null) {
            return null;
        }
        return itemBuilder.build();
    }

    @Override
    public @Nullable String getItemId(ItemStack itemStack) {
        return NexoItems.idFromItem(itemStack);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onNexoPlace(NexoBlockPlaceEvent event) {
        if (this.plugin.leavesConfig().isItemId(event.getMechanic().getItemID())) {
            event.setCancelled(true);
        }
    }

}
