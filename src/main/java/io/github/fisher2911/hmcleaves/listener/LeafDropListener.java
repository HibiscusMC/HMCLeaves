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

package io.github.fisher2911.hmcleaves.listener;

import io.github.fisher2911.hmcleaves.Config;
import io.github.fisher2911.hmcleaves.FakeLeafState;
import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.LeafItem;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

public class LeafDropListener implements Listener {

    private final HMCLeaves plugin;
    private final Config config;

    public LeafDropListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.config = this.plugin.config();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onItemDrop(BlockDropItemEvent event) {
        final Block block = event.getBlock();
        final FakeLeafState data = this.plugin.getLeafCache().getAt(block.getLocation());
        if (data == null) return;
        final LeafItem leafItem = this.config.getByState(data);
        if (leafItem == null) return;
        for (Item item : event.getItems()) {
            final ItemStack itemStack = item.getItemStack();
            if (Tag.LEAVES.isTagged(itemStack.getType())) {
                final ItemStack dropReplacement = this.config.getLeafDropReplacement(leafItem.id());
                if (dropReplacement == null) continue;
                this.transferItemData(itemStack, dropReplacement);
                continue;
            }
            if (Tag.SAPLINGS.isTagged(itemStack.getType())) {
                final ItemStack sapling = this.config.getSapling(leafItem.id());
                if (sapling == null) continue;
                this.transferItemData(itemStack, sapling);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onItemDrop(ItemSpawnEvent event) {
        final Block block = event.getLocation().getBlock();
        if (!(block.getBlockData() instanceof final Leaves leaves)) return;
        final FakeLeafState data = this.plugin.getLeafCache().getAt(block.getLocation());
        final LeafItem leafItem = this.config.getByState(data);
        if (leafItem == null) return;
        final Item item = event.getEntity();
        final ItemStack itemStack = item.getItemStack();
        if (Tag.LEAVES.isTagged(itemStack.getType())) {
            final ItemStack dropReplacement = this.config.getLeafDropReplacement(leafItem.id());
            if (dropReplacement == null) return;
            this.transferItemData(itemStack, dropReplacement);
            return;
        }
        if (Tag.SAPLINGS.isTagged(itemStack.getType())) {
            final ItemStack sapling = this.config.getSapling(leafItem.id());
            if (sapling == null) return;
            this.transferItemData(itemStack, sapling);
        }
    }

    private void transferItemData(ItemStack original, ItemStack toTransfer) {
        original.setType(toTransfer.getType());
        original.setAmount(toTransfer.getAmount());
        original.setItemMeta(toTransfer.getItemMeta());
    }

}
