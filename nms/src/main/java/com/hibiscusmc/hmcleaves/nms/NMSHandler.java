package com.hibiscusmc.hmcleaves.nms;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface NMSHandler {

    byte calculateBreakSpeed(Player player, ItemStack itemStack, Block block, int ticksPassed);

}
