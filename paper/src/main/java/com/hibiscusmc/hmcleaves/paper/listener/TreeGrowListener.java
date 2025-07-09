package com.hibiscusmc.hmcleaves.paper.listener;

import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlock;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlockState;
import com.hibiscusmc.hmcleaves.paper.util.WorldUtil;
import com.hibiscusmc.hmcleaves.paper.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.paper.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.paper.world.Position;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.StructureGrowEvent;

public final class TreeGrowListener extends CustomBlockListener {

    public TreeGrowListener(HMCLeaves plugin) {
        super(plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTreeGrow(StructureGrowEvent event) {
        final LeavesWorld leavesWorld = this.worldManager.getWorld(event.getWorld().getUID());
        if (leavesWorld == null) {
            return;
        }
        event.getBlocks().forEach(block -> {
            final CustomBlock defaultBlock = this.config.getCustomBlockFromWorldBlockData(block.getBlockData());
            if (defaultBlock == null) {
                return;
            }
            final Position position = WorldUtil.convertLocation(block.getLocation());
            final ChunkPosition chunkPosition = position.toChunkPosition();
            final LeavesChunk chunk = leavesWorld.getChunk(chunkPosition);
            if (chunk == null) {
                return;
            }
            final CustomBlockState customBlockState = defaultBlock.getBlockStateFromWorldBlock(block.getBlockData());
            chunk.setBlock(position, customBlockState);
        });
    }

}
