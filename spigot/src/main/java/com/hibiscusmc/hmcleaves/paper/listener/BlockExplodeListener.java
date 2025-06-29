package com.hibiscusmc.hmcleaves.paper.listener;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlockState;
import com.hibiscusmc.hmcleaves.paper.packet.PacketUtil;
import com.hibiscusmc.hmcleaves.paper.util.PlayerUtils;
import com.hibiscusmc.hmcleaves.paper.util.WorldUtil;
import com.hibiscusmc.hmcleaves.paper.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.paper.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.paper.world.Position;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;

public final class BlockExplodeListener extends CustomBlockListener {

    public BlockExplodeListener(HMCLeaves plugin) {
        super(plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event) {
        this.handleExplosion(event.blockList());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockExplode(EntityExplodeEvent event) {
        this.handleExplosion(event.blockList());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void handleExplosion(List<Block> blocks) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            for (Block block : blocks) {
                final World world = block.getWorld();
                if (!this.config.isWorldWhitelisted(world.getName())) {
                    return;
                }
                final LeavesWorld leavesWorld = this.worldManager.getWorld(world.getUID());
                if (leavesWorld == null) {
                    continue;
                }
                final Position position = WorldUtil.convertLocation(block.getLocation());
                final ChunkPosition chunkPosition = position.toChunkPosition();
                final LeavesChunk chunk = leavesWorld.getChunk(chunkPosition);
                if (chunk == null) {
                    continue;
                }
                final CustomBlockState customBlockState = chunk.getBlock(position);
                if (customBlockState == null) {
                    continue;
                }
                chunk.removeBlock(position);
                PacketUtil.sendSingleBlockChange(WrappedBlockState.getDefaultState(StateTypes.AIR), position, PlayerUtils.getNearbyPlayers(world.getUID(), chunkPosition));
            }
        }, 1);
    }

}
