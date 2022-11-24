package io.github.fisher2911.hmcleaves.hook.itemsadder;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent;
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent;
import io.github.fisher2911.hmcleaves.hook.ItemHook;
import io.github.fisher2911.hmcleaves.util.LeafUpdater3;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onNoteblockPlace(CustomBlockPlaceEvent event) {
        final Block block = event.getBlock();
        LeafUpdater3.scheduleTick(block.getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onNoteblockRemove(CustomBlockBreakEvent event) {
        final Block block = event.getBlock();
        LeafUpdater3.scheduleTick(block.getLocation());
    }

}
