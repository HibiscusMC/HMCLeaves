package com.hibiscusmc.hmcleaves.paper.api;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

public final class HMCLeavesBreakEvent extends BlockBreakEvent {

    public HMCLeavesBreakEvent(@NotNull Block theBlock, @NotNull Player player) {
        super(theBlock, player);
    }

}
