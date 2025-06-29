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
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

public final class BlockWaterlogListener extends CustomBlockListener {

    public BlockWaterlogListener(HMCLeaves plugin) {
        super(plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBucketInteract(PlayerBucketEmptyEvent event) {
        final Block block = event.getBlock();
        this.handleBucketEvent(block.getWorld(), block.getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBucketInteract(PlayerBucketFillEvent event) {
        final Block block = event.getBlock();
        this.handleBucketEvent(block.getWorld(), block.getLocation());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBucketInteract(BlockDispenseEvent event) {
        final Block block = event.getBlock();
        if (!(block.getBlockData() instanceof final Directional directional)) {
            return;
        }
        final Block changed = block.getRelative(directional.getFacing());
        this.handleBucketEvent(changed.getWorld(), changed.getLocation());
    }

    private void handleBucketEvent(World world, Location location) {
        if (!this.config.isWorldWhitelisted(world.getName())) {
            return;
        }
        final LeavesWorld leavesWorld = this.worldManager.getWorld(world.getUID());
        if (leavesWorld == null) {
            return;
        }
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            final Block block = location.getBlock();
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
                PacketUtil.sendSingleBlockChange(newBlockState.getBlockState(), position, PlayerUtils.getNearbyPlayers(world.getUID(), chunkPosition));
            });
        });
    }

}
