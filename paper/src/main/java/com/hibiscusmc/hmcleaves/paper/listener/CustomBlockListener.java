package com.hibiscusmc.hmcleaves.paper.listener;

import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.block.CustomBlockState;
import com.hibiscusmc.hmcleaves.paper.config.LeavesConfig;
import com.hibiscusmc.hmcleaves.paper.util.WorldUtil;
import com.hibiscusmc.hmcleaves.paper.world.ChunkPosition;
import com.hibiscusmc.hmcleaves.paper.world.LeavesChunk;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorld;
import com.hibiscusmc.hmcleaves.paper.world.LeavesWorldManager;
import com.hibiscusmc.hmcleaves.paper.world.Position;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;

import java.util.function.Consumer;

public abstract class CustomBlockListener implements Listener {

    protected final HMCLeaves plugin;
    protected final LeavesWorldManager worldManager;
    protected final LeavesConfig config;

    public CustomBlockListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.worldManager();
        this.config = plugin.leavesConfig();
    }

    protected boolean removeBlock(Block block, Consumer<CustomBlockState> dropHandler) {
        final World world = block.getWorld();
        if (!this.config.isWorldWhitelisted(world.getName())) {
            return true;
        }
        final LeavesWorld leavesWorld = this.worldManager.getWorld(block.getWorld().getUID());
        if (leavesWorld == null) {
            return false;
        }
        final Position position = WorldUtil.convertLocation(block.getLocation());
        final ChunkPosition chunkPosition = position.toChunkPosition();
        final LeavesChunk chunk = leavesWorld.getChunk(chunkPosition);
        if (chunk == null) {
            return false;
        }
        final CustomBlockState customBlockState = chunk.getBlock(position);
        if (customBlockState == null) {
            return false;
        }
        leavesWorld.editInsertChunk(chunkPosition, leavesChunk -> leavesChunk.removeBlock(position));
        dropHandler.accept(customBlockState);
        return true;
    }

}
