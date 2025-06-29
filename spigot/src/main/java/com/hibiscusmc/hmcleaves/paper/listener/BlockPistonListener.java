package com.hibiscusmc.hmcleaves.paper.listener;

import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlockState;
import com.hibiscusmc.hmcleaves.paper.util.WorldUtil;
import com.hibiscusmc.hmcleaves.paper.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.paper.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.paper.world.Position;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class BlockPistonListener extends CustomBlockListener {

    public BlockPistonListener(HMCLeaves plugin) {
        super(plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        this.onPistonEvent(event.getBlock().getWorld(), event.getBlocks(), event.getDirection());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        this.onPistonEvent(event.getBlock().getWorld(), event.getBlocks(), event.getDirection());
    }

    private void onPistonEvent(World world, Collection<Block> blocks, BlockFace direction) {
        if (!this.config.isWorldWhitelisted(world.getName())) {
            return;
        }
        final LeavesWorld leavesWorld = this.worldManager.getWorld(world.getUID());
        if (leavesWorld == null) {
            return;
        }
        final Map<Position, CustomBlockState> insert = new HashMap<>();
        final Map<Position, BlockData> bukkitData = new HashMap<>();
        for (Block block : blocks) {
            final Location location = block.getLocation();
            final Position position = WorldUtil.convertLocation(location);
            final ChunkPosition chunkPosition = position.toChunkPosition();
            final LeavesChunk chunk = leavesWorld.getChunk(chunkPosition);
            if (chunk == null) {
                continue;
            }
            final CustomBlockState customBlockState = chunk.getBlock(position);
            if (customBlockState == null) {
                continue;
            }
            final PistonMoveReaction reaction = block.getPistonMoveReaction();
            if (reaction == PistonMoveReaction.BLOCK) {
                continue;
            }
            if (reaction == PistonMoveReaction.BREAK) {
                leavesWorld.editInsertChunk(chunkPosition, leavesChunk -> leavesChunk.removeBlock(position));
                continue;
            }
            if (reaction == PistonMoveReaction.MOVE) {
                final Position newPosition = position.add(direction.getModX(), direction.getModY(), direction.getModZ());
                leavesWorld.editInsertChunk(chunkPosition, leavesChunk -> {
                    leavesChunk.removeBlock(position);
                    insert.put(newPosition, customBlockState);
                    bukkitData.put(position, block.getBlockData());
                });
            }
        }
        for (Map.Entry<Position, CustomBlockState> entry : insert.entrySet()) {
            final Position position = entry.getKey();
            final ChunkPosition chunkPosition = position.toChunkPosition();
            final CustomBlockState customBlockState = entry.getValue();
            leavesWorld.editInsertChunk(chunkPosition, leavesChunk -> leavesChunk.setBlock(position, customBlockState));
        }
    }

}
