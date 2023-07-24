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

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.CaveVines;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.Sapling;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

public class LeafDropListener implements Listener {

    private final HMCLeaves plugin;
    private final LeavesConfig leavesConfig;

    public LeafDropListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.leavesConfig = this.plugin.getLeavesConfig();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onItemDrop(BlockDropItemEvent event) {
        final Block block = event.getBlock();
        if (!this.leavesConfig.isWorldWhitelisted(block.getWorld())) return;
        final Position position = Position.fromLocation(block.getLocation());
        final BlockData data = this.plugin.getBlockCache().removeFromDropPositions(position);
        if (data == BlockData.EMPTY) return;
        final String id = data.id();
        for (Item item : event.getItems()) {
            final ItemStack itemStack = item.getItemStack();
            if (Tag.LEAVES.isTagged(itemStack.getType())) {
                final Supplier<ItemStack> dropReplacementSupplier = this.leavesConfig.getLeafDropReplacement(id);
                if (dropReplacementSupplier == null) {
                    continue;
                }
                final ItemStack dropReplacement = dropReplacementSupplier.get();
                if (dropReplacement == null) {
                    continue;
                }
                this.transferItemData(itemStack, dropReplacement);
                continue;
            }
            if (Tag.SAPLINGS.isTagged(itemStack.getType())) {
                final Supplier<ItemStack> saplingSupplier = this.leavesConfig.getSapling(id);
                if (saplingSupplier == null) continue;
                final ItemStack sapling = saplingSupplier.get();
                if (sapling == null) continue;
                this.transferItemData(itemStack, sapling);
                continue;
            }
            if (itemStack.getType() == data.worldBlockType()) {
                final ItemStack replacementItem = this.leavesConfig.getItemStack(id);
                if (replacementItem == null) return;
                this.transferItemData(item.getItemStack(), replacementItem);
                continue;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onItemDrop(ItemSpawnEvent event) {
        final Block block = event.getLocation().getBlock();
        if (!this.leavesConfig.isWorldWhitelisted(block.getWorld())) return;
        final Position position = Position.fromLocation(block.getLocation());
        final Item item = event.getEntity();
        if (block.getBlockData() instanceof Sapling) {
            if (!Tag.SAPLINGS.isTagged(item.getItemStack().getType())) return;
            final BlockData blockData = this.plugin.getBlockCache().removeFromDropPositions(position);
            final Supplier<ItemStack> supplier = this.leavesConfig.getItemSupplier(blockData.id());
            if (supplier == null) return;
            final ItemStack saplingItem = supplier.get();
            if (saplingItem == null) return;
            this.transferItemData(item.getItemStack(), saplingItem);
            return;
        }
        if ((block.getBlockData() instanceof CaveVinesPlant || block.getBlockData() instanceof CaveVines) && item.getItemStack().getType() != Material.GLOW_BERRIES) {
            return;
        }
        final BlockData blockData = this.plugin.getBlockCache().removeFromDropPositions(position);
        if (blockData == BlockData.EMPTY) return;
        final String id = blockData.id();
        if (!(block.getBlockData() instanceof Leaves)) {
            final ItemStack itemStack = this.leavesConfig.getItemStack(id);
            if (itemStack == null) return;
            this.transferItemData(item.getItemStack(), itemStack);
            return;
        }
        final ItemStack itemStack = item.getItemStack();
        if (Tag.LEAVES.isTagged(itemStack.getType())) {
            final Supplier<ItemStack> dropReplacementSupplier = this.leavesConfig.getLeafDropReplacement(id);
            if (dropReplacementSupplier == null) return;
            final ItemStack dropReplacement = dropReplacementSupplier.get();
            if (dropReplacement == null) return;
            this.transferItemData(itemStack, dropReplacement);
            return;
        }
        if (Tag.SAPLINGS.isTagged(itemStack.getType())) {
            final Supplier<ItemStack> saplingSupplier = this.leavesConfig.getSapling(id);
            if (saplingSupplier == null) return;
            final ItemStack sapling = saplingSupplier.get();
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
