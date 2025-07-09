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
import org.bukkit.event.block.LeavesDecayEvent;

public final class BlockDecayListener extends CustomBlockListener {

    public BlockDecayListener(HMCLeaves plugin) {
        super(plugin);
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
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
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            chunk.removeBlock(position);
            PacketUtil.sendSingleBlockChange(WrappedBlockState.getDefaultState(StateTypes.AIR), position, PlayerUtils.getNearbyPlayers(world.getUID(), chunkPosition));
        }, 1);
    }

}
