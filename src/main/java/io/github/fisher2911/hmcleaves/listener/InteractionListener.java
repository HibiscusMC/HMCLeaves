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
import io.github.fisher2911.hmcleaves.packet.PacketUtils;
import io.github.fisher2911.hmcleaves.util.PDCUtil;
import io.github.fisher2911.hmcleaves.world.Position;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

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
        final ItemStack clickedWith = event.getItem();
        if (clickedWith == null) return;
        final Block block = event.getClickedBlock();
        if (block == null) return;
        final Location placeLocation;
        if (block.getType() == Material.GRASS) {
            placeLocation = block.getLocation();
        } else {
            placeLocation = block.getRelative(event.getBlockFace()).getLocation();
        }
        final World world = placeLocation.getWorld();
        final Material placeLocationType = placeLocation.getBlock().getType();

        if (!placeLocationType.isAir() &&
                placeLocationType != Material.GRASS &&
                placeLocationType != Material.WATER &&
                placeLocationType != Material.LAVA
        ) {
            return;
        }
        if (world == null) return;
        final Axis axis = this.axisFromBlockFace(event.getBlockFace());
        final BlockData blockData = this.leavesConfig.getBlockData(clickedWith, axis);
        if (this.doDebugTool(clickedWith, event.getPlayer(), block)) {
            event.setCancelled(true);
            return;
        }
        final Player player = event.getPlayer();
        if (this.checkStripLog(player, block, clickedWith)) return;
        if (blockData == null) return;
        if (block.getType().isInteractable() && !player.isSneaking()) return;
        if (!(world.getNearbyEntities(placeLocation.clone().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5, LivingEntity.class::isInstance).isEmpty())) {
            event.setCancelled(true);
            return;
        }
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            final Block placedBlock = placeLocation.getBlock();
            final BlockState previousState = placedBlock.getState();
            placedBlock.setType(blockData.worldBlockType(), false);
            final BlockPlaceEvent blockPlaceEvent = createEvent(placedBlock, previousState, block, clickedWith, player, true, event.getHand());
            Bukkit.getPluginManager().callEvent(blockPlaceEvent);
            if (blockPlaceEvent.isCancelled()) {
                previousState.update(true, false);
                return;
            }
            if (player.getGameMode() != GameMode.CREATIVE) {
                clickedWith.setAmount(clickedWith.getAmount() - 1);
            }
            this.blockCache.addBlockData(Position.fromLocation(placeLocation), blockData);
            PacketUtils.sendArmSwing(player, List.of(player));
            final Sound sound = blockData.placeSound();
            if (sound != null) {
                world.playSound(placeLocation, sound, 1, 1);
            }
            if (placedBlock.getBlockData() instanceof final Leaves leaves) {
                if (!(blockData instanceof final LeafData leafData)) {
                    return;
                }
                if (player.getGameMode() != GameMode.CREATIVE || leafData.worldPersistence()) {
                    leaves.setPersistent(true);
                }
                leaves.setDistance(this.getDistance(placeLocation));
                placedBlock.setBlockData(leaves, true);
                return;
            }
            if (blockData instanceof final LogData logData && placedBlock.getBlockData() instanceof final Orientable orientable) {
                orientable.setAxis(logData.axis());
                placedBlock.setBlockData(orientable, true);
            }
        }, 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event instanceof LeafPlaceEvent || event instanceof LogPlaceEvent) return;
        final Block block = event.getBlock();
        final Material material = block.getType();
        final Position position = Position.fromLocation(block.getLocation());
        if (block.getBlockData() instanceof Leaves) {
            final BlockData blockData = this.leavesConfig.getDefaultLeafData(material);
            if (blockData == null) return;
            this.blockCache.addBlockData(position, blockData);
            return;
        }
        if (Tag.LOGS.isTagged(material)) {
            final Orientable orientable = (Orientable) block.getBlockData();
            final BlockData blockData = this.leavesConfig.getDefaultLogData(material, orientable.getAxis());
            if (blockData == null) return;
            this.blockCache.addBlockData(position, blockData);
        }
    }

    private static final Set<BlockFace> BLOCK_RELATIVE_FACES = Set.of(
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    );

    private int getDistance(Location location) {
        final Block block = location.getBlock();
        int minDistance = 7;
        for (BlockFace blockFace : BLOCK_RELATIVE_FACES) {
            final Block relative = block.getRelative(blockFace);
            // for some reason leaf distance isn't updating when placing near a log or leaf
            if (Tag.LOGS.isTagged(relative.getType())) {
                return 1;
            }
            // exit early if leaf state will update by itself
            if (Tag.LEAVES.isTagged(relative.getType())) {
                minDistance = Math.min(minDistance, ((Leaves) relative.getBlockData()).getDistance());
            }
        }
        return Math.min(minDistance + 1, 7);
    }

    private boolean doDebugTool(ItemStack itemStack, Player player, Block clicked) {
        if (!LeavesConfig.DEBUG_TOOL_ID.equalsIgnoreCase(PDCUtil.getItemId(itemStack))) {
            return false;
        }
        final BlockData blockData = this.blockCache.getBlockData(Position.fromLocation(clicked.getLocation()));
        if (!(clicked.getBlockData() instanceof final Leaves leaves) || !(blockData instanceof final LeafData leafData)) {
            if (blockData instanceof final LogData logData) {
                player.sendMessage(logData.id() + " Log type: " + " stripped: " + logData.stripped() + " realBlockType:" + logData.worldBlockType() + " : " + clicked.getType());
                return true;
            }
            player.sendMessage("Block data is not leaf data or log data: " + blockData.getClass().getSimpleName());
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

    private static final Set<Material> AXES = Set.of(
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.GOLDEN_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE
    );

    private boolean checkStripLog(Player player, Block block, ItemStack clickedWith) {
        if (!AXES.contains(clickedWith.getType())) return false;
        final Position position = Position.fromLocation(block.getLocation());
        final BlockData blockData = this.blockCache.getBlockData(position);
        if (!(blockData instanceof final LogData logData)) {
            return false;
        }
        if (logData.stripped()) {
            return false;
        }
        final LogData strippedData = logData.strip();
        this.blockCache.addBlockData(position, strippedData);
        block.setType(strippedData.worldBlockType());
        return true;
    }

    private Axis axisFromBlockFace(BlockFace face) {
        return switch (face) {
            case UP, DOWN -> Axis.Y;
            case NORTH, SOUTH -> Axis.Z;
            case EAST, WEST -> Axis.X;
            default -> throw new IllegalStateException("Unexpected value: " + face);
        };
    }

    private static class LogPlaceEvent extends BlockPlaceEvent {

        public LogPlaceEvent(@NotNull Block placedBlock, @NotNull BlockState replacedBlockState, @NotNull Block placedAgainst, @NotNull ItemStack itemInHand, @NotNull Player thePlayer, boolean canBuild, @NotNull EquipmentSlot hand) {
            super(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer, canBuild, hand);
        }

    }

    private static class LeafPlaceEvent extends BlockPlaceEvent {

        public LeafPlaceEvent(@NotNull Block placedBlock, @NotNull BlockState replacedBlockState, @NotNull Block placedAgainst, @NotNull ItemStack itemInHand, @NotNull Player thePlayer, boolean canBuild, @NotNull EquipmentSlot hand) {
            super(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer, canBuild, hand);
        }

    }

    private static BlockPlaceEvent createEvent(@NotNull Block placedBlock, @NotNull BlockState replacedBlockState, @NotNull Block placedAgainst, @NotNull ItemStack itemInHand, @NotNull Player thePlayer, boolean canBuild, @NotNull EquipmentSlot hand) {
        if (Tag.LOGS.isTagged(placedBlock.getType())) {
            return new LogPlaceEvent(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer, canBuild, hand);
        }
        if (Tag.LEAVES.isTagged(placedBlock.getType())) {
            return new LeafPlaceEvent(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer, canBuild, hand);
        }
        throw new IllegalArgumentException("Block is not a log or leaf: " + placedBlock.getType());
    }

}
