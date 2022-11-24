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

//    @EventHandler(priority = EventPriority.LOW)
//    public void onItemDrop(BlockDropItemEvent event) {
//        final Block block = event.getBlock();
//        final Location location = block.getLocation();
//        final Position2D chunkPos = new Position2D(location.getWorld().getUID(), location.getChunk().getX(), location.getChunk().getZ());
//        final Position position = new Position(ChunkUtil.getCoordInChunk(location.getBlockX()), location.getBlockY(), ChunkUtil.getCoordInChunk(location.getBlockZ()));
//        final var state = this.plugin.getLeafCache().getAt(chunkPos, position);
//        if (state == null) return;
//        final LeafItem leafItem = this.config.getByState(state);
//        if (leafItem == null) return;
//        for (Item item : event.getItems()) {
//            final ItemStack itemStack = item.getItemStack();
//            if (Tag.LEAVES.isTagged(itemStack.getType())) {
//                final ItemStack dropReplacement = this.config.getLeafDropReplacement(leafItem.id());
//                if (dropReplacement == null) continue;
//                this.transferItemData(itemStack, dropReplacement);
//                continue;
//            }
//            if (Tag.SAPLINGS.isTagged(itemStack.getType())) {
//                final ItemStack sapling = this.config.getSapling(leafItem.id());
//                if (sapling == null) continue;
//                this.transferItemData(itemStack, sapling);
//            }
//        }
//    }
//
//    private void transferItemData(ItemStack original, ItemStack toTransfer) {
//        original.setType(toTransfer.getType());
//        original.setAmount(toTransfer.getAmount());
//        original.setItemMeta(toTransfer.getItemMeta());
//    }

//    private final HMCLeaves plugin;
//    private final Config config;
//
//    public LeafDropListener(HMCLeaves plugin) {
//        this.plugin = plugin;
//        this.config = this.plugin.config();
//    }

    @EventHandler(priority = EventPriority.LOW)
    public void onItemDrop(BlockDropItemEvent event) {
        final Block block = event.getBlock();
        final FakeLeafState data = this.plugin.getLeafCache().getAt(block.getLocation());
        if (data == null) return;
        final LeafItem leafItem = this.config.getByState(
                data
//                data.fakeDistance(),
//                data.fakePersistence(),
//                data.actualPersistence()
        );
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
        final LeafItem leafItem = this.config.getByState(
//                leaves.getDistance(),
//                leaves.isPersistent(),
//                false
                data
        );
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
