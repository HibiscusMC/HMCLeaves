/*
 *
 *  *     HMCLeaves
 *  *     Copyright (C) 2022  Hibiscus Creative Studios
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU General Public License
 *  *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.github.fisher2911.hmcleaves.hook.itemsadder;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent;
import dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.hook.ItemHook;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class ItemsAdderHook implements ItemHook {

    private final HMCLeaves plugin;
    private final LeavesConfig config;

    public ItemsAdderHook(HMCLeaves plugin) {
        this.plugin = plugin;
        this.config = plugin.getLeavesConfig();
    }

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

    @Override
    @Nullable
    public Integer getBlockId(String id) {
        final CustomBlock block = CustomBlock.getInstance(id);
        if (block == null) return null;
        return SpigotConversionUtil.fromBukkitBlockData(block.getBaseBlockData()).getGlobalId();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onNoteblockPlace(CustomBlockPlaceEvent event) {
        final String id = event.getNamespacedID();
        if (this.config.getItemSupplier(id) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onNoteblockRemove(CustomBlockBreakEvent event) {
        final String id = event.getNamespacedID();
        if (this.config.getItemSupplier(id) != null) {
            event.setCancelled(true);
        }
    }

    @Override
    public void transferTextures(File file) {
        // todo: figure out path
    }

    @Override
    public @Nullable String getCustomBlockIdAt(Location location) {
        final CustomBlock block = CustomBlock.byAlreadyPlaced(location.getBlock());
        if (block == null) return null;
        return block.getId();
    }

}