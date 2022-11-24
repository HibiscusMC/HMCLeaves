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
