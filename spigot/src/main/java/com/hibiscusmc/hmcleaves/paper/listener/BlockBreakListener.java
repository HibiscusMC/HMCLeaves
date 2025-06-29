package com.hibiscusmc.hmcleaves.paper.listener;

import com.hibiscusmc.hmcleaves.paper.HMCLeaves;
import com.hibiscusmc.hmcleaves.paper.config.BlockDropConfig;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public final class BlockBreakListener extends CustomBlockListener {

    public BlockBreakListener(HMCLeaves plugin) {
        super(plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        this.removeBlock(event.getBlock(), customBlockState -> {
            if (!event.isDropItems()) {
                return;
            }
            final Player player = event.getPlayer();
            if (player.getGameMode() == GameMode.CREATIVE) {
                return;
            }
            event.setDropItems(false);
            final Block block = event.getBlock();
            final Location location = block.getLocation();
            final BlockDropConfig drops = this.config.getBlockDrops(customBlockState.customBlock().id());
            if (drops == null) {
                return;
            }
            final ItemStack itemStack = player.getInventory().getItemInMainHand();
            if (itemStack.getType() != Material.SHEARS && drops.requiresShears()) {
                return;
            }
            final ItemStack dropItem = drops.copyLeavesItem();
            if (dropItem == null) {
                return;
            }
            final World world = block.getWorld();
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> world.dropItemNaturally(location.clone(), dropItem), 1);
        });
    }

}
