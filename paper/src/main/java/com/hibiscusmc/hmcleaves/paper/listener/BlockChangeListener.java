package com.hibiscusmc.hmcleaves.paper.listener;

import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlock;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlockState;
import com.hibiscusmc.hmcleaves.paper.packet.PacketUtil;
import com.hibiscusmc.hmcleaves.paper.util.PlayerUtils;
import com.hibiscusmc.hmcleaves.paper.util.WorldUtil;
import com.hibiscusmc.hmcleaves.paper.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.paper.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.paper.world.Position;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public final class BlockChangeListener extends CustomBlockListener {

    public BlockChangeListener(HMCLeaves plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockChange(EntityChangeBlockEvent event) {
        final Block block = event.getBlock();
        if (!this.config.isWorldWhitelisted(block.getWorld().getName())) {
            return;
        }
        final LeavesWorld leavesWorld = this.worldManager.getWorld(block.getWorld().getUID());
        if (leavesWorld == null) {
            return;
        }
        Bukkit.getScheduler().runTask(this.plugin, () -> {
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
            final CustomBlock customBlock = customBlockState.customBlock();
            final CustomBlockState newBlockState = customBlock.getBlockStateFromWorldBlock(block.getBlockData());
            if (newBlockState.equals(customBlockState)) {
                return;
            }
            leavesWorld.editInsertChunk(chunkPosition, leavesChunk -> {
                leavesChunk.setBlock(position, newBlockState);
                PacketUtil.sendSingleBlockChange(newBlockState.getBlockState(), position, PlayerUtils.getNearbyPlayers(leavesWorld.worldId(), chunkPosition));
            });
        });
    }

}
