package com.hibiscusmc.hmcleaves.paper.listener;

import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlockState;
import com.hibiscusmc.hmcleaves.paper.config.BlockDropConfig;
import com.hibiscusmc.hmcleaves.paper.util.WorldUtil;
import com.hibiscusmc.hmcleaves.paper.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.paper.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.paper.world.Position;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

public final class BlockDropItemListener extends CustomBlockListener {

    public BlockDropItemListener(HMCLeaves plugin) {
        super(plugin);
    }

    @EventHandler
    public void onBlockDropItemsEvent(ItemSpawnEvent event) {
        final Location location = event.getLocation();
        final ItemStack itemStack = event.getEntity().getItemStack();
        final World world = location.getWorld();
        final LeavesWorld leavesWorld = this.worldManager.getWorld(world.getUID());
        if (leavesWorld == null) {
            return;
        }
        final Position position = WorldUtil.convertLocation(location);
        final ChunkPosition chunkPosition = position.toChunkPosition();
        final LeavesChunk chunk = leavesWorld.getChunk(chunkPosition);
        if (chunk == null) {
            return;
        }
        final CustomBlockState customBlockState = chunk.getBlock(position);
        if (customBlockState == null) {
            return;
        }
        final BlockDropConfig drops = this.config.getBlockDrops(customBlockState.customBlock().id());
        if (drops == null) {
            return;
        }
        if (Tag.LOGS.isTagged(itemStack.getType())) {
            final ItemStack created = this.config.createItemStack(customBlockState.customBlock().id());
            if (created != null) {
                event.getEntity().setItemStack(created);
            }
            return;
        }
        if (!Tag.SAPLINGS.isTagged(itemStack.getType())) {
            return;
        }
        final ItemStack sapling = drops.copySapling();
        if (sapling == null) {
            return;
        }
        event.getEntity().setItemStack(sapling);
        chunk.removeBlock(position);
    }

}
