/*
 *
 *  *     HMCLeaves
 *  *     Copyright (C) 2022  Hibiscus Creative Studios
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU General Public License
 *  *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.github.fisher2911.hmcleaves.listener;

import io.github.fisher2911.hmcleaves.HMCLeaves;
import io.github.fisher2911.hmcleaves.cache.BlockCache;
import io.github.fisher2911.hmcleaves.config.LeavesConfig;
import io.github.fisher2911.hmcleaves.data.BlockData;
import io.github.fisher2911.hmcleaves.data.LeafData;
import io.github.fisher2911.hmcleaves.data.LogData;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class InteractionListener implements Listener {

    private final HMCLeaves plugin;
    private final LeavesConfig leavesConfig;
    private final BlockCache blockCache;

    public InteractionListener(HMCLeaves plugin) {
        this.plugin = plugin;
        this.leavesConfig = plugin.getLeavesConfig();
        this.blockCache = plugin.getBlockCache();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Bukkit.broadcastMessage("Clicked block");
        final ItemStack clickedWith = event.getItem();
        if (clickedWith == null) return;
        final Block block = event.getClickedBlock();
        if (block == null) return;
        final Location placeLocation = block.getRelative(event.getBlockFace()).getLocation();
        final World world = placeLocation.getWorld();
        if (world == null) return;
        final BlockData blockData = this.leavesConfig.getBlockData(clickedWith);
        if (this.doDebugTool(clickedWith, event.getPlayer(), block)) return;
        if (blockData == null) return;
        if (world.getNearbyEntities(placeLocation.getBlock().getBoundingBox()).size() > 0) return;
        if (blockData instanceof LeafData leafData) {
            event.getPlayer().sendMessage(leafData.displayDistance() + " " + leafData.displayDistance() + " " + leafData.getNewState().getType().getName());
        }
        if (blockData instanceof LogData) {
            event.getPlayer().sendMessage(blockData.getNewState().getType().getName());
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            this.blockCache.addBlockData(Position.fromLocation(placeLocation), blockData);
            final Block placedBlock = placeLocation.getBlock();
            placedBlock.setType(blockData.realBlockType());
            if (placedBlock.getBlockData() instanceof final Leaves leaves) {
                leaves.setPersistent(true);
                placedBlock.setBlockData(leaves);
            }
        }, 1);
    }

    private boolean doDebugTool(ItemStack itemStack, Player player, Block clicked) {
        if (!LeavesConfig.DEBUG_TOOL_ID.equalsIgnoreCase(PDCUtil.getItemId(itemStack))) {
            return false;
        }
        final BlockData blockData = this.blockCache.getBlockData(Position.fromLocation(clicked.getLocation()));
        if (!(clicked.getBlockData() instanceof final Leaves leaves) || !(blockData instanceof final LeafData leafData)) {
            if (blockData instanceof LogData) {
                player.sendMessage("Log");
            }
            return true;
        }
        if (blockData == BlockData.EMPTY) {
            player.sendMessage("Server distance: " + leaves.getDistance() + " Server persistence: " + leaves.isPersistent());
            player.sendMessage("The fake leaf data was not found");
            return true;
        }
        player.sendMessage("Display distance: " + leafData.displayDistance() + " Display persistence: " + leafData.displayPersistence() + " " +
                "server distance: " + leaves.getDistance() + " server persistence: " + leaves.isPersistent());
        return true;
    }

}
