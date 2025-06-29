package com.hibiscusmc.hmcleaves.paper.listener;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlockState;
import com.hibiscusmc.hmcleaves.paper.util.WorldUtil;
import com.hibiscusmc.hmcleaves.paper.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.paper.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.paper.world.Position;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public final class BlockDestroyListener extends CustomBlockListener {

    public BlockDestroyListener(HMCLeaves plugin) {
        super(plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockDestroy(BlockDestroyEvent event) {
        final Block block = event.getBlock();
        final World world = block.getWorld();
        if (!this.config.isWorldWhitelisted(world.getName())) {
            return;
        }
        final LeavesWorld leavesWorld = this.worldManager.getWorld(world.getUID());
        if (leavesWorld == null) {
            return;
        }
        final Position position = WorldUtil.convertLocation(block.getLocation());
        final ChunkPosition chunkPosition = position.toChunkPosition();
        final LeavesChunk chunk = leavesWorld.getChunk(chunkPosition);
        if (chunk == null) {
            return;
        }
        final CustomBlockState customBlockState = chunk.getBlock(position);
        if (customBlockState == null) {
            return;
        }
        leavesWorld.editInsertChunk(chunkPosition, leavesChunk -> leavesChunk.removeBlock(position));
    }

}
