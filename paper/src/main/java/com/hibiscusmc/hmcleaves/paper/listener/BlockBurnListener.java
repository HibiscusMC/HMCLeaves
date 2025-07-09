package com.hibiscusmc.hmcleaves.paper.listener;

import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBurnEvent;

public final class BlockBurnListener extends CustomBlockListener {

    public BlockBurnListener(HMCLeaves plugin) {
        super(plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLeavesBurn(BlockBurnEvent event) {
        this.removeBlock(event.getBlock(), customBlockState -> {
        });
    }

}
