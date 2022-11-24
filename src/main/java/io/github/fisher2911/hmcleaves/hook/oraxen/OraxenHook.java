package io.github.fisher2911.hmcleaves.hook.oraxen;

import io.github.fisher2911.hmcleaves.hook.ItemHook;
import io.github.fisher2911.hmcleaves.util.LeafUpdater3;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.api.events.OraxenNoteBlockBreakEvent;
import io.th0rgal.oraxen.api.events.OraxenNoteBlockPlaceEvent;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onNoteblockPlace(OraxenNoteBlockPlaceEvent event) {
        final Block block = event.getBlock();
        LeafUpdater3.scheduleTick(block.getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onNoteblockRemove(OraxenNoteBlockBreakEvent event) {
        final Block block = event.getBlock();
        LeafUpdater3.scheduleTick(block.getLocation());
    }

}
